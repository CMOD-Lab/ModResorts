package com.acme.modres.mbean.reservation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.acme.modres.Constants;

/**
 * Holds reservation check state using java.time API (UTC-based) for cloud
 * compatibility. Replaces java.util.Date to eliminate timezone inconsistencies
 * across distributed cloud environments.
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

  /**
   * Returns the selected date as a UTC-based LocalDate.
   */
  public LocalDate getSelectedDate() {
    return selectedDate;
  }

  /**
   * Parses the date string using java.time API standardized on UTC.
   * Replaces java.util.Date / SimpleDateFormat for cloud-safe date handling.
   */
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
