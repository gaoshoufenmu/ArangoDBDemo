package dao;

import org.apache.ibatis.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.lang.AutoCloseable;

public class RedisAccess implements AutoCloseable {
    private static JedisPool pool = null;
    private static Logger logger = LoggerFactory.getLogger(RedisAccess.class);
    static {
        try {
            InputStream is = Resources.getResourceAsStream("config.properties");
            Properties prop = new Properties();
            prop.load(is);
            String[] host = prop.getProperty("REDIS_HOST").split(":");
            String pwd = prop.getProperty("REDIS_PWD");
            int port = Integer.getInteger(host[1]);
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(1000);
            config.setMaxIdle(100);
            if (pwd != null && !pwd.equals(""))
                pool = new JedisPool(config, host[0], port, 10000, pwd);
            else
                pool = new JedisPool(config, host[0], port, 10000);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }


    private Jedis jedis = null;
    public RedisAccess() {
        jedis = getJedis();
    }

    public static String get(String key) {
        try(RedisAccess ra = new RedisAccess()) {
            return ra.jedis.get(key);
        }
//        Jedis jedis = getJedis();
//        String val = jedis.get(key);
//        close(jedis);
//        return val;
    }

    public static String set(String key, String value) {
        try(RedisAccess ra = new RedisAccess()) {
            return ra.jedis.set(key, value);
        }
//        Jedis jedis = getJedis();
//        String status = jedis.set(key, value);
//        close(jedis);
//        return status;
    }

    public static long del(String... keys) {
        try(RedisAccess ra = new RedisAccess()) {
            return ra.jedis.del(keys);
        }
//        Jedis jedis = getJedis();
//        Long count = jedis.del(keys);
//        close(jedis);
//        return count;   // 0 if none of the specified key existed
    }

    public static long append(String key, String value) {
        try(RedisAccess ra = new RedisAccess()) {
            return ra.jedis.append(key, value);
        }
//        Jedis jedis = getJedis();
//        Long length = jedis.append(key, value);
//        close(jedis);
//        return length;  // total length of the string after the append operation
    }

    public static boolean exists(String key) {
        try(RedisAccess ra = new RedisAccess()) {
            return ra.jedis.exists(key);
        }
//        Jedis jedis = getJedis();
//        boolean res = jedis.exists(key);
//        close(jedis);
//        return res;
    }

    public static long exists(String... keys) {
        try(RedisAccess ra = new RedisAccess()) {
            return ra.jedis.exists(keys);
        }
//        Jedis jedis = getJedis();
//        long res = jedis.exists(keys);
//        close(jedis);
//        return res;
    }


    /**
     *
     * @param keys e.g. "a", "b"
     * @return
     */
    public static List<String> mget(String... keys) {
        try(RedisAccess ra = new RedisAccess()) {
            return ra.jedis.mget(keys);
        }
//        Jedis jedis = getJedis();
//        List<String> values = jedis.mget(keys);
//        close(jedis);
//        return values;
    }

    /**
     *
     * @param keysValues e.g. "a","1","b","2"
     * @return
     */
    public static String mset(String... keysValues) {
        try(RedisAccess ra = new RedisAccess()) {
            return ra.jedis.mset(keysValues);
        }
//        Jedis jedis = getJedis();
//        String status = jedis.mset(keysValues);
//        close(jedis);
//        return status;
    }

    /**
     * this is an atomic operation and will not perform any operation even if just a single key already exists
     * @param keysValues e.g. "a","1","b","2"
     * @return 1 if all the keys were set 0 if on key was set(at least one key already existed)
     */
    public static long msetnx(String... keysValues) {
        try(RedisAccess ra = new RedisAccess()) {
            return ra.jedis.msetnx(keysValues);
        }
//        Jedis jedis = getJedis();
//        long count = jedis.msetnx(keysValues);
//        close(jedis);
//        return count;
    }

    public static String getSet(String key, String value) {
        try(RedisAccess ra = new RedisAccess()) {
            return ra.jedis.getSet(key, value);
        }
//        Jedis jedis = getJedis();
//        String oldValue = jedis.getSet(key, value);
//        close(jedis);
//        return oldValue;
    }






    public static Set<String> smembers(String key) {
        try(RedisAccess ra = new RedisAccess()) {
            return ra.jedis.smembers(key);
        }
//        Jedis jedis = getJedis();
//        Set<String> set = jedis.smembers(key);
//        close(jedis);
//        return set;
    }

    public static boolean sismember(String key, String member) {
        try(RedisAccess ra = new RedisAccess()) {
            return ra.jedis.sismember(key, member);
        }

//        Jedis jedis = getJedis();
//        boolean b = jedis.sismember(key, member);
//        close(jedis);
//        return b;
    }

    public static long sadd(String key, String... members) {
        try(RedisAccess ra = new RedisAccess()) {
            return ra.jedis.sadd(key, members);
        }

//        Jedis jedis = getJedis();
//        long count = jedis.sadd(key, members);
//        close(jedis);
//        return count;
    }





    public static String type(String key) {
        try(RedisAccess ra = new RedisAccess()) {
            return ra.jedis.type(key);
        }
    }

    public Jedis getJedis(int dbIndex) {
        Jedis jedis = pool.getResource();
        if (dbIndex>0)
            jedis.select(dbIndex);
        return jedis;
    }
    public Jedis getJedis() {
        return getJedis(0);
    }

    public void close(Jedis jedis) {
        if(jedis != null) {
            jedis.close();
        }
    }

    public void close() {
        try {
            if (jedis != null) {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
