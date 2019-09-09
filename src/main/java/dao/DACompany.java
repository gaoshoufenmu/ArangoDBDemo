package dao;

import entity.*;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class DACompany extends DataAccessBase {
    private SqlSession session;
    private static DACompany client = new DACompany();
    private Logger logger = LoggerFactory.getLogger(DACompany.class);

    public static DACompany Client() {
        return client;
    }

    private DACompany() {
        try {
            InputStream is = Resources.getResourceAsStream(mybatis_config);
            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(is, "DACompany");
            this.session = factory.openSession();
            is.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public void close() {
        if (this.session != null) this.session.close();
    }

    public List<Person> selectMany_Person(int start, int count) {
        return this.session.selectList("DACompany.selectMany_Person", new RetrieveRange(start, count));
    }

    public PersonInfo select_OrgCompanyDtl(String code) {
        return this.session.selectOne("DACompany.select_PersonInfo", code);
    }


    public int select_Checkpt(String key) {
        return this.session.selectOne("DACompany.select_Checkpt", key);
    }

    public void insert_Checkpt(String key) {
        this.session.insert("DACompany.insert_Checkpt", key);
    }

    public void update_Checkpt(String key, int value) {
        this.session.update("DACompany.update_Checkpt", new KeyValue(key, value));
    }
}
