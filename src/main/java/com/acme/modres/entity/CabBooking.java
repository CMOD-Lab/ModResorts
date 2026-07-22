package com.acme.modres.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.Column;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CAB_BOOKING")
public class CabBooking {

  @Id
  @Column(name = "id", length = 36)
  private String id;

  @Column(name = "reservation_id", length = 36, nullable = false)
  private String reservationId;

  @Column(name = "uber_ride_id", length = 255)
  private String uberRideId;

  @Column(name = "pickup_address", length = 500)
  private String pickupAddress;

  @Column(name = "dropoff_address", length = 500, nullable = false)
  private String dropoffAddress;

  @Column(name = "scheduled_datetime")
  private LocalDateTime scheduledDateTime;

  @Column(name = "booking_status", length = 20, nullable = false)
  private String bookingStatus;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "driver_name", length = 255)
  private String driverName;

  @Column(name = "vehicle_details", length = 255)
  private String vehicleDetails;

  @Column(name = "estimated_arrival")
  private String estimatedArrival;

  public CabBooking() {
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getReservationId() {
    return reservationId;
  }

  public void setReservationId(String reservationId) {
    this.reservationId = reservationId;
  }

  public String getUberRideId() {
    return uberRideId;
  }

  public void setUberRideId(String uberRideId) {
    this.uberRideId = uberRideId;
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

  public LocalDateTime getScheduledDateTime() {
    return scheduledDateTime;
  }

  public void setScheduledDateTime(LocalDateTime scheduledDateTime) {
    this.scheduledDateTime = scheduledDateTime;
  }

  public String getBookingStatus() {
    return bookingStatus;
  }

  public void setBookingStatus(String bookingStatus) {
    this.bookingStatus = bookingStatus;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
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

  public String getEstimatedArrival() {
    return estimatedArrival;
  }

  public void setEstimatedArrival(String estimatedArrival) {
    this.estimatedArrival = estimatedArrival;
  }
}
