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
 * blocker-8, blocker-9 (cr-java-0085): Replaced EJB 2.x annotations (@Singleton, @Startup)
 * with Spring Boot @Service stereotype annotation.
 * EJB container dependencies (javax.ejb.*) have been removed and replaced with
 * Spring-managed bean lifecycle, aligning with cloud-native microservices patterns
 * and AWS managed services (RDS for persistence).
 */
@Service
public class ModResortsCustomerInformation {
  private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";

  // blocker-8, blocker-9: Replaced EJB @Resource injection with Spring @Autowired
  // for DataSource, compatible with Spring Boot auto-configuration and AWS RDS.
  @Autowired(required = false)
  private DataSource dataSource;

  public ArrayList<String> getCustomerInformation() {
    ArrayList<String> customerInfo = new ArrayList<>();

    if (dataSource == null) {
      return customerInfo;
    }

    // Use try-with-resources for automatic resource management (cloud-safe pattern)
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
