package com.acme.modres.repository;

import com.acme.modres.entity.CabBooking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CabBookingRepository
 * Tests database operations for cab booking persistence
 */
public class CabBookingRepositoryTest {

  @Mock
  private DataSource dataSource;

  @Mock
  private Connection connection;

  @Mock
  private PreparedStatement preparedStatement;

  @Mock
  private ResultSet resultSet;

  @InjectMocks
  private CabBookingRepository cabBookingRepository;

  @BeforeEach
  public void setUp() throws SQLException {
    MockitoAnnotations.openMocks(this);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
  }

  @Test
  public void testSave_Success() throws SQLException {
    // Test successful cab booking save
    CabBooking cabBooking = new CabBooking();
    cabBooking.setId("CAB-123");
    cabBooking.setReservationId("RES-456");
    cabBooking.setUberRideId("UBER-789");
    cabBooking.setPickupAddress("Resort Address");
    cabBooking.setDropoffAddress("Airport");
    cabBooking.setBookingStatus("REQUESTED");
    cabBooking.setCreatedAt(LocalDateTime.now());
    cabBooking.setUpdatedAt(LocalDateTime.now());

    cabBookingRepository.save(cabBooking);

    verify(preparedStatement).executeUpdate();
    verify(preparedStatement).setString(1, "CAB-123");
    verify(preparedStatement).setString(2, "RES-456");
    verify(preparedStatement).setString(3, "UBER-789");
  }

  @Test
  public void testSave_WithScheduledDateTime() throws SQLException {
    // Test saving cab booking with scheduled pickup time
    CabBooking cabBooking = new CabBooking();
    cabBooking.setId("CAB-123");
    cabBooking.setReservationId("RES-456");
    cabBooking.setDropoffAddress("Airport");
    cabBooking.setScheduledDateTime(LocalDateTime.now().plusHours(2));
    cabBooking.setBookingStatus("REQUESTED");
    cabBooking.setCreatedAt(LocalDateTime.now());
    cabBooking.setUpdatedAt(LocalDateTime.now());

    cabBookingRepository.save(cabBooking);

    verify(preparedStatement).executeUpdate();
  }

  @Test
  public void testSave_SQLException() throws SQLException {
    // Test handling of SQL exceptions
    CabBooking cabBooking = new CabBooking();
    cabBooking.setId("CAB-123");
    cabBooking.setReservationId("RES-456");
    cabBooking.setDropoffAddress("Airport");
    cabBooking.setBookingStatus("REQUESTED");
    cabBooking.setCreatedAt(LocalDateTime.now());
    cabBooking.setUpdatedAt(LocalDateTime.now());

    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

    assertThrows(SQLException.class, () -> {
      cabBookingRepository.save(cabBooking);
    });
  }

  @Test
  public void testUpdate_Success() throws SQLException {
    // Test successful cab booking update
    CabBooking cabBooking = new CabBooking();
    cabBooking.setId("CAB-123");
    cabBooking.setUberRideId("UBER-789");
    cabBooking.setBookingStatus("CONFIRMED");
    cabBooking.setDriverName("John Doe");
    cabBooking.setVehicleDetails("Toyota Camry");

    cabBookingRepository.update(cabBooking);

    verify(preparedStatement).executeUpdate();
  }

  @Test
  public void testFindById_Found() throws SQLException {
    // Test finding cab booking by ID
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getString("id")).thenReturn("CAB-123");
    when(resultSet.getString("reservation_id")).thenReturn("RES-456");
    when(resultSet.getString("uber_ride_id")).thenReturn("UBER-789");
    when(resultSet.getString("pickup_address")).thenReturn("Resort");
    when(resultSet.getString("dropoff_address")).thenReturn("Airport");
    when(resultSet.getString("booking_status")).thenReturn("REQUESTED");
    when(resultSet.getTimestamp("created_at")).thenReturn(java.sql.Timestamp.valueOf(LocalDateTime.now()));
    when(resultSet.getTimestamp("updated_at")).thenReturn(java.sql.Timestamp.valueOf(LocalDateTime.now()));

    CabBooking result = cabBookingRepository.findById("CAB-123");

    assertNotNull(result);
    assertEquals("CAB-123", result.getId());
    assertEquals("RES-456", result.getReservationId());
    assertEquals("UBER-789", result.getUberRideId());
  }

  @Test
  public void testFindById_NotFound() throws SQLException {
    // Test finding non-existent cab booking
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    CabBooking result = cabBookingRepository.findById("CAB-999");

    assertNull(result);
  }

  @Test
  public void testFindByReservationId_Success() throws SQLException {
    // Test finding all cab bookings for a reservation
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true, true, false);
    when(resultSet.getString("id")).thenReturn("CAB-123", "CAB-124");
    when(resultSet.getString("reservation_id")).thenReturn("RES-456", "RES-456");
    when(resultSet.getString("uber_ride_id")).thenReturn("UBER-789", "UBER-790");
    when(resultSet.getString("pickup_address")).thenReturn("Resort", "Resort");
    when(resultSet.getString("dropoff_address")).thenReturn("Airport", "Mall");
    when(resultSet.getString("booking_status")).thenReturn("COMPLETED", "REQUESTED");
    when(resultSet.getTimestamp("created_at")).thenReturn(java.sql.Timestamp.valueOf(LocalDateTime.now()));
    when(resultSet.getTimestamp("updated_at")).thenReturn(java.sql.Timestamp.valueOf(LocalDateTime.now()));

    List<CabBooking> results = cabBookingRepository.findByReservationId("RES-456");

    assertNotNull(results);
    assertEquals(2, results.size());
    assertEquals("CAB-123", results.get(0).getId());
    assertEquals("CAB-124", results.get(1).getId());
  }

  @Test
  public void testFindByReservationId_Empty() throws SQLException {
    // Test finding cab bookings for reservation with no bookings
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    List<CabBooking> results = cabBookingRepository.findByReservationId("RES-999");

    assertNotNull(results);
    assertTrue(results.isEmpty());
  }

  @Test
  public void testConnectionManagement() throws SQLException {
    // Test that connections are properly closed (try-with-resources)
    CabBooking cabBooking = new CabBooking();
    cabBooking.setId("CAB-123");
    cabBooking.setReservationId("RES-456");
    cabBooking.setDropoffAddress("Airport");
    cabBooking.setBookingStatus("REQUESTED");
    cabBooking.setCreatedAt(LocalDateTime.now());
    cabBooking.setUpdatedAt(LocalDateTime.now());

    cabBookingRepository.save(cabBooking);

    verify(connection).close();
    verify(preparedStatement).close();
  }
}
