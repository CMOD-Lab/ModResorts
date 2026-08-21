package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for ReservationList class.
 */
public class ReservationListTest {

    private ReservationList reservationList;

    @BeforeEach
    void setUp() {
        reservationList = new ReservationList();
    }

    @Test
    void testDefaultConstructor_createsInstance() {
        assertNotNull(reservationList);
    }

    @Test
    void testDefaultConstructor_emptyList() {
        assertNotNull(reservationList.getReservations());
        assertTrue(reservationList.getReservations().isEmpty());
    }

    @Test
    void testParameterizedConstructor_setsReservations() {
        List<Reservation> list = new ArrayList<>();
        list.add(new Reservation("04/10/2024", "04/15/2024"));
        ReservationList rl = new ReservationList(list);
        assertEquals(1, rl.getReservations().size());
    }

    @Test
    void testParameterizedConstructor_withEmptyList() {
        ReservationList rl = new ReservationList(new ArrayList<>());
        assertNotNull(rl.getReservations());
        assertTrue(rl.getReservations().isEmpty());
    }

    @Test
    void testAdd_singleReservation() {
        Reservation r = new Reservation("04/10/2024", "04/15/2024");
        reservationList.add(r);
        assertEquals(1, reservationList.getReservations().size());
    }

    @Test
    void testAdd_multipleReservations() {
        reservationList.add(new Reservation("04/10/2024", "04/15/2024"));
        reservationList.add(new Reservation("04/22/2024", "04/26/2024"));
        assertEquals(2, reservationList.getReservations().size());
    }

    @Test
    void testAdd_preservesOrder() {
        Reservation r1 = new Reservation("04/10/2024", "04/15/2024");
        Reservation r2 = new Reservation("04/22/2024", "04/26/2024");
        reservationList.add(r1);
        reservationList.add(r2);
        assertEquals("04/10/2024", reservationList.getReservations().get(0).getFromDate());
        assertEquals("04/22/2024", reservationList.getReservations().get(1).getFromDate());
    }

    @Test
    void testGetReservations_returnsCorrectList() {
        Reservation r = new Reservation("01/01/2024", "01/10/2024");
        reservationList.add(r);
        List<Reservation> result = reservationList.getReservations();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("01/01/2024", result.get(0).getFromDate());
    }

    @Test
    void testAdd_nullReservation() {
        reservationList.add(null);
        assertEquals(1, reservationList.getReservations().size());
        assertNull(reservationList.getReservations().get(0));
    }

    @Test
    void testParameterizedConstructor_withMultipleReservations() {
        List<Reservation> list = new ArrayList<>();
        list.add(new Reservation("04/10/2024", "04/15/2024"));
        list.add(new Reservation("04/22/2024", "04/26/2024"));
        ReservationList rl = new ReservationList(list);
        assertEquals(2, rl.getReservations().size());
    }
}
