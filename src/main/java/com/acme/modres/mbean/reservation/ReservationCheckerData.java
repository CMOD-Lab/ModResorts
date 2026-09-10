package com.acme.modres.mbean.reservation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.acme.modres.Constants;

/**
 * ReservationCheckerData - Updated for Java 17 compatibility.
 * Migrated from legacy java.util.Date/SimpleDateFormat to java.time.LocalDate/DateTimeFormatter.
 */
public class ReservationCheckerData {
  private ReservationList reservations;
  private LocalDate selectedDate;
  private boolean available;

  public ReservationCheckerData(ReservationList reservations) {
    this.reservations = reservations;
    this.available = true;
  }

  public ReservationList getReservationList() {
    return reservations;
  }

  public LocalDate getSelectedDate() {
    return selectedDate;
  }

  public boolean setSelectedDate(String dateStr) {
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
      selectedDate = LocalDate.parse(dateStr, formatter);
    } catch (DateTimeParseException e) {
      return false;
    }
    return true;
  }

  public boolean isAvailible() {
    return available;
  }

  public void setAvailablility(boolean available) {
    this.available = available;
  }
}
