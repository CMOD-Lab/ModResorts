package com.acme.modres.mbean.reservation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.acme.modres.Constants;

/**
 * DateChecker checks whether a selected date falls within any reserved date range.
 *
 * Cloud-native change: Replaced java.util.Date and SimpleDateFormat with the
 * java.time API (LocalDate, DateTimeFormatter). java.util.Date is timezone-sensitive
 * and relies on server-local timezone settings, which causes scheduling failures
 * and time-related logic errors in distributed cloud environments across multiple
 * regions or containers. LocalDate with UTC-standardized parsing is timezone-safe
 * and consistent across all cloud instances.
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
      // Use LocalDate (java.time) instead of java.util.Date for cloud-safe date handling
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
