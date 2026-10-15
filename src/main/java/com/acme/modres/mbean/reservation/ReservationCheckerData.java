package com.acme.modres.mbean.reservation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.acme.modres.Constants;

/**
 * ReservationCheckerData holds the state for a reservation availability check.
 *
 * Cloud-native change: Replaced java.util.Date and SimpleDateFormat with the
 * java.time API (LocalDate, DateTimeFormatter). java.util.Date relies on
 * server-local timezone settings which causes inconsistencies in distributed
 * cloud environments. LocalDate with a fixed pattern formatter is timezone-safe
 * and produces consistent results across all cloud instances and regions.
 */
public class ReservationCheckerData {
  private ReservationList reservations;
  // Replaced java.util.Date with java.time.LocalDate for cloud-safe UTC date handling
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
