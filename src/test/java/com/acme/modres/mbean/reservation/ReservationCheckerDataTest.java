package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for ReservationCheckerData class.
 */
public class ReservationCheckerDataTest {

    private ReservationList reservationList;
    private ReservationCheckerData checkerData;

    @BeforeEach
    void setUp() {
        reservationList = new ReservationList();
        checkerData = new ReservationCheckerData(reservationList);
    }

    @Test
    void testConstructor_createsInstance() {
        assertNotNull(checkerData);
    }

    @Test
    void testConstructor_setsReservationList() {
        assertNotNull(checkerData.getReservationList());
        assertSame(reservationList, checkerData.getReservationList());
    }

    @Test
    void testConstructor_defaultAvailabilityIsTrue() {
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testGetReservationList_returnsCorrectList() {
        ReservationList rl = new ReservationList();
        rl.add(new Reservation("04/10/2024", "04/15/2024"));
        ReservationCheckerData data = new ReservationCheckerData(rl);
        assertEquals(rl, data.getReservationList());
    }

    @Test
    void testSetSelectedDate_validDate_returnsTrue() {
        boolean result = checkerData.setSelectedDate("04/12/2024");
        assertTrue(result);
    }

    @Test
    void testSetSelectedDate_validDate_setsDate() {
        checkerData.setSelectedDate("04/12/2024");
        LocalDate date = checkerData.getSelectedDate();
        assertNotNull(date);
        assertEquals(4, date.getMonthValue());
        assertEquals(12, date.getDayOfMonth());
        assertEquals(2024, date.getYear());
    }

    @Test
    void testSetSelectedDate_invalidDate_returnsFalse() {
        boolean result = checkerData.setSelectedDate("not-a-date");
        assertFalse(result);
    }

    @Test
    void testSetSelectedDate_nullDate_returnsFalse() {
        boolean result = checkerData.setSelectedDate(null);
        assertFalse(result);
    }

    @Test
    void testSetSelectedDate_emptyString_returnsFalse() {
        boolean result = checkerData.setSelectedDate("");
        assertFalse(result);
    }

    @Test
    void testSetSelectedDate_wrongFormat_returnsFalse() {
        boolean result = checkerData.setSelectedDate("2024-04-12");
        assertFalse(result);
    }

    @Test
    void testGetSelectedDate_beforeSet_isNull() {
        assertNull(checkerData.getSelectedDate());
    }

    @Test
    void testIsAvailible_defaultTrue() {
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testSetAvailablility_toFalse() {
        checkerData.setAvailablility(false);
        assertFalse(checkerData.isAvailible());
    }

    @Test
    void testSetAvailablility_toTrue() {
        checkerData.setAvailablility(false);
        checkerData.setAvailablility(true);
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testSetSelectedDate_firstDayOfYear() {
        boolean result = checkerData.setSelectedDate("01/01/2024");
        assertTrue(result);
        LocalDate date = checkerData.getSelectedDate();
        assertEquals(1, date.getMonthValue());
        assertEquals(1, date.getDayOfMonth());
        assertEquals(2024, date.getYear());
    }

    @Test
    void testSetSelectedDate_lastDayOfYear() {
        boolean result = checkerData.setSelectedDate("12/31/2024");
        assertTrue(result);
        LocalDate date = checkerData.getSelectedDate();
        assertEquals(12, date.getMonthValue());
        assertEquals(31, date.getDayOfMonth());
        assertEquals(2024, date.getYear());
    }

    @Test
    void testSetSelectedDate_leapDay_validYear() {
        boolean result = checkerData.setSelectedDate("02/29/2024");
        assertTrue(result);
        LocalDate date = checkerData.getSelectedDate();
        assertEquals(2, date.getMonthValue());
        assertEquals(29, date.getDayOfMonth());
    }

    @Test
    void testSetSelectedDate_invalidMonth_returnsFalse() {
        boolean result = checkerData.setSelectedDate("13/01/2024");
        assertFalse(result);
    }

    @Test
    void testSetSelectedDate_invalidDay_returnsFalse() {
        boolean result = checkerData.setSelectedDate("01/32/2024");
        assertFalse(result);
    }
}
