package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

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
    void testParameterizedConstructor_withList() {
        List<Reservation> list = new ArrayList<>();
        list.add(new Reservation("01/01/2024", "01/10/2024"));
        ReservationList rl = new ReservationList(list);
        assertEquals(1, rl.getReservations().size());
    }

    @Test
    void testAdd_singleReservation() {
        Reservation r = new Reservation("01/01/2024", "01/10/2024");
        reservationList.add(r);
        assertEquals(1, reservationList.getReservations().size());
    }

    @Test
    void testAdd_multipleReservations() {
        reservationList.add(new Reservation("01/01/2024", "01/10/2024"));
        reservationList.add(new Reservation("02/01/2024", "02/10/2024"));
        reservationList.add(new Reservation("03/01/2024", "03/10/2024"));
        assertEquals(3, reservationList.getReservations().size());
    }

    @Test
    void testGetReservations_returnsCorrectList() {
        Reservation r = new Reservation("06/01/2024", "06/30/2024");
        reservationList.add(r);
        List<Reservation> reservations = reservationList.getReservations();
        assertNotNull(reservations);
        assertEquals(1, reservations.size());
        assertEquals("06/01/2024", reservations.get(0).getFromDate());
    }

    @Test
    void testAdd_preservesOrder() {
        reservationList.add(new Reservation("01/01/2024", "01/10/2024"));
        reservationList.add(new Reservation("02/01/2024", "02/10/2024"));

        List<Reservation> reservations = reservationList.getReservations();
        assertEquals("01/01/2024", reservations.get(0).getFromDate());
        assertEquals("02/01/2024", reservations.get(1).getFromDate());
    }

    @Test
    void testParameterizedConstructor_withEmptyList() {
        List<Reservation> emptyList = new ArrayList<>();
        ReservationList rl = new ReservationList(emptyList);
        assertNotNull(rl.getReservations());
        assertTrue(rl.getReservations().isEmpty());
    }

    @Test
    void testParameterizedConstructor_withNullList() {
        ReservationList rl = new ReservationList(null);
        // The list is set to null
        assertNull(rl.getReservations());
    }
}
