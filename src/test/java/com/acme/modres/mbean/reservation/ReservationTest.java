package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class ReservationTest {

  private Reservation reservation;

  @BeforeEach
  public void setUp() {
    reservation = new Reservation();
  }

  @Test
  public void testDefaultConstructor() {
    assertNotNull(reservation);
    assertNull(reservation.getFromDate());
    assertNull(reservation.getToDate());
  }

  @Test
  public void testParameterizedConstructor() {
    Reservation res = new Reservation("2024-01-01", "2024-01-07");
    assertEquals("2024-01-01", res.getFromDate());
    assertEquals("2024-01-07", res.getToDate());
  }

  @Test
  public void testSetFromDate() {
    reservation.setFromDate("2024-06-15");
    assertEquals("2024-06-15", reservation.getFromDate());
  }

  @Test
  public void testSetToDate() {
    reservation.setToDate("2024-06-22");
    assertEquals("2024-06-22", reservation.getToDate());
  }

  @Test
  public void testSetFromDateWithNull() {
    reservation.setFromDate(null);
    assertNull(reservation.getFromDate());
  }

  @Test
  public void testSetToDateWithNull() {
    reservation.setToDate(null);
    assertNull(reservation.getToDate());
  }

  @Test
  public void testGettersAndSetters() {
    String fromDate = "2024-03-01";
    String toDate = "2024-03-10";

    reservation.setFromDate(fromDate);
    reservation.setToDate(toDate);

    assertEquals(fromDate, reservation.getFromDate());
    assertEquals(toDate, reservation.getToDate());
  }

  @Test
  public void testSetFromDateWithEmptyString() {
    reservation.setFromDate("");
    assertEquals("", reservation.getFromDate());
  }

  @Test
  public void testSetToDateWithEmptyString() {
    reservation.setToDate("");
    assertEquals("", reservation.getToDate());
  }
}
