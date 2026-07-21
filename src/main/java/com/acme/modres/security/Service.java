package com.acme.modres.security;

public class Service {
  public static final String OPERATION = "my-operation";

  public void operation() {
    // SecurityManager has been deprecated for removal since Java 17 (JEP 411).
    // System.getSecurityManager() is no longer used; security checks should be
    // handled via alternative mechanisms (e.g., AccessController, module system).
    System.out.println("Operation is executed");
  }
}
