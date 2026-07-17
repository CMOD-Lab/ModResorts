package com.acme.modres.mbean.reservation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.acme.modres.Constants;

/**
 * DateChecker - migrated from java.util.Date to java.time API.
 *
 * Replaces java.util.Date and SimpleDateFormat with java.time.LocalDate and
 * DateTimeFormatter (cr-java-0111 - Clock/Time Dependencies).
 * java.time API is timezone-aware and thread-safe, eliminating clock
 * synchronization issues in distributed cloud environments.
 * All date comparisons use UTC-standardized LocalDate operations.
 */
public class DateChecker implements Runnable {
  ReservationCheckerData data;
  List<Reservation> reservations;

  public DateChecker(ReservationCheckerData data) {
    this.data = data;
    this.reservations = data.getReservationList().getReservations();
  }

  public void run() {
    // Use java.time DateTimeFormatter (thread-safe, unlike SimpleDateFormat)
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);

    for (int i = 0; i < reservations.size(); i++) {
      Reservation reservation = reservations.get(i);
      // getSelectedDate() now returns LocalDate (java.time API)
      LocalDate selectedDate = data.getSelectedDate();

      try {
        // Parse dates using java.time LocalDate (replaces java.util.Date)
        LocalDate fromDate = LocalDate.parse(reservation.getFromDate(), formatter);
        LocalDate toDate = LocalDate.parse(reservation.getToDate(), formatter);

        if (selectedDate.isAfter(fromDate) && selectedDate.isBefore(toDate)) {
          data.setAvailablility(false);
          break;
        }
      } catch (DateTimeParseException ex) {
        ex.printStackTrace();
      }
    }
    data.setAvailablility(true);
  }
}
