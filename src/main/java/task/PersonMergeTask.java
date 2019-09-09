package task;

import com.arangodb.ArangoCursor;
import com.arangodb.entity.BaseDocument;
import dao.ArangoDBAccess;
import entity.*;

import java.util.*;

public class PersonMergeTask {

    /**
     * merge Person nodes via connected graph
     * In old implementation, if two disconnected companies which have more than 2 pairs of the same person names,
     *  then the two companies node should be connected and those pairs of person nodes should be merged.
     * @param cs
     */
    public static void merge(List<NodeCom> cs) {
        HashSet<String> comKeys = new HashSet<>();
        for(NodeCom c : cs) {
            if(comKeys.contains(c.get_key())) continue;

            comKeys.addAll(mergeOne(c));
        }
    }

    private static List<String> mergeOne(NodeCom c) {
        List<String> comKeys = new ArrayList<>();
        List<GraphVEP> veps = ArangoDBAccess.traverse(c, 100, GraphVEP.class);
        Map<String, List<GraphVEP.ComPersonMix>> name2persons = new HashMap<>();
        Map<String, List<EdgeComPerson>> person2Edges = new HashMap<>();

        String personColl = ArangoDBAccess.getPersonCollName();
        for(GraphVEP vep : veps) {
            EdgeComPerson e = vep.getEdge();
            GraphVEP.ComPersonMix cpm = vep.getVertex();
            String from = e.get_from();
            if (from.startsWith(personColl)) {
                if(person2Edges.containsKey(from)) {
                    person2Edges.get(from).add(e);
                } else {
                    List<EdgeComPerson> edges = new ArrayList<EdgeComPerson>();
                    edges.add(e);
                    person2Edges.put(from, edges);
                }
            }
            if (cpm.get_id().startsWith(personColl)) {
                if (name2persons.containsKey(cpm.getName())) {
                    if(cpm.get_key().startsWith("#")){
                        name2persons.get(cpm.getName()).add(0, cpm);
                    } else {
                        name2persons.get(cpm.getName()).add(cpm);
                    }
                } else {
                    name2persons.put(cpm.getName(), new ArrayList<GraphVEP.ComPersonMix>() {{add(cpm);}});
                }
            } else {        // we can believe that cpm is absolutely a company here
                comKeys.add(cpm.get_key());
            }
        }

        List<EdgeComPerson> edge2Add = new ArrayList<>();
        List<BaseDocument> person2Add = new ArrayList<>();
        List<String> edges2Del = new ArrayList<>();
        List<String> person2Del = new ArrayList<>();
        for(String k : name2persons.keySet()) {
            // get a group of persons with the same name
            List<GraphVEP.ComPersonMix> ps = name2persons.get(k);
            GraphVEP.ComPersonMix firstP = ps.get(0);
            String from = firstP.get_id();

            // Try to find the unique person(marked as A) who has many relations with many companies
            //  If found, all other persons will be merged into A, and all their original relations
            //  will be transferred to new relations which are established from A
            //  to their original related companies.
            //  If not found, new a person as A, and all persons will be merged into A, the reminding operations
            //  are the same as former case.
            // Notice that different from normal person who has a name started with a company code, A's name
            //  should start with '#' which is followed by a company code, and because of this, it is possible
            //  for us to find A, as which the beginning of this comment described.

            int start = 0;
            if (firstP.get_key().startsWith("#")) { // have establish the unique NodePerson
                start = 1;
            } else {
                BaseDocument np = new BaseDocument();
                np.setKey("#"+firstP.get_key());
                np.setId(personColl+"/"+np.getKey());
                np.addAttribute("name", firstP.getName());
                from = np.getId();
                person2Add.add(np);
            }
            // all other person will be merged into 'firstP'
            for(int i = start; i < ps.size(); i++) {
                GraphVEP.ComPersonMix p = ps.get(i);
                person2Del.add(p.get_key());
                List<EdgeComPerson> oldEdges = person2Edges.get(p.get_id());
                for(EdgeComPerson e : oldEdges) {
                    EdgeComPerson ne = new EdgeComPerson();
                    ne.set_to(e.get_to());
                    ne.set_from(from);
                    ne.setStatus(e.getStatus());
                    ne.setPosition(e.getPosition());
                    ne.setType(e.getType());
                    edge2Add.add(ne);
                    edges2Del.add(e.get_key());
                }
            }
        }
        // ===== Notice the execution order!!! =======

        // delete old Edges first
        // and then delete old NodePerson
        ArangoDBAccess.deleteDocs(edges2Del, ArangoDBAccess.getEdgeColl());
        ArangoDBAccess.deleteDocs(person2Del, personColl);

        // add new NodePerson first
        // and then add new Edges
        ArangoDBAccess.bulkInsert(person2Add, personColl);
        ArangoDBAccess.upsertEdgeBySh(edge2Add);
        return comKeys;
    }

