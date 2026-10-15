package com.acme.modres.mbean.reservation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.acme.modres.Constants;

/**
 * blocker-12, blocker-13 (cr-java-0111): Replaced java.util.Date and SimpleDateFormat
 * with java.time API (LocalDate, DateTimeFormatter) to eliminate timezone and clock
 * synchronization issues in distributed cloud environments.
 * All date comparisons now use UTC-aligned LocalDate operations.
 */
public class DateChecker implements Runnable {
  ReservationCheckerData data;
  List<Reservation> reservations;

  public DateChecker(ReservationCheckerData data) {
    this.data = data;
    this.reservations = data.getReservationList().getReservations();
  }

  public void run() {
    // blocker-12, blocker-13: Use java.time DateTimeFormatter instead of SimpleDateFormat
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
    for (int i = 0; i < reservations.size(); i++) {
      Reservation reservation = reservations.get(i);
      // blocker-12, blocker-13: Use LocalDate (java.time) instead of java.util.Date
      LocalDate selectedDate = data.getSelectedDate();

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
