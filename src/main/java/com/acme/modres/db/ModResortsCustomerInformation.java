package com.acme.modres.db;

import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

@Singleton
@Startup
public class ModResortsCustomerInformation {
  // PostgreSQL-compatible query: use lowercase table/column names (snake_case)
  // per PostgreSQL naming conventions
  private static final String SELECT_CUSTOMERS_QUERY = "SELECT info FROM customer";

  private static final Logger logger = Logger.getLogger(ModResortsCustomerInformation.class.getName());

  // Removing DB connection for ease of demo setup
  // @Resource(lookup = "jdbc/ModResortsJndi")
  private DataSource dataSource;

  public ArrayList<String> getCustomerInformation() {
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
        String info = rs.getString("info");
        customerInfo.add(info);
      }

    } catch (SQLException e) {
      logger.severe("SQLException while fetching customer information: " + e.getMessage());
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
        logger.severe("SQLException while closing resources: " + e.getMessage());
        e.printStackTrace();
      }
    }
    return customerInfo;
  }
}
