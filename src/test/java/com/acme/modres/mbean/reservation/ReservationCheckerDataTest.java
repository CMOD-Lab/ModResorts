package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

public class ReservationCheckerDataTest {

    private ReservationCheckerData checkerData;
    private ReservationList reservationList;

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
        assertEquals(reservationList, checkerData.getReservationList());
    }

    @Test
    void testConstructor_defaultAvailabilityIsTrue() {
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testSetSelectedDate_withValidDate_returnsTrue() {
        boolean result = checkerData.setSelectedDate("06/15/2024");
        assertTrue(result);
    }

    @Test
    void testSetSelectedDate_withInvalidDate_returnsFalse() {
        boolean result = checkerData.setSelectedDate("invalid-date");
        assertFalse(result);
    }

    @Test
    void testSetSelectedDate_withNullDate_returnsFalse() {
        boolean result = checkerData.setSelectedDate(null);
        assertFalse(result);
    }

    @Test
    void testSetSelectedDate_withEmptyDate_returnsFalse() {
        boolean result = checkerData.setSelectedDate("");
        assertFalse(result);
    }

    @Test
    void testGetSelectedDate_afterValidSet_returnsDate() {
        checkerData.setSelectedDate("06/15/2024");
        Date selectedDate = checkerData.getSelectedDate();
        assertNotNull(selectedDate);
    }

    @Test
    void testGetSelectedDate_beforeSet_returnsNull() {
        assertNull(checkerData.getSelectedDate());
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
    void testGetReservationList_returnsCorrectList() {
        ReservationList list = new ReservationList();
        list.add(new Reservation("01/01/2024", "01/10/2024"));
        ReservationCheckerData data = new ReservationCheckerData(list);
        assertEquals(list, data.getReservationList());
    }

    @Test
    void testSetSelectedDate_withAlphaString_returnsFalse() {
        // Completely non-numeric string should fail
        assertFalse(checkerData.setSelectedDate("abc/def/ghij"));
    }

    @Test
    void testSetSelectedDate_withValidDateFormat_parsesCorrectly() {
        assertTrue(checkerData.setSelectedDate("01/01/2024"));
        assertNotNull(checkerData.getSelectedDate());
    }

    @Test
    void testSetSelectedDate_withISOFormat_returnsFalse() {
        // ISO format "2024-06-15" should fail for MM/dd/yyyy
        assertFalse(checkerData.setSelectedDate("2024-06-15"));
    }

    @Test
    void testIsAvailible_initiallyTrue() {
        ReservationCheckerData data = new ReservationCheckerData(new ReservationList());
        assertTrue(data.isAvailible());
    }
}
