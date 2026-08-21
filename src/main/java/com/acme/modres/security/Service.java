package com.acme.modres.security;

public class Service {
  public static final String OPERATION = "my-operation";

  public void operation() {
    // SecurityManager has been removed in Java 17+ (deprecated since Java 17, removed in Java 21)
    // The following code has been updated to remove SecurityManager usage
    // SecurityManager securityManager = System.getSecurityManager();
    // if (securityManager != null) {
    //   // this SecurityManager method is not available in Java 11
    //   // securityManager.checkMemberAccess(Service.class, Member.PUBLIC);
    // }
    System.out.println("Operation is executed");
  }
}
