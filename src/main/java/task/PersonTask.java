package task;

import com.arangodb.entity.BaseDocument;
import dao.DACompany;
import dao.RedisAccess;
import entity.Person;
import org.slf4j.Logger;
import dao.ArangoDBAccess;
import entity.NodeCom;
import org.slf4j.LoggerFactory;
import utils.CacheProp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ComTask {
    private Logger logger = LoggerFactory.getLogger(ComTask.class);
    private int table_count = CacheProp.getProp("Tabel_Count", 8000);
    private int comStart = CacheProp.getProp("MainTable_Start", 0);
    private String consumer = CacheProp.getProp("MainTable_Consumer", null);
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    private ExecutorService executor = Executors.newFixedThreadPool(10);

    public void Start() {
        precedingWork();
        write2Arango();
    }

    /**
     * reset the reading start-position of Table `Person` for a given task(consumer)
     * when the consumer specified by config item `MainTable_Consumer` is changed, reset the
     *  start-position and update the config item `MainTable_Consumer` to be the current consumer
     * this method is usually called when starting an new task which need to traverse table `Person`
     * @param consumer
     * @param force if turned on, reset start-position no matter the consumer is changed
     */
    private void resetStart(String consumer, boolean force) {
        if (force || this.consumer == null || !this.consumer.equals(consumer)) {
            this.consumer = consumer;
            comStart = 0;
            CacheProp.setProp("MainTable_Consumer", consumer);
        }
    }

    /**
     * reversely index company
     * save to redis with key the company name and value the code+area
     * Notice: if there are some companies with the same name, then the first company will be
     *  stored in form of key-value pair, and all companies(include the first one) will be
     *  stored in form of key-set pair
     */
    public void reverseIndexCom() {
        resetStart("reverseIndexCom",false);
        while(reverseIndexComInner()) {

        }
        System.out.println("reversely index company finished.");
    }

    private boolean reverseIndexComInner() {
        List<Person> coms = DACompany.Client().selectMany_Person(
                comStart, table_count);
        if (coms.isEmpty()) return false;
        return reverseIndexComInner(coms);
    }

    private boolean reverseIndexComInner(List<Person> coms) {
        ArrayList<String> keyValues = new ArrayList<String>(coms.size()*2);
        List<String> keys = new ArrayList<>(coms.size());
        for(Person com : coms) {
            if (com.getP_name()==null || "".equals(com.getP_name())) continue;
            keys.add(com.getP_name());
            keyValues.add(com.getP_name());
        }
        String[] strs = new String[keyValues.size()];
        long count = RedisAccess.msetnx(keyValues.toArray(strs));

        if (count == 0) {
            keyValues.clear();
            String[] ks = new String[keys.size()];

            List<String> vs = RedisAccess.mget(keys.toArray(ks));

            int idx = 0;
            for(String v : vs) {
                if (v!=null) {
                    RedisAccess.sadd("set:"+ks[idx], v, strs[idx*2+1]);
                } else {
                    keyValues.add(ks[idx]);
                    keyValues.add(strs[idx*2+1]);
                }
                idx++;
            }
            strs = new String[keyValues.size()];
            RedisAccess.mset(keyValues.toArray(strs));
        }

        comStart = coms.get(coms.size()-1).getP_id();
        CacheProp.setProp("MainTable_Start", String.valueOf(comStart));
        return true;
    }

    /**
     * We can firstly write reversely Index data into redis and company base data into ArangoDB
     */
    public void precedingWork() {
        resetStart("precedingWork",false);
        while(precedingWorkInner()) {
            Date date = new Date();
            System.out.println("offset: " + comStart + " at " + dateFormat.format(date));
        }
        System.out.println("precedingWork finished.");
    }

    private boolean precedingWorkInner() {
        List<Person> coms = DACompany.Client().selectMany_Person(
                comStart, table_count);
        if (coms.isEmpty()) return false;

        writeCom2Arango(coms);
        reverseIndexComInner(coms);
        return true;
    }

    /**
     * write company base data into ArangoDB
     * Note that the company's unique which is promised by database
     * @param coms
     * @return
     */
    private boolean writeCom2Arango(List<Person> coms){
        ArrayList<BaseDocument> docs = new ArrayList<BaseDocument>(coms.size());
        for(Person com : coms) {
            BaseDocument doc = new BaseDocument();
            doc.addAttribute("name", com.getP_name());
            docs.add(doc);
        }
        ArangoDBAccess.bulkInsert(docs, ArangoDBAccess.getComCollName());
        return true;
    }

    /**
     * write to ArangoDB
     */
    public void write2Arango() {
        resetStart("write2Arango",false);
        while(write2ArangoInner()) {

        }
        System.out.println("write company into ArangoDB finished.");
    }


    private boolean write2ArangoInner() {
        List<Person> coms = DACompany.Client().selectMany_Person(
                comStart, table_count);
        if (coms.isEmpty()) return false;

        ArrayList<NodeCom> cfs = new ArrayList<NodeCom>(coms.size());
        CountDownLatch latch = new CountDownLatch(coms.size()*3);
        for(Person com : coms) {
            if (com.getP_name()==null || "".equals(com.getP_name())) continue;

            NodeCom cf = new NodeCom().updateByOrgCompanyList(com);
            cfs.add(cf);
            executor.submit(new ComDtlTask(latch, cf));
            executor.submit(new ComMemberTask(cf.get_key(), cf.getArea(), latch));
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }

        // upsert main body of companies
        ArangoDBAccess.upsertComs(cfs);

        // person merging
        PersonMergeTask.merge(cfs);
        
        comStart = coms.get(coms.size()-1).getP_id();
        CacheProp.setProp("MainTable_Start", String.valueOf(comStart));
        return true;
    }
}
