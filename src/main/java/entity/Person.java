package entity;

public class Person {
    private int p_id;

    private String p_name;

    public int getP_id() {
        return p_id;
    }

    public void setP_id(int p_id) {
        this.p_id = p_id;
    }


    public String getP_name() {
        return p_name;
    }

    public void setP_name(String p_name) {
        this.p_name = p_name;
    }

    public String getOc_credit() {
        return oc_credit;
    }

    public void setOc_credit(String oc_credit) {
        this.oc_credit = oc_credit;
    }

    private String oc_credit;



    @Override
    public String toString() {
        return String.format("p_id: %d, p_name: %s",
                p_id, p_name);
    }
}
