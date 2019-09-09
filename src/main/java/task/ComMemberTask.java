package task;

import dao.ArangoDBAccess;
import dao.DACompany;
import dao.RedisAccess;
import entity.*;
import utils.Checker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class ComMemberTask implements Runnable {

    /**
     * code of a company which has a group of stockholders to be stored
     *  into ArangoDB via this task
     */
    private String code;
    /**
     * area of a company which has a code `code`
     */
    private String area;

    private CountDownLatch latch = null;

    public ComMemberTask(String code, String area, CountDownLatch latch) {
        this.code = code;
        this.area = area;
        this.latch = latch;
    }

    /**
     * TODO: process other district except "shenzhen"
     * @param code
     * @param area
     */
    public void write2Arango(String code, String area) {
        if (area == null || area.length() < 2) return;
        if ("4403".equals(area)) {
            write2Arango(code);
            return;
        }

        int area_prefix = Integer.getInteger(area.substring(0,2));
        List<OrgCompanyGsxtDtlMgr> mms = DACompany.Client().selectMany_OrgCompanyGsxtDtlMgr(code, area_prefix);
        List<NodePerson> ps = new ArrayList<NodePerson>(mms.size());
        List<NodeCom> cs = new ArrayList<NodeCom>();
        List<EdgeComPerson> edges = new ArrayList<EdgeComPerson>();
        Map<String, String> person2position = new HashMap<>();
        Map<String, Integer> person2status = new HashMap<>();
        for(OrgCompanyGsxtDtlMgr mm : mms) {
            if (mm.getOm_name() == null || mm.getOm_name().equals(""))
                mm.setOm_name("unknown");
            String old = person2position.get(mm.getOm_name());
            String neu = old == null ? mm.getOm_position() : old + ";" + mm.getOm_position();
            person2position.put(mm.getOm_name(), neu);
            person2status.put(mm.getOm_name(), mm.getOm_status()==4?0:2);
        }
        for(String key : person2position.keySet()) {
            EdgeComPerson ecp = new EdgeComPerson();
            ecp.set_to(ArangoDBAccess.getVertexColls()[0]+"/"+code);
            ecp.setPosition(person2position.get(key));
            ecp.setType(2);
            ecp.setStatus(person2status.get(key));
            edges.add(ecp);
            if ("unknown".equals(key) || !Checker.comVSPerson(key)) {
                NodePerson np = new NodePerson();
                np.setName(key);
                np.set_key(code+key);
                np.set_id(ArangoDBAccess.getVertexColls()[1]+"/"+np.get_key());
                ps.add(np);

                ecp.set_key(code+key);
                ecp.set_from(np.get_id());
            } else {
                NodeCom nc = new NodeCom();
                nc.setName(key);
                String codearea = RedisAccess.get(key);
                if (codearea != null && codearea.length() >= 9) {
                    nc.set_key(codearea.substring(0,9));
                    nc.setArea(codearea.substring(9));
                } else {
                    nc.set_key(key);
                }
                nc.set_id(ArangoDBAccess.getVertexColls()[0]+"/"+nc.get_key());
                cs.add(nc);
                ecp.set_key(code+nc.get_key());
                ecp.set_from(nc.get_id());
            }
        }

        save2Arango(cs, ps, edges);
    }

    /**
     * store members of a company in SHENZHEN district
     * @param code
     */
    private void write2Arango(String code) {
        List<OrgCompanyDtlMgr> mms = DACompany.Client().selectMany_OrgCompanyDtlMgr(code);
        List<NodePerson> ps = new ArrayList<NodePerson>(mms.size());
        List<NodeCom> cs = new ArrayList<NodeCom>();
        List<EdgeComPerson> edges = new ArrayList<EdgeComPerson>();
        Map<String, String> person2position = new HashMap<>();

        for(OrgCompanyDtlMgr mm : mms) {
            if (mm.getOm_name() == null || mm.getOm_name().equals(""))
                mm.setOm_name("unknown");
            String old = person2position.get(mm.getOm_name());
            String neu = old == null ? mm.getOm_position() : old + ";" + mm.getOm_position();
            person2position.put(mm.getOm_name(), neu);
        }
        for(String key : person2position.keySet()) {
            EdgeComPerson ecp = new EdgeComPerson();
            ecp.set_to(ArangoDBAccess.getVertexColls()[0]+"/"+code);
            ecp.setPosition(person2position.get(key));
            ecp.setType(2);
            ecp.setStatus(2);
            edges.add(ecp);
            if ("unknown".equals(key) || !Checker.comVSPerson(key)) {
                NodePerson np = new NodePerson();
                np.setName(key);
                np.set_key(code+key);
                np.set_id(ArangoDBAccess.getVertexColls()[1]+"/"+np.get_key());
                ps.add(np);

                ecp.set_key(code+key);
                ecp.set_from(np.get_id());
            } else {
                NodeCom nc = new NodeCom();
                nc.setName(key);
                String codearea = RedisAccess.get(key);
                if (codearea != null && codearea.length() >= 9) {
                    nc.set_key(codearea.substring(0,9));
                    nc.setArea(codearea.substring(9));
                } else {
                    nc.set_key(key);
                }
                nc.set_id(ArangoDBAccess.getVertexColls()[0]+"/"+nc.get_key());
                cs.add(nc);
                ecp.set_key(code+nc.get_key());
                ecp.set_from(nc.get_id());
            }
        }

        save2Arango(cs, ps, edges);
    }

    private void save2Arango(List<NodeCom> cs, List<NodePerson> ps,
                             List<EdgeComPerson> es){

        // upsert vertices
        ArangoDBAccess.insertComs(cs);      // companies collected here must be stored in ArangoDB
        //   through `insert` operation, that means can't overwrite original data
//        ArangoDBAccess.upsertPersonsByMm(ps);
        ArangoDBAccess.upsertPersons(ps);
        // upsert edges
        ArangoDBAccess.upsertEdgeByMm(es);
    }

    public void run() {
        write2Arango(code, area);

        latch.countDown();
    }
}
