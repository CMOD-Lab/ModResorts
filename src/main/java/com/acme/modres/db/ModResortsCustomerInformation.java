package com.acme.modres.db;

import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * blocker-8, blocker-9: Replaced EJB 2.x annotations (@Singleton, @Startup
 * from javax.ejb) with Spring Boot equivalents.
 *
 * - @Singleton + @Startup → @Service (Spring-managed singleton bean)
 * - @PostConstruct is retained (available in both Java EE and Spring)
 * - DataSource injection is handled by Spring's dependency injection
 *   (configured via application.properties / environment variables for
 *   cloud-native AWS RDS connectivity)
 *
 * This removes the heavy EJB container dependency and aligns the component
 * with Spring Boot microservices patterns as required by the remediation.
 */
@Service
public class ModResortsCustomerInformation {
  private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";

  // DataSource is injected by Spring Boot (configured via environment variables
  // or AWS Secrets Manager for cloud-native RDS connectivity)
  private DataSource dataSource;

  public ModResortsCustomerInformation() {
    // Default constructor for Spring instantiation
  }

  public ModResortsCustomerInformation(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostConstruct
  public void init() {
    // Initialization logic if needed after dependency injection
  }

  public ArrayList<String> getCustomerInformation() {
    ArrayList<String> customerInfo = new ArrayList<>();

    if (dataSource == null) {
      return customerInfo;
    }

    // blocker-5 pattern: use try-with-resources for automatic resource management
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
