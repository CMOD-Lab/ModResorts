package com.acme.modres.security;

public class Service {
  public static final String OPERATION = "my-operation";

  public void operation() {
    // SecurityManager is deprecated and removed in Java 21
    // Removed System.getSecurityManager() check as it's no longer supported
    // Alternative permission validation should be implemented at application level if needed
    System.out.println("Operation is executed");
  }
}
