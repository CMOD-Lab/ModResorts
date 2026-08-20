package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

public class ReservationCheckerDataTest {

  private ReservationList reservationList;
  private ReservationCheckerData checkerData;

  @BeforeEach
  public void setUp() {
    reservationList = new ReservationList();
    reservationList.add(new Reservation("2024-01-01", "2024-01-07"));
    checkerData = new ReservationCheckerData(reservationList);
  }

  @Test
  public void testConstructor() {
    assertNotNull(checkerData);
    assertEquals(reservationList, checkerData.getReservationList());
    assertTrue(checkerData.isAvailible());
  }

  @Test
  public void testGetReservationList() {
    ReservationList result = checkerData.getReservationList();
    assertEquals(reservationList, result);
    assertEquals(1, result.getReservations().size());
  }

  @Test
  public void testSetSelectedDateValid() {
    boolean result = checkerData.setSelectedDate("2024-06-15");
    assertTrue(result);
    assertNotNull(checkerData.getSelectedDate());
  }

  @Test
  public void testSetSelectedDateInvalid() {
    boolean result = checkerData.setSelectedDate("invalid-date");
    assertFalse(result);
  }

  @Test
  public void testSetSelectedDateNull() {
    boolean result = checkerData.setSelectedDate(null);
    assertFalse(result);
  }

  @Test
  public void testSetSelectedDateEmpty() {
    boolean result = checkerData.setSelectedDate("");
    assertFalse(result);
  }

  @Test
  public void testIsAvailibleDefaultTrue() {
    assertTrue(checkerData.isAvailible());
  }

  @Test
  public void testSetAvailability() {
    checkerData.setAvailablility(false);
    assertFalse(checkerData.isAvailible());

    checkerData.setAvailablility(true);
    assertTrue(checkerData.isAvailible());
  }

  @Test
  public void testGetSelectedDate() {
    checkerData.setSelectedDate("2024-07-20");
    Date selectedDate = checkerData.getSelectedDate();
    assertNotNull(selectedDate);
  }

  @Test
  public void testSetSelectedDateWithVariousFormats() {
    // Valid date format
    assertTrue(checkerData.setSelectedDate("2024-12-31"));

    // Invalid formats
    assertFalse(checkerData.setSelectedDate("12/31/2024"));
    assertFalse(checkerData.setSelectedDate("2024/12/31"));
    assertFalse(checkerData.setSelectedDate("31-12-2024"));
  }
}
