package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Reservation class.
 */
public class ReservationTest {

    private Reservation reservation;

    @BeforeEach
    void setUp() {
        reservation = new Reservation();
    }

    @Test
    void testDefaultConstructor_createsInstance() {
        assertNotNull(reservation);
    }

    @Test
    void testDefaultConstructor_nullFromDate() {
        assertNull(reservation.getFromDate());
    }

    @Test
    void testDefaultConstructor_nullToDate() {
        assertNull(reservation.getToDate());
    }

    @Test
    void testParameterizedConstructor_setsFromDate() {
        Reservation r = new Reservation("04/10/2024", "04/15/2024");
        assertEquals("04/10/2024", r.getFromDate());
    }

    @Test
    void testParameterizedConstructor_setsToDate() {
        Reservation r = new Reservation("04/10/2024", "04/15/2024");
        assertEquals("04/15/2024", r.getToDate());
    }

    @Test
    void testParameterizedConstructor_withNullValues() {
        Reservation r = new Reservation(null, null);
        assertNull(r.getFromDate());
        assertNull(r.getToDate());
    }

    @Test
    void testSetFromDate_andGetFromDate() {
        reservation.setFromDate("04/10/2024");
        assertEquals("04/10/2024", reservation.getFromDate());
    }

    @Test
    void testSetFromDate_withNull() {
        reservation.setFromDate(null);
        assertNull(reservation.getFromDate());
    }

    @Test
    void testSetToDate_andGetToDate() {
        reservation.setToDate("04/15/2024");
        assertEquals("04/15/2024", reservation.getToDate());
    }

    @Test
    void testSetToDate_withNull() {
        reservation.setToDate(null);
        assertNull(reservation.getToDate());
    }

    @Test
    void testSetAndGetBothDates() {
        reservation.setFromDate("01/01/2024");
        reservation.setToDate("12/31/2024");
        assertEquals("01/01/2024", reservation.getFromDate());
        assertEquals("12/31/2024", reservation.getToDate());
    }

    @Test
    void testSetFromDate_overwritesPreviousValue() {
        reservation.setFromDate("01/01/2024");
        reservation.setFromDate("06/01/2024");
        assertEquals("06/01/2024", reservation.getFromDate());
    }

    @Test
    void testSetToDate_overwritesPreviousValue() {
        reservation.setToDate("12/31/2024");
        reservation.setToDate("06/30/2024");
        assertEquals("06/30/2024", reservation.getToDate());
    }
}
