package entity;

import dao.ArangoDBAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Cluster4Merge {
    /**
     * node list of persons in this cluster
     */
    public List<GraphVEP> veps = new ArrayList<>();
    /**
     * person names' set in this cluster
     */
    public HashSet<String> pnames = new HashSet<>();

    public Cluster4Merge(List<GraphVEP> veps) {
        for(GraphVEP vep : veps){
            GraphVEP.ComPersonMix cpm = vep.getVertex();
            if(cpm.get_id().startsWith(ArangoDBAccess.getComCollName())){
                pnames.add(cpm.getName());
                this.veps.add(vep);
            }
        }
    }

    public void merge_(Cluster4Merge that){
        veps.addAll(that.veps);
        pnames.addAll(that.pnames);
    }

    public HashMap<String, List<GraphVEP>> splitByName(){
        HashMap<String, List<GraphVEP>> map = new HashMap<>();
        for(GraphVEP vep : veps) {
            GraphVEP.ComPersonMix cpm = vep.getVertex();
            String name = cpm.getName();
            if(map.containsKey(name)) {
                if(cpm.get_key().startsWith("#")){
                    map.get(name).add(0, vep);
                } else {
                    map.get(name).add(vep);
                }
            } else {
                List<GraphVEP> l = new ArrayList<>();
                l.add(vep);
                map.put(name, l);
            }
        }
        return map;
    }
}
