package com.acme.modres.mbean.reservation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.acme.modres.Constants;

/**
 * blocker-14 (cr-java-0111): Replaced java.util.Date and SimpleDateFormat with
 * java.time API (LocalDate, DateTimeFormatter) to eliminate timezone and clock
 * synchronization issues in distributed cloud environments.
 * All date storage and parsing now uses UTC-aligned LocalDate.
 */
public class ReservationCheckerData {
  private ReservationList reservations;
  // blocker-14: Replaced java.util.Date with java.time.LocalDate
  private LocalDate selectedDate;
  private boolean available;

  public ReservationCheckerData(ReservationList reservations) {
    this.reservations = reservations;
    this.available = true;
  }

  public ReservationList getReservationList() {
    return reservations;
  }

  // blocker-14: Returns LocalDate instead of java.util.Date
  public LocalDate getSelectedDate() {
    return selectedDate;
  }

  public boolean setSelectedDate(String dateStr) {
    try {
      // blocker-14: Use DateTimeFormatter (java.time) instead of SimpleDateFormat
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
      selectedDate = LocalDate.parse(dateStr, formatter);
    } catch (DateTimeParseException e) {
      return false;
    } catch (Exception e) {
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
