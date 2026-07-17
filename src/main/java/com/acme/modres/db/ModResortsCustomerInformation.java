package com.acme.modres.db;

import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * ModResortsCustomerInformation - Spring Boot Service component.
 *
 * Migrated from EJB 2.x (@Singleton, @Startup) to Spring Boot @Service
 * (cr-java-0085 - EJB 2.x Usage). Spring Boot @Service is a cloud-native
 * stereotype that works with AWS managed services (RDS, ECS, EKS) without
 * requiring a heavyweight EJB container.
 *
 * The DataSource is injected via Spring's dependency injection, enabling
 * use of AWS RDS-backed connection pools (HikariCP) configured externally.
 */
@Service
public class ModResortsCustomerInformation {
  private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";

  // DataSource injected by Spring Boot (configured via application.properties
  // or environment variables pointing to AWS RDS)
  private final DataSource dataSource;

  public ModResortsCustomerInformation(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public ArrayList<String> getCustomerInformation() {
    ArrayList<String> customerInfo = new ArrayList<>();

    // Use try-with-resources for automatic resource management
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
