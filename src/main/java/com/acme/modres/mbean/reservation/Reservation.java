package com.acme.modres.mbean.reservation;

import com.acme.modres.entity.CabBooking;
import java.util.ArrayList;
import java.util.List;

public class Reservation {
  private String fromDate;
  private String toDate;
  private List<CabBooking> cabBookings;

  public Reservation() {
    this.cabBookings = new ArrayList<>();
  }

  public Reservation(String fromDate, String toDate) {
    this.fromDate = fromDate;
    this.toDate = toDate;
    this.cabBookings = new ArrayList<>();
  }

  public void setFromDate(String fromDate) {
    this.fromDate = fromDate;
  }

  public void setToDate(String toDate) {
    this.toDate = toDate;
  }

  public String getFromDate() {
    return fromDate;
  }

  public String getToDate() {
    return toDate;
  }

  public List<CabBooking> getCabBookings() {
    return cabBookings;
  }

  public void setCabBookings(List<CabBooking> cabBookings) {
    this.cabBookings = cabBookings;
  }

  public void addCabBooking(CabBooking cabBooking) {
    this.cabBookings.add(cabBooking);
  }
}