    /**
     * If in two clusters there are more than 2 pairs of persons
     *  that in each pair the two persons' name is same, then merge the two cluster.
     * The math describing language is:
     *  CLUSTER A, CLUSTER B, people with same name are following:
     *  [name1, name2,...] \in A
     *  [name1, name2,...] \in B
     *  then, merge A and B
     * @param ps
     */
    public static void merge2(List<NodePerson> ps) {
        // make the person name unique
        HashSet<String> hs = new HashSet<>();
        for(NodePerson p : ps) {
            if(!hs.contains(p.getName())){
                hs.add(p.getName());
                merge2(p);
            }
        }
    }

    public static void merge2(NodePerson np) {
        // get all person nodes with the same name
        List<NodePerson> ps = ArangoDBAccess.getByPersonName(np.getName());
        // get all known people for each person

        List<Cluster4Merge> clusters = new ArrayList<>();
        for(NodePerson p : ps) {
            NodeCom c = new NodeCom();
            String pk = p.get_key();
            if(pk.startsWith("#")) {
                c.set_id(ArangoDBAccess.getComCollName()+"/"+pk.substring(1,1+9));
            } else {
                c.set_id(ArangoDBAccess.getComCollName()+"/"+pk.substring(0,9));
            }

            List<GraphVEP> veps = ArangoDBAccess.traverse(c, 100, GraphVEP.class);
            Cluster4Merge cur = new Cluster4Merge(veps);

            int index = -1;
            while(index>=-1) {
                index = -2;
                for(int i = 0; i < clusters.size(); i++){
                    Cluster4Merge cluster = clusters.get(i);
                    Set<String> set = new HashSet<>();
                    set.addAll(cluster.pnames);
                    set.retainAll(cur.pnames);
                    if (set.size()>=2){
                        cur.merge_(cluster);
                        index = i;
                        break;
                    }
                }
                if (index>=0) {
                    // delete the cluster that be merged into other cluster
                    clusters.remove(index);
                }
            }
            clusters.add(cur);
        }

        String personColl = ArangoDBAccess.getPersonCollName();
        List<EdgeComPerson> edge2Add = new ArrayList<>();
        List<BaseDocument> person2Add = new ArrayList<>();
        List<String> edges2Del = new ArrayList<>();
        List<String> person2Del = new ArrayList<>();
        // foreach cluster, delete old edges and NodePersons and then add new NodePersons and edges
        for(Cluster4Merge cluster : clusters) {
            Map<String, List<GraphVEP>> map = cluster.splitByName();
            for(String name : map.keySet()) {
                // all person nodes for the current person name
                List<GraphVEP> l = map.get(name);

                GraphVEP.ComPersonMix first = l.get(0).getVertex();
                String from = first.get_id();

                // Try to find the unique person(marked as A) who has many relations with many companies
                //  If found, all other persons will be merged into A, and all their original relations
                //  will be transferred to new relations which are established from A
                //  to their original related companies.
                //  If not found, new a person as A, and all persons will be merged into A, the reminding operations
                //  are the same as former case.
                // Notice that different from normal person who has a name started with a company code, A's name
                //  should start with '#' which is followed by a company code, and because of this, it is possible
                //  for us to find A, as which the beginning of this comment described.

                int start = 0;
                if (first.get_key().startsWith("#")) { // have establish the unique NodePerson
                    start = 1;
                } else {
                    BaseDocument doc = new BaseDocument();
                    doc.setKey("#"+first.get_key());
                    doc.setId(personColl+"/"+doc.getKey());
                    doc.addAttribute("name", first.getName());
                    from = doc.getId();
                    person2Add.add(doc);
                }
                // all other person will be merged into 'firstP'
                for(int i = start; i < l.size(); i++) {
                    GraphVEP.ComPersonMix p = l.get(i).getVertex();
                    person2Del.add(p.get_key());
                    EdgeComPerson e = l.get(i).getEdge();
                    EdgeComPerson ne = new EdgeComPerson();
                    ne.set_to(e.get_to());
                    ne.set_from(from);
                    ne.setStatus(e.getStatus());
                    ne.setPosition(e.getPosition());
                    ne.setType(e.getType());
                    edge2Add.add(ne);
                    edges2Del.add(e.get_key());
                }
            }
        }

        // ===== Notice the execution order!!! =======

        // delete old Edges first
        // and then delete old NodePerson
        ArangoDBAccess.deleteDocs(edges2Del, ArangoDBAccess.getEdgeColl());
        ArangoDBAccess.deleteDocs(person2Del, personColl);

        // add new NodePerson first
        // and then add new Edges
        ArangoDBAccess.bulkInsert(person2Add, personColl);
        ArangoDBAccess.upsertEdgeBySh(edge2Add);

    }


}
