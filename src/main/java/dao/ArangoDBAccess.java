package dao;

import com.arangodb.*;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.util.MapBuilder;
import com.google.gson.Gson;
import entity.EdgeComPerson;
import entity.NodeCom;
import entity.NodePerson;
import org.apache.ibatis.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;


public class ArangoDBAccess {
    private static ArangoDB client = null;
    private static Logger logger = LoggerFactory.getLogger(ArangoDBAccess.class);
    private static String dbName = null;
    private static String graphName = null;
    private static String[] vertexColls = null;

    public static String[] getVertexColls() {
        return vertexColls;
    }

    public static String getPersonCollName() {
        return vertexColls[1];
    }

    public static String getComCollName() {
        return vertexColls[0];
    }

    public static String getEdgeColl() {
        return edgeColl;
    }

    private static String edgeColl = null;
    static {
        try {
            InputStream is = Resources.getResourceAsStream("config.properties");
            Properties prop = new Properties();
            prop.load(is);
            String host = prop.getProperty("ARANGODB_HOST");
            String[] ipport= host.split(",", 2);
            int port = Integer.getInteger(ipport[1]);
            String[] userpwd = prop.getProperty("ARANGODB_USER").split(",", 2);
            client = new ArangoDB.Builder().host(ipport[0], port)
                    .user(userpwd[0]).password(userpwd[1]).build();
            dbName = prop.getProperty("ARANGODB_DB");
            graphName = prop.getProperty("ARANGODB_GRAPH");
            vertexColls = prop.getProperty("ARANGODB_VERTEXCOLLS").split(",");
            edgeColl = prop.getProperty("ARANGODB_EDGECOLLS");
        } catch (IOException e){
            logger.error(e.getMessage());
        }

    }

    public static void initGraph() {
        ArangoDatabase db = client.db(dbName);
        if (!db.exists()) client.createDatabase(dbName);
        ArangoGraph graph = db.graph(graphName);
        if (!graph.exists()) {
            EdgeDefinition ed = new EdgeDefinition().collection(edgeColl)
                    .from(vertexColls).to(vertexColls[0]);
            db.createGraph(graphName, Arrays.asList(ed));
        }
    }

    public static List<NodePerson> getByPersonName(String name) {
        ArangoDatabase db = client.db(dbName);
        ArangoCollection coll = db.collection(vertexColls[1]);
        ArangoCursor<BaseDocument> cursor = db.query(
                "FOR i IN @@collection FILTER i.name == '@@name' RETURN i",
                new MapBuilder().put("@collection", vertexColls[1]).put("@name", name).get(),
                null,
                BaseDocument.class
        );
        List<NodePerson> ps = new LinkedList<>();
        while(cursor.hasNext()){
            BaseDocument doc = cursor.next();
            NodePerson p = new NodePerson();
            p.set_key(doc.getKey());
            p.setName((String) doc.getAttribute("name"));
            ps.add(p);
        }
        return ps;
    }

