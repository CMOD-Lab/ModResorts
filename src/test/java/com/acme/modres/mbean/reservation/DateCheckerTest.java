package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class DateCheckerTest {

    private ReservationCheckerData checkerData;

    @BeforeEach
    void setUp() {
        ReservationList reservationList = new ReservationList();
        checkerData = new ReservationCheckerData(reservationList);
    }

    @Test
    void testRun_withNoReservations_isAvailable() {
        checkerData.setSelectedDate("06/15/2024");
        DateChecker checker = new DateChecker(checkerData);
        checker.run();
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testRun_withReservationNotOverlapping_isAvailable() {
        ReservationList reservationList = new ReservationList();
        reservationList.add(new Reservation("01/01/2024", "01/10/2024"));
        checkerData = new ReservationCheckerData(reservationList);
        checkerData.setSelectedDate("06/15/2024");

        DateChecker checker = new DateChecker(checkerData);
        checker.run();

        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testRun_withReservationOverlapping_isNotAvailable() {
        ReservationList reservationList = new ReservationList();
        reservationList.add(new Reservation("06/01/2024", "06/30/2024"));
        checkerData = new ReservationCheckerData(reservationList);
        checkerData.setSelectedDate("06/15/2024");

        DateChecker checker = new DateChecker(checkerData);
        checker.run();

        assertFalse(checkerData.isAvailible());
    }

    @Test
    void testRun_withMultipleReservations_oneOverlapping_isNotAvailable() {
        ReservationList reservationList = new ReservationList();
        reservationList.add(new Reservation("01/01/2024", "01/10/2024"));
        reservationList.add(new Reservation("06/01/2024", "06/30/2024"));
        reservationList.add(new Reservation("12/01/2024", "12/31/2024"));
        checkerData = new ReservationCheckerData(reservationList);
        checkerData.setSelectedDate("06/15/2024");

        DateChecker checker = new DateChecker(checkerData);
        checker.run();

        assertFalse(checkerData.isAvailible());
    }

    @Test
    void testRun_withMultipleReservations_noneOverlapping_isAvailable() {
        ReservationList reservationList = new ReservationList();
        reservationList.add(new Reservation("01/01/2024", "01/10/2024"));
        reservationList.add(new Reservation("03/01/2024", "03/15/2024"));
        checkerData = new ReservationCheckerData(reservationList);
        checkerData.setSelectedDate("06/15/2024");

        DateChecker checker = new DateChecker(checkerData);
        checker.run();

        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testRun_withInvalidDateFormat_doesNotThrow() {
        ReservationList reservationList = new ReservationList();
        reservationList.add(new Reservation("invalid-date", "also-invalid"));
        checkerData = new ReservationCheckerData(reservationList);
        checkerData.setSelectedDate("06/15/2024");

        DateChecker checker = new DateChecker(checkerData);
        assertDoesNotThrow(() -> checker.run());
    }

    @Test
    void testConstructor_setsDataAndReservations() {
        ReservationList reservationList = new ReservationList();
        reservationList.add(new Reservation("01/01/2024", "01/10/2024"));
        checkerData = new ReservationCheckerData(reservationList);
        checkerData.setSelectedDate("06/15/2024");

        DateChecker checker = new DateChecker(checkerData);
        assertNotNull(checker);
    }
}
