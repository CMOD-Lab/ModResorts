package com.acme.modres.dto;

public class CabBookingRequest {
  private String reservationId;
  private String destination;
  private String pickupTime;
  private String rideType;

  public CabBookingRequest() {
  }

  public String getReservationId() {
    return reservationId;
  }

  public void setReservationId(String reservationId) {
    this.reservationId = reservationId;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public String getPickupTime() {
    return pickupTime;
  }

  public void setPickupTime(String pickupTime) {
    this.pickupTime = pickupTime;
  }

  public String getRideType() {
    return rideType;
  }

  public void setRideType(String rideType) {
    this.rideType = rideType;
  }
}
