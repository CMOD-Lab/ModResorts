package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class ReservationListTest {

  private ReservationList reservationList;

  @BeforeEach
  public void setUp() {
    reservationList = new ReservationList();
  }

  @Test
  public void testDefaultConstructor() {
    assertNotNull(reservationList);
    assertNotNull(reservationList.getReservations());
    assertTrue(reservationList.getReservations().isEmpty());
  }

  @Test
  public void testParameterizedConstructor() {
    List<Reservation> reservations = new ArrayList<>();
    reservations.add(new Reservation("2024-01-01", "2024-01-07"));
    reservations.add(new Reservation("2024-02-01", "2024-02-07"));

    ReservationList list = new ReservationList(reservations);
    assertEquals(2, list.getReservations().size());
  }

  @Test
  public void testAddReservation() {
    Reservation res1 = new Reservation("2024-03-01", "2024-03-05");
    reservationList.add(res1);

    assertEquals(1, reservationList.getReservations().size());
    assertEquals(res1, reservationList.getReservations().get(0));
  }

  @Test
  public void testAddMultipleReservations() {
    Reservation res1 = new Reservation("2024-03-01", "2024-03-05");
    Reservation res2 = new Reservation("2024-04-01", "2024-04-05");
    Reservation res3 = new Reservation("2024-05-01", "2024-05-05");

    reservationList.add(res1);
    reservationList.add(res2);
    reservationList.add(res3);

    assertEquals(3, reservationList.getReservations().size());
  }

  @Test
  public void testGetReservations() {
    List<Reservation> reservations = reservationList.getReservations();
    assertNotNull(reservations);
    assertTrue(reservations instanceof List);
  }

  @Test
  public void testEmptyReservationList() {
    assertTrue(reservationList.getReservations().isEmpty());
    assertEquals(0, reservationList.getReservations().size());
  }
}
