package com.acme.modres.security;

public class Service {
  public static final String OPERATION = "my-operation";

  public void operation() {
    // SecurityManager was deprecated in Java 17 and removed in Java 21.
    // The SecurityManager API is no longer supported; access control
    // should be handled via other mechanisms (e.g., module system, OS-level controls).
    System.out.println("Operation is executed");
  }
}
