package com.acme.modres.mbean.reservation;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.acme.modres.Constants;

/**
 * ReservationCheckerData holds the state for a reservation availability check.
 *
 * Blocker-14 (cr-java-0111): Replaced java.util.Date and java.text.SimpleDateFormat
 * with java.time API (Instant, DateTimeFormatter) standardized on UTC to eliminate
 * timezone and clock synchronization issues in distributed cloud environments.
 */
public class ReservationCheckerData {
  private ReservationList reservations;
  // Use Instant (UTC) instead of java.util.Date (blocker-14)
  private Instant selectedDate;
  private boolean available;

  public ReservationCheckerData(ReservationList reservations) {
    this.reservations = reservations;
    this.available = true;
  }

  public ReservationList getReservationList() {
    return reservations;
  }

  /**
   * Returns the selected date as an Instant (UTC).
   * Replaces java.util.Date with java.time.Instant for cloud-safe UTC handling.
   */
  public Instant getSelectedDate() {
    return selectedDate;
  }

  /**
   * Parses the date string and stores it as a UTC Instant.
   * Uses java.time DateTimeFormatter instead of java.text.SimpleDateFormat (blocker-14).
   */
  public boolean setSelectedDate(String dateStr) {
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT)
          .withZone(ZoneOffset.UTC);
      selectedDate = formatter.parse(dateStr, java.time.temporal.TemporalQueries.localDate())
          .atStartOfDay(ZoneOffset.UTC).toInstant();
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
