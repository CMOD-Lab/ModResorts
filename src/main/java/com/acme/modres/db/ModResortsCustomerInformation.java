package com.acme.modres.db;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Customer information data access class.
 *
 * Singleton state has been externalized to Amazon ElastiCache (Redis) so that
 * all EKS pod replicas share a single consistent data store.  The former
 * @Singleton / @Startup EJB annotations have been removed; state is now
 * stored and retrieved via a shared JedisPool backed by the Redis endpoint
 * supplied through the REDIS_HOST and REDIS_PORT environment variables.
 *
 * Required environment variables:
 *   REDIS_HOST  – ElastiCache Redis primary endpoint (e.g. my-cluster.abc123.ng.0001.use1.cache.amazonaws.com)
 *   REDIS_PORT  – Redis port (default: 6379)
 */
public class ModResortsCustomerInformation {

    private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";

    /** Redis key under which the customer-info list is cached. */
    private static final String REDIS_CUSTOMER_KEY = "modresorts:customer:info";

    // Removing DB connection for ease of demo setup
    // @Resource(lookup = "jdbc/ModResortsJndi")
    private DataSource dataSource;

    // ---------------------------------------------------------------------------
    // Redis / ElastiCache connection pool – configured via environment variables
    // ---------------------------------------------------------------------------
    private static final JedisPool jedisPool;

    static {
        String redisHost = System.getenv("REDIS_HOST") != null
                ? System.getenv("REDIS_HOST") : "localhost";
        int redisPort = 6379;
        String redisPortEnv = System.getenv("REDIS_PORT");
        if (redisPortEnv != null && !redisPortEnv.isEmpty()) {
            try {
                redisPort = Integer.parseInt(redisPortEnv);
            } catch (NumberFormatException ignored) {
                // fall back to default 6379
            }
        }
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        jedisPool = new JedisPool(poolConfig, redisHost, redisPort);
    }

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /**
     * Returns customer information.  Results are first looked up in the shared
     * Redis cache (ElastiCache).  On a cache miss the database is queried and
     * the result is stored in Redis so that every pod replica benefits from the
     * same cached data.
     */
    public ArrayList<String> getCustomerInformation() {
        // 1. Try to serve from Redis cache (shared across all pod replicas)
        List<String> cached = getFromRedis();
        if (cached != null && !cached.isEmpty()) {
            return new ArrayList<>(cached);
        }

        // 2. Cache miss – query the database
        ArrayList<String> customerInfo = fetchFromDatabase();

        // 3. Persist result in Redis so other replicas can reuse it
        if (!customerInfo.isEmpty()) {
            storeInRedis(customerInfo);
        }

        return customerInfo;
    }

    // ---------------------------------------------------------------------------
    // Redis helpers
    // ---------------------------------------------------------------------------

    private List<String> getFromRedis() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.lrange(REDIS_CUSTOMER_KEY, 0, -1);
        } catch (Exception e) {
            // Log and fall through to database on Redis unavailability
            e.printStackTrace();
            return null;
        }
    }

    private void storeInRedis(List<String> customerInfo) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Remove any stale data before re-populating
            jedis.del(REDIS_CUSTOMER_KEY);
            for (String info : customerInfo) {
                jedis.rpush(REDIS_CUSTOMER_KEY, info);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------------
    // Database helpers
    // ---------------------------------------------------------------------------

    private ArrayList<String> fetchFromDatabase() {
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
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return customerInfo;
    }
}
