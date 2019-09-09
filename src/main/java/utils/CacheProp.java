package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.ibatis.io.Resources;

import java.io.*;
import java.util.Properties;

public class CacheProp {
    private static Properties prop = new Properties();
    private static Logger logger = LoggerFactory.getLogger(CacheProp.class);
    private static String propFilePath = CacheProp.class.getResource("/").getPath()+"cache.properties";
    static {
        try {
            InputStream is = Resources.getResourceAsStream("cache.properties");
            prop.load(is);
        } catch (IOException e) {

        }
    }

    public static void setProp(String key, String value) {
        prop.setProperty(key, value);
        try {
            OutputStream os = new FileOutputStream("cache.properties" /*propFilePath*/);
            prop.store(os, "update value");
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String getProp(String key, String def) {
        return prop.getProperty(key, def);
    }

    public static int getProp(String key, int def) {
        String val = prop.getProperty(key, null);
        if (val == null) return def;

        return Integer.getInteger(key, def);
    }
}
