package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ReservationTest {

    @Test
    void testDefaultConstructor_createsInstance() {
        Reservation reservation = new Reservation();
        assertNotNull(reservation);
    }

    @Test
    void testParameterizedConstructor_setsFromAndToDate() {
        Reservation reservation = new Reservation("01/01/2024", "01/10/2024");
        assertEquals("01/01/2024", reservation.getFromDate());
        assertEquals("01/10/2024", reservation.getToDate());
    }

    @Test
    void testSetFromDate_andGetFromDate() {
        Reservation reservation = new Reservation();
        reservation.setFromDate("06/01/2024");
        assertEquals("06/01/2024", reservation.getFromDate());
    }

    @Test
    void testSetToDate_andGetToDate() {
        Reservation reservation = new Reservation();
        reservation.setToDate("06/30/2024");
        assertEquals("06/30/2024", reservation.getToDate());
    }

    @Test
    void testDefaultConstructor_fieldsAreNull() {
        Reservation reservation = new Reservation();
        assertNull(reservation.getFromDate());
        assertNull(reservation.getToDate());
    }

    @Test
    void testSetFromDate_withNull() {
        Reservation reservation = new Reservation("01/01/2024", "01/10/2024");
        reservation.setFromDate(null);
        assertNull(reservation.getFromDate());
    }

    @Test
    void testSetToDate_withNull() {
        Reservation reservation = new Reservation("01/01/2024", "01/10/2024");
        reservation.setToDate(null);
        assertNull(reservation.getToDate());
    }

    @Test
    void testParameterizedConstructor_withNullValues() {
        Reservation reservation = new Reservation(null, null);
        assertNull(reservation.getFromDate());
        assertNull(reservation.getToDate());
    }

    @Test
    void testSetFromDate_updatesValue() {
        Reservation reservation = new Reservation("01/01/2024", "01/10/2024");
        reservation.setFromDate("02/01/2024");
        assertEquals("02/01/2024", reservation.getFromDate());
    }

    @Test
    void testSetToDate_updatesValue() {
        Reservation reservation = new Reservation("01/01/2024", "01/10/2024");
        reservation.setToDate("02/28/2024");
        assertEquals("02/28/2024", reservation.getToDate());
    }
}
