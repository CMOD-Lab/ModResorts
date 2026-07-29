package com.acme.modres.mbean.reservation;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

import com.acme.modres.Constants;

/**
 * Holds the state for a reservation availability check.
 *
 * Cloud-native migration (blocker 14 — cr-java-0111 Clock/Time Dependencies):
 * Replaced java.text.SimpleDateFormat with java.time.format.DateTimeFormatter
 * for date parsing. The internal selectedDate is now stored as a LocalDate
 * (UTC-standardized) to ensure consistent behavior across distributed cloud
 * environments and multiple time zones. A legacy java.util.Date accessor is
 * retained for backward compatibility with callers that still use Date.
 */
public class ReservationCheckerData {
  private ReservationList reservations;
  private LocalDate selectedLocalDate;
  private boolean available;

  public ReservationCheckerData(ReservationList reservations) {
    this.reservations = reservations;
    this.available = true;
  }

  public ReservationList getReservationList() {
    return reservations;
  }

  /**
   * Returns the selected date as a legacy java.util.Date (UTC midnight)
   * for backward compatibility with existing callers.
   */
  public Date getSelectedDate() {
    if (selectedLocalDate == null) {
      return null;
    }
    return Date.from(selectedLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant());
  }

  /**
   * Returns the selected date as a LocalDate (UTC-standardized).
   */
  public LocalDate getSelectedLocalDate() {
    return selectedLocalDate;
  }

  /**
   * Parses the date string using java.time DateTimeFormatter (UTC-standardized).
   * Replaces java.text.SimpleDateFormat to eliminate timezone inconsistencies
   * in distributed cloud environments.
   *
   * @param dateStr the date string in the format defined by Constants.DATA_FORMAT
   * @return true if parsing succeeded, false otherwise
   */
  public boolean setSelectedDate(String dateStr) {
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
      selectedLocalDate = LocalDate.parse(dateStr, formatter);
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
