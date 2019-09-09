package entity;

public class GraphVEP {
    private EdgeComPerson edge;
    private ComPersonMix vertex;
    // path
    public EdgeComPerson getEdge() {
        return edge;
    }

    public void setEdge(EdgeComPerson edge) {
        this.edge = edge;
    }

    public ComPersonMix getVertex() {
        return vertex;
    }

    public void setVertex(ComPersonMix vertex) {
        this.vertex = vertex;
    }

    public class ComPersonMix {
        private String name;
        private String area;
        private String _id;
        private String _key;
        @Deprecated
        private int type;
        private double regcapital = -1;

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

        public String get_id() {
            return _id;
        }

        public void set_id(String _id) {
            this._id = _id;
        }

        public String get_key() {
            return _key;
        }

        public void set_key(String _key) {
            this._key = _key;
        }

        @Deprecated
        public int getType() {
            return type;
        }

        @Deprecated
        public void setType(int type) {
            this.type = type;
        }

        public double getRegcapital() {
            return regcapital;
        }

        public void setRegcapital(double regcapital) {
            this.regcapital = regcapital;
        }
    }
}
