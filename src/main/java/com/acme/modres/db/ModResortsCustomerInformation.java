package com.acme.modres.db;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides customer information backed by Amazon ElastiCache (Redis) on EKS.
 *
 * <p>The singleton EJB pattern has been replaced with a Redis-backed store so
 * that all EKS pod replicas share a single, consistent data source.
 * Connection details are read from environment variables:
 * <ul>
 *   <li>{@code REDIS_HOST}  – ElastiCache primary endpoint (default: localhost)</li>
 *   <li>{@code REDIS_PORT}  – ElastiCache port              (default: 6379)</li>
 * </ul>
 */
public class ModResortsCustomerInformation {

    private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";
    private static final String REDIS_CUSTOMER_KEY     = "modresorts:customer:info";

    /** Redis connection pool – shared across all instances of this class. */
    private static final JedisPool jedisPool;

    static {
        String redisHost = System.getenv("REDIS_HOST") != null
                ? System.getenv("REDIS_HOST") : "localhost";
        int redisPort = System.getenv("REDIS_PORT") != null
                ? Integer.parseInt(System.getenv("REDIS_PORT")) : 6379;

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        jedisPool = new JedisPool(poolConfig, redisHost, redisPort);
    }

    // Removing DB connection for ease of demo setup
    // @Resource(lookup = "jdbc/ModResortsJndi")
    private DataSource dataSource;

    /**
     * Returns customer information.
     *
     * <p>The result list is cached in Redis under the key
     * {@value #REDIS_CUSTOMER_KEY} (pipe-delimited). On a cache miss the
     * database is queried and the result is stored in Redis so subsequent
     * calls – even from different EKS pod replicas – are served from the
     * shared cache.
     *
     * @return list of customer INFO strings
     */
    public ArrayList<String> getCustomerInformation() {
        // 1. Try to serve from Redis cache first
        try (Jedis jedis = jedisPool.getResource()) {
            String cached = jedis.get(REDIS_CUSTOMER_KEY);
            if (cached != null && !cached.isEmpty()) {
                ArrayList<String> result = new ArrayList<>();
                for (String entry : cached.split("\\|")) {
                    result.add(entry);
                }
                return result;
            }
        } catch (Exception e) {
            // Redis unavailable – fall through to DB query
            e.printStackTrace();
        }

        // 2. Cache miss: query the database
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ArrayList<String> customerInfo = new ArrayList<>();

        try {
            // Get a connection from the injected data source
            conn = dataSource.getConnection();
            // Create a prepared statement
            stmt = conn.prepareStatement(SELECT_CUSTOMERS_QUERY);
            // Execute the query
            rs = stmt.executeQuery();

            // Process the results
            while (rs.next()) {
                String info = rs.getString("INFO");
                customerInfo.add(info);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Close the result set, statement, and connection
            try {
                if (rs != null)   rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // 3. Populate Redis cache so all pod replicas benefit
        if (!customerInfo.isEmpty()) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.set(REDIS_CUSTOMER_KEY, String.join("|", customerInfo));
            } catch (Exception e) {
                // Non-fatal: cache write failure should not break the response
                e.printStackTrace();
            }
        }

        return customerInfo;
    }

    /**
     * Invalidates the Redis cache entry for customer information.
     * Call this whenever customer data is mutated so all pod replicas
     * pick up the fresh data on their next request.
     */
    public void invalidateCache() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(REDIS_CUSTOMER_KEY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
