package utils;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.regex.Pattern;

public class Checker {

    public static boolean isPerson(String name) {
        if (name == null || "".equals(name) || name.length()==1) return false;

        String res = NLP.findPersonName(name);
        if (res == null) return false;

        double ratio = ((double)res.length())/name.length();

        return ratio >= 0.6;
    }

    public static boolean containsPerson(String name) {
        if (name == null || "".equals(name) || name.length()==1) return false;

        return NLP.findPersonName(name) != null;
    }

    public static boolean isOrg(String name) {
        if (name == null || "".equals(name) || name.length()<3) return false;

        String res = NLP.findPersonName(name);
        if (res == null) return false;

        double ratio = ((double)res.length())/name.length();

        return ratio >= 0.5;
    }

    public static boolean containsOrg(String name) {
        if (name == null || "".equals(name) || name.length()<3) return false;

        return NLP.findOrgName(name) != null;
    }

    /**
     * judge if is company or person by a given name
     * @param name
     * @return true if company or false for person
     */
    public static boolean comVSPerson(String name) {
        if (name == null || "".equals(name) || name.length()<4) return false;

        return containsOrg(name);
    }


    /**
     *
     * @param name candidate name to check
     * @param area related company's area code
     * @param type extra type info crawled from web
     * @return
     */
    public static boolean isPerson(String name, String area, String type) {
        throw new NotImplementedException();
    }

    /**
     *
     * @param key
     * @return
     */
    public static boolean startswithComCode(String key) {
        String reg = "[0-9A-Z]{9}.+";
        boolean isMatch = Pattern.matches(reg, key);
        return isMatch;
    }
}
