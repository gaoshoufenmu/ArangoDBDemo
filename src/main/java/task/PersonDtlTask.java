package task;

import dao.ArangoDBAccess;
import dao.DACompany;
import dao.RedisAccess;
import entity.EdgeComPerson;
import entity.NodeCom;
import entity.NodePerson;
import entity.PersonInfo;
import utils.Checker;

import java.util.concurrent.CountDownLatch;

/**
 * Get extra infos for Company from Table 'OrgCompanyDtl'
 */
public class PersonDtlTask implements Runnable {
    private final CountDownLatch latch;
    private NodeCom cf;


    public PersonDtlTask(CountDownLatch latch, NodeCom cf) {
        this.latch = latch;
        this.cf = cf;
    }
    public void run() {
        // ...

        latch.countDown();
    }

}
