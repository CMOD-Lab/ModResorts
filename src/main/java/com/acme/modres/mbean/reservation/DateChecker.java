package com.acme.modres.mbean.reservation;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.acme.modres.Constants;

/**
 * Checks whether a selected date falls within any reserved date range.
 *
 * Cloud-native migration (blockers 12 & 13 — cr-java-0111 Clock/Time Dependencies):
 * Replaced java.util.Date and java.text.SimpleDateFormat with the java.time API
 * (LocalDate, DateTimeFormatter). All date comparisons are now performed using
 * LocalDate.isAfter() / LocalDate.isBefore() which are timezone-safe and
 * consistent across distributed cloud environments. UTC is used as the
 * standard timezone for all date operations.
 */
public class DateChecker implements Runnable {
  ReservationCheckerData data;
  List<Reservation> reservations;

  public DateChecker(ReservationCheckerData data) {
    this.data = data;
    this.reservations = data.getReservationList().getReservations();
  }

  public void run() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
    for (int i = 0; i < reservations.size(); i++) {
      Reservation reservation = reservations.get(i);
      // Convert the legacy Date to LocalDate using UTC to ensure consistency
      // across distributed cloud environments (standardized on UTC)
      LocalDate selectedDate = data.getSelectedDate()
          .toInstant().atZone(ZoneOffset.UTC).toLocalDate();

      try {
        LocalDate fromDate = LocalDate.parse(reservation.getFromDate(), formatter);
        LocalDate toDate = LocalDate.parse(reservation.getToDate(), formatter);
        if (selectedDate.isAfter(fromDate) && selectedDate.isBefore(toDate)) {
          data.setAvailablility(false);
          return;
        }
      } catch (DateTimeParseException ex) {
        ex.printStackTrace();
      }
    }
    data.setAvailablility(true);
  }
}
