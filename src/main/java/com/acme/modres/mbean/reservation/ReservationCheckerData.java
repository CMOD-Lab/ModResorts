package com.acme.modres.mbean.reservation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.acme.modres.Constants;

/**
 * ReservationCheckerData - migrated from java.util.Date to java.time API.
 *
 * Replaces java.util.Date and SimpleDateFormat with java.time.LocalDate and
 * DateTimeFormatter (cr-java-0111 - Clock/Time Dependencies).
 * LocalDate is immutable and timezone-neutral, ensuring consistent behavior
 * across distributed cloud environments and multiple AWS regions.
 */
public class ReservationCheckerData {
  private ReservationList reservations;
  // Replaced java.util.Date with java.time.LocalDate for cloud-safe date handling
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
      // Use java.time DateTimeFormatter (thread-safe) instead of SimpleDateFormat
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
