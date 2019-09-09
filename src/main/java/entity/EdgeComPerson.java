package entity;

import com.arangodb.entity.DocumentField;
import com.arangodb.entity.DocumentField.Type;

import java.text.MessageFormat;

/**
 * edge between NodeCom and NodePerson/NodeCom(There is not a direct relation between two NodePersons)
 * represents the relation between company and person
 * edge direction is from person to com
 */
public class EdgeComPerson {
    @DocumentField(Type.KEY)
    private String _key;

    @DocumentField(Type.FROM)
    private String _from;

    @DocumentField(Type.TO)
    private String _to;

    private String position;
    /**
     * person type w.r.t company
     */
    private int type;

    /**
     * person status w.r.t company
     */
    private int status;

    public String get_key() {
        return _key;
    }

    public void set_key(String _key) {
        this._key = _key;
    }

    public String get_from() {
        return _from;
    }

    public void set_from(String _from) {
        this._from = _from;
    }

    public String get_to() {
        return _to;
    }

    public void set_to(String _to) {
        this._to = _to;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }



    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String upsertShAql(String collName) {
        String aql = MessageFormat.format(
                "UPSERT { _key: '{0}'} INSERT {_key:'{0}', type: 1, _from: '{1}', "
                        + "_to: '{2}', status: {3} } UPDATE { type: OLD.type|1, "
                        + "status: OLD.status&0xFE|{3} } IN "
                        + collName,
                _key, _from, _to, status);
        return aql;
    }

    public String upsertMmAql(String collName) {
        String aql = MessageFormat.format(
                "UPSERT { _key: '{0}'} INSERT {_key:'{0}', position:'{1}', type: 2, _from: '{2}', "
                        + "_to: '{3}', status: {4} } UPDATE { position: '{1}', type: OLD.type|2, "
                        + "status: OLD.status&0xFD|{4} } IN " + collName,
                _key, position, _from, _to, status);
        return aql;
    }

    public String upsertLpAql(String collName) {
        String aql = MessageFormat.format(
                "UPSERT { _key: '{0}'} INSERT {_key:'{0}', type: 4, _from: '{1}', "
                        + "_to: '{2}' } UPDATE { type: OLD.type|4 } IN " + collName,
                _key, _from, _to);
        return aql;
    }
}
