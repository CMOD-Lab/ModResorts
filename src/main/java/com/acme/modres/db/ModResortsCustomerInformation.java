package com.acme.modres.db;

import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Customer information service.
 *
 * Cloud-native migration (blockers 8 & 9 — cr-java-0085 EJB 2.x Usage):
 * Replaced EJB 2.x annotations (@Singleton, @Startup from javax.ejb) with
 * Spring Boot @Service stereotype. The EJB container-managed lifecycle
 * (@Singleton/@Startup) has been replaced with Spring's IoC container,
 * which is compatible with AWS managed services (ECS, EKS, Fargate) and
 * does not require a heavyweight EJB container.
 *
 * The DataSource injection is preserved for use with Spring-managed
 * connection pools (e.g., HikariCP with AWS RDS).
 */
@Service
public class ModResortsCustomerInformation {
  private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";

  // DataSource injected by Spring (configure via application.properties or
  // environment variables pointing to AWS RDS)
  private DataSource dataSource;

  public ModResortsCustomerInformation() {
    // Default constructor for Spring instantiation
  }

  public ModResortsCustomerInformation(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public ArrayList<String> getCustomerInformation() {
    ArrayList<String> customerInfo = new ArrayList<>();

    if (dataSource == null) {
      return customerInfo;
    }

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