    /**
     * traverse a graph from a given start node
     * e.g.
     *      for v, e, p IN 1..100 ANY "coms/code_2" GRAPH 'commap' return {vertex: v, edge: e, path: p}
     * @param c given start node
     * @param maxLength max distance
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> List<T> traverse(NodeCom c, int maxLength, Class<T> clazz) {
        if (maxLength<1)
            throw new IllegalArgumentException("maxLength must be larger then or equal to 1");

        ArangoDatabase db = client.db(dbName);
        int flag = 0;
        String rtn = null;
        String clazzName = clazz.getName();
        if (clazzName.contains("NodePerson")) {
            flag = 2;
            rtn = "v";
        } else if (clazzName.contains("NodeCom")) {
            flag = 1;
            rtn = "v";
        } else if (clazzName.contains("EdgeComPerson")) {
            flag = 3;
            rtn = "e";
        } else if (clazzName.contains("GraphVEP")) {
            flag = 4;
            rtn = "{vertex: v, edge: e}";
        } else {
            throw new IllegalArgumentException("invalid class: " + clazz.getName());
        }
        String aql = String.format("FOR v, e, p IN 1..%d ANY @start GRAPH '%s' return %s",
                maxLength, graphName, rtn);
        final Map<String, Object> map = new MapBuilder().put("start", c.get_id()).get();
        ArangoCursor<String> cur = db.query(aql, map, null, String.class);

        List<T> cluster = new ArrayList<T>();
        Gson gs = new Gson();

        for(;cur.hasNext();) {
            String json = cur.next();

            if ((flag == 2 && json.contains("person/") && !json.contains("com/")) ||
                    (flag == 1 && json.contains("com/")&&!json.contains("person/")) ||
                    flag >= 3) {
                cluster.add(gs.fromJson(json, clazz));
            }
        }
        return cluster;
    }


    public static void deleteDocs(List<String> keys, String collName) {
        ArangoDatabase db = client.db(dbName);
        ArangoCollection coll = db.collection(collName);
        coll.deleteDocuments(keys);
    }



    public static void upsertComs(List<NodeCom> coms) {
        ArangoDatabase db = client.db(dbName);
        for(NodeCom com : coms) {
            db.query(com.upsertAql(vertexColls[0]), null);
        }
    }

    public static void insertComs(List<NodeCom> coms) {
        ArangoDatabase db = client.db(dbName);
        for(NodeCom com : coms) {
            db.query(com.insertAql(vertexColls[0]), null);
        }
    }

    public static void bulkInsert(List<BaseDocument> docs, String collName) {
        ArangoDatabase db = client.db(dbName);
        ArangoCollection coll = db.collection(collName);
        coll.insertDocuments(docs, new DocumentCreateOptions());
    }

    /**
     * upsert Stockholders nodes(person/com)
     * @param ps
     */
    @Deprecated
    public static void upsertPersonsBySh(List<NodePerson> ps) {
        ArangoDatabase db = client.db(dbName);
        for(NodePerson p : ps) {
            db.query(p.upsertShAql(vertexColls[1]), null);
        }
    }

    @Deprecated
    public static void upsertPersonsByMm(List<NodePerson> ps) {
        ArangoDatabase db = client.db(dbName);
        for(NodePerson p : ps) {
            db.query(p.upsertMmAql(vertexColls[1]), null);
        }
    }

    public static void upsertComByLp(NodeCom c) {
        ArangoDatabase db = client.db(dbName);
        db.query(c.upsertAql(vertexColls[0]), null);
    }

    @Deprecated
    public static void upsertPersonByLp(NodePerson p) {
        ArangoDatabase db = client.db(dbName);
        db.query(p.upsertLpAql(vertexColls[1]), null);
    }

    /**
     * From now, person do not maintain it's type to a company, so
     * it's not necessary to distinguish stockholder, member or legal-person
     * @param p
     */
    public static void upsertPerson(NodePerson p) {
        ArangoDatabase db = client.db(dbName);
        db.query(p.upsertAql(vertexColls[1]), null);
    }

    public static void upsertPersons(List<NodePerson> ps) {
        ArangoDatabase db = client.db(dbName);
        for(NodePerson p : ps) {
            db.query(p.upsertAql(vertexColls[1]), null);
        }
    }

    public static void upsertEdgeBySh(List<EdgeComPerson> es) {
        ArangoDatabase db = client.db(dbName);
        for(EdgeComPerson e : es) {
            db.query(e.upsertShAql(edgeColl), null);
        }
    }

    public static void upsertEdgeByLp(EdgeComPerson e) {
        ArangoDatabase db = client.db(dbName);
        db.query(e.upsertLpAql(edgeColl), null);
    }

    public static void upsertEdgeByMm(List<EdgeComPerson> es) {
        ArangoDatabase db = client.db(dbName);
        for(EdgeComPerson e : es) {
            db.query(e.upsertMmAql(edgeColl), null);
        }
    }
}
