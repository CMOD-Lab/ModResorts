package com.acme.modres.dto;

public class UberRideRequest {
  private double startLatitude;
  private double startLongitude;
  private double endLatitude;
  private double endLongitude;
  private String productId;
  private Long scheduledTime;

  public UberRideRequest() {
  }

  public double getStartLatitude() {
    return startLatitude;
  }

  public void setStartLatitude(double startLatitude) {
    this.startLatitude = startLatitude;
  }

  public double getStartLongitude() {
    return startLongitude;
  }

  public void setStartLongitude(double startLongitude) {
    this.startLongitude = startLongitude;
  }

  public double getEndLatitude() {
    return endLatitude;
  }

  public void setEndLatitude(double endLatitude) {
    this.endLatitude = endLatitude;
  }

  public double getEndLongitude() {
    return endLongitude;
  }

  public void setEndLongitude(double endLongitude) {
    this.endLongitude = endLongitude;
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public Long getScheduledTime() {
    return scheduledTime;
  }

  public void setScheduledTime(Long scheduledTime) {
    this.scheduledTime = scheduledTime;
  }
}
