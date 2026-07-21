package com.acme.modres.mbean.reservation;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.acme.modres.Constants;

/**
 * DateChecker checks reservation availability for a selected date.
 *
 * Blocker-12 and Blocker-13 (cr-java-0111): Replaced java.util.Date and
 * java.text.SimpleDateFormat with java.time API (LocalDate, DateTimeFormatter)
 * standardized on UTC to eliminate timezone and clock synchronization issues
 * in distributed cloud environments.
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
      // Use java.time Instant converted to UTC LocalDate (blocker-12, blocker-13)
      LocalDate selectedDate = data.getSelectedDate()
          .atZone(ZoneOffset.UTC).toLocalDate();

      try {
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
