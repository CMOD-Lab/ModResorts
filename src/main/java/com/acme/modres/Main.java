package com.acme.modres;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;
import java.util.logging.Logger;

/**
 * Main entry point for the ModResorts application as an executable JAR
 * with embedded Tomcat servlet container.
 *
 * blocker-15, blocker-16 (cr-java-0107): This class enables the application to run
 * as a self-contained executable JAR without requiring an external application server
 * (Tomcat, JBoss, WebLogic, WebSphere). This pattern supports deployment on
 * Amazon ECS, EKS, or AWS Fargate with simplified containerization and faster startup.
 */
public class Main {

  private static final Logger logger = Logger.getLogger(Main.class.getName());

  public static void main(String[] args) throws Exception {
    // Read port from environment variable (12-factor app principle)
    int port = 8080;
    String portEnv = System.getenv("PORT");
    if (portEnv != null && !portEnv.trim().isEmpty()) {
      try {
        port = Integer.parseInt(portEnv.trim());
      } catch (NumberFormatException e) {
        logger.warning("Invalid PORT environment variable value: " + portEnv + ". Using default port 8080.");
      }
    }

    Tomcat tomcat = new Tomcat();
    tomcat.setPort(port);
    tomcat.getConnector(); // Trigger connector creation

    // Configure the web application context
    String webappDir = new File("WebContent").getAbsolutePath();
    Context ctx = tomcat.addWebapp("", webappDir);

    // Add compiled classes and resources to the context
    File additionWebInfClasses = new File("target/classes");
    StandardRoot resources = new StandardRoot(ctx);
    resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
        additionWebInfClasses.getAbsolutePath(), "/"));
    ctx.setResources(resources);

    tomcat.start();
    logger.info("ModResorts application started on port " + port);
    tomcat.getServer().await();
  }
}
