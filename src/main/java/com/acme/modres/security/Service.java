package com.acme.modres.security;

/**
 * Service - Updated for Java 17 compatibility.
 * SecurityManager is deprecated for removal in Java 17 (JEP 411).
 * Removed SecurityManager usage.
 */
public class Service {
  public static final String OPERATION = "my-operation";

  public void operation() {
    // SecurityManager is deprecated for removal in Java 17 (JEP 411).
    // Removed System.getSecurityManager() call as it is deprecated and
    // will be removed in a future Java version.
    System.out.println("Operation is executed");
  }
}
