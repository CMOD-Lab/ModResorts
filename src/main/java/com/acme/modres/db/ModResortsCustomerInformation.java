package com.acme.modres.db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Customer information service migrated from EJB 2.x to Spring Boot.
 *
 * Cloud-native changes:
 * - Replaced EJB @Singleton/@Startup annotations with Spring @Service stereotype
 * - Replaced EJB @Resource DataSource injection with Spring @Autowired
 * - Spring Boot auto-configures HikariCP connection pooling for AWS RDS
 * - DataSource is externalized via application.properties / environment variables
 *   (spring.datasource.url, spring.datasource.username, spring.datasource.password)
 */
@Service
public class ModResortsCustomerInformation {
  private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";

  @Autowired
  private DataSource dataSource;

  public ArrayList<String> getCustomerInformation() {
    ArrayList<String> customerInfo = new ArrayList<>();

    // Use try-with-resources for automatic resource management (prevents leaks in cloud containers)
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(SELECT_CUSTOMERS_QUERY);
         ResultSet rs = stmt.executeQuery()) {

      while (rs.next()) {
        String info = rs.getString("INFO");
        customerInfo.add(info);
      }

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return customerInfo;
  }
}
