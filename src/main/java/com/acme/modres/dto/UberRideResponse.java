package com.acme.modres.dto;

public class UberRideResponse {
  private String requestId;
  private String status;
  private String eta;
  private String driverName;
  private String vehicleDetails;
  private String productId;
  private String destination;
  private String pickup;

  public UberRideResponse() {
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getEta() {
    return eta;
  }

  public void setEta(String eta) {
    this.eta = eta;
  }

  public String getDriverName() {
    return driverName;
  }

  public void setDriverName(String driverName) {
    this.driverName = driverName;
  }

  public String getVehicleDetails() {
    return vehicleDetails;
  }

  public void setVehicleDetails(String vehicleDetails) {
    this.vehicleDetails = vehicleDetails;
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public String getPickup() {
    return pickup;
  }

  public void setPickup(String pickup) {
    this.pickup = pickup;
  }
}
