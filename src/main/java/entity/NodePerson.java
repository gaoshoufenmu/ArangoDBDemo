package entity;

import java.text.MessageFormat;

/**
 * node for person in ArangoDB
 */
public class NodePerson {
    private String _id;
    /**
     * code of related company + person name
     */
    private String _key;
    private String code;
    private String name;





//    private String position;

    public String get_key() {
        return _key;
    }

    public void set_key(String _key) {
        this._key = _key;
    }




    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }



    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }


    public String upsertAql(String collName) {
        return MessageFormat.format("UPSERT { _key: '{0}'} INSERT {_key:'{0}', name: '{1}' }"
                + " UPDATE {name: '{1}' } IN " + collName,
                _key, name);
    }

    /**
     * upsertion AQL for stockholder part
     * @return
     */
    public String upsertShAql(String collName) {
        String aql = MessageFormat.format(
                "UPSERT { _key: '{0}'} INSERT {_key:'{0}', name: '{1}', type: 1}"
                + " UPDATE {name: '{1}', type: OLD.type|1 } IN " + collName,
                _key, name);
        return aql;
    }

    /**
     * upsertion AQL for member part
     * @param collName
     * @return
     */
    public String upsertMmAql(String collName) {
        String aql = MessageFormat.format(
                "UPSERT { _key: '{0}'} INSERT {_key:'{0}', name: '{1}', type: 2}"
                        + " UPDATE {name: '{1}', type: OLD.type|2 } IN " + collName,
                _key, name);
        return aql;
    }

    /**
     * upsertion AQL for legal-person part
     * @param collName name of collection of NodePerson
     * @return
     */
    public String upsertLpAql(String collName) {
        String aql = MessageFormat.format(
                "UPSERT { _key: '{0}'} INSERT {_key:'{0}', name: '{1}', type: 4}"
                        + " UPDATE {name: '{1}', type: OLD.type|4 } IN " + collName,
                _key, name);
        return aql;
    }
}
