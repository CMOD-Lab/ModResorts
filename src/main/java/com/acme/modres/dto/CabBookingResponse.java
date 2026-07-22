package com.acme.modres.dto;

public class CabBookingResponse {
  private String cabBookingId;
  private String uberRideId;
  private String status;
  private String estimatedArrival;
  private String pickupAddress;
  private String dropoffAddress;
  private String scheduledTime;
  private String driverName;
  private String vehicleDetails;
  private String createdAt;
  private String message;
  private String refundInfo;

  public CabBookingResponse() {
  }

  public String getCabBookingId() {
    return cabBookingId;
  }

  public void setCabBookingId(String cabBookingId) {
    this.cabBookingId = cabBookingId;
  }

  public String getUberRideId() {
    return uberRideId;
  }

  public void setUberRideId(String uberRideId) {
    this.uberRideId = uberRideId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getEstimatedArrival() {
    return estimatedArrival;
  }

  public void setEstimatedArrival(String estimatedArrival) {
    this.estimatedArrival = estimatedArrival;
  }

  public String getPickupAddress() {
    return pickupAddress;
  }

  public void setPickupAddress(String pickupAddress) {
    this.pickupAddress = pickupAddress;
  }

  public String getDropoffAddress() {
    return dropoffAddress;
  }

  public void setDropoffAddress(String dropoffAddress) {
    this.dropoffAddress = dropoffAddress;
  }

  public String getScheduledTime() {
    return scheduledTime;
  }

  public void setScheduledTime(String scheduledTime) {
    this.scheduledTime = scheduledTime;
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

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getRefundInfo() {
    return refundInfo;
  }

  public void setRefundInfo(String refundInfo) {
    this.refundInfo = refundInfo;
  }
}
