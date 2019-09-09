package entity;

import java.text.MessageFormat;

/**
 * node for company in ArangoDB
 */
public class NodeCom {
    private String name;
    private String area;
    private String _id;
    /**
     * code of company
     * if company has no code, then use its name instead
     */
    private String _key;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String get_key() {
        return _key;
    }

    public void set_key(String _key) {
        this._key = _key;
    }



    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public NodeCom updateByOrgCompanyList(Person c) {
        this.name = c.getP_name();
        return  this;
    }

    /**
     * upsert a NodeCom
     * @param collName name of collection for NodeCom
     * @return
     */
    public String upsertAql(String collName) {
        String aql = MessageFormat.format(
                "UPSERT { _key: '{0}'} INSERT {_key:'{0}', name: '{1}', area:'{2}'",
                _key, name, area);

        aql += "} UPDATE {name: '{1}', area:'{2}'";


        aql += "} IN" + collName;
        return aql;
    }

    public String insertAql(String collName) {
        String aql = MessageFormat.format(
                "INSERT {_key:'{0}', name: '{1}', area:'{2}'",
                _key, name, area);


        aql += "} INTO " + collName + " OPTIONS {ignoreErrors: true }";
        return aql;
    }
}
