package com.acme.modres.mbean.reservation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.acme.modres.Constants;

/**
 * Checks reservation date availability using java.time API (UTC-based).
 * Replaces java.util.Date / SimpleDateFormat to eliminate timezone
 * inconsistencies in distributed cloud environments.
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
      // Use java.time LocalDate (UTC-standardized) instead of java.util.Date
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
