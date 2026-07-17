package com.acme.modres.db;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Spring Boot service replacing EJB 2.x Singleton/Startup bean.
 * Uses Spring's @Service stereotype and @Autowired dependency injection
 * instead of EJB container-managed lifecycle annotations (@Singleton, @Startup).
 * Compatible with AWS managed services (RDS) via Spring Data / HikariCP connection pool.
 */
@Service
public class ModResortsCustomerInformation {
  private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";

  // Spring-managed DataSource (configured via application.properties / environment variables)
  // Replaces EJB @Resource(lookup = "jdbc/ModResortsJndi") JNDI lookup
  @Autowired(required = false)
  private DataSource dataSource;

  public ArrayList<String> getCustomerInformation() {
    ArrayList<String> customerInfo = new ArrayList<>();

    if (dataSource == null) {
      return customerInfo;
    }

    // Use try-with-resources for automatic resource management —
    // prevents connection/statement/resultset leaks in cloud containers
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
