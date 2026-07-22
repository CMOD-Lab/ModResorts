package com.acme.modres.repository;

import com.acme.modres.entity.CabBooking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Repository
public class CabBookingRepository {

  private static final Logger logger = Logger.getLogger(CabBookingRepository.class.getName());

  @Autowired
  private DataSource dataSource;

  public void save(CabBooking cabBooking) throws SQLException {
    String sql = "INSERT INTO CAB_BOOKING (id, reservation_id, uber_ride_id, pickup_address, dropoff_address, " +
        "scheduled_datetime, booking_status, created_at, updated_at, driver_name, vehicle_details, estimated_arrival) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, cabBooking.getId());
      stmt.setString(2, cabBooking.getReservationId());
      stmt.setString(3, cabBooking.getUberRideId());
      stmt.setString(4, cabBooking.getPickupAddress());
      stmt.setString(5, cabBooking.getDropoffAddress());

      if (cabBooking.getScheduledDateTime() != null) {
        stmt.setTimestamp(6, Timestamp.valueOf(cabBooking.getScheduledDateTime()));
      } else {
        stmt.setTimestamp(6, null);
      }

      stmt.setString(7, cabBooking.getBookingStatus());
      stmt.setTimestamp(8, Timestamp.valueOf(cabBooking.getCreatedAt()));
      stmt.setTimestamp(9, Timestamp.valueOf(cabBooking.getUpdatedAt()));
      stmt.setString(10, cabBooking.getDriverName());
      stmt.setString(11, cabBooking.getVehicleDetails());
      stmt.setString(12, cabBooking.getEstimatedArrival());

      stmt.executeUpdate();
      logger.log(Level.FINE, "Saved cab booking with id: " + cabBooking.getId());
    }
  }

  public void update(CabBooking cabBooking) throws SQLException {
    String sql = "UPDATE CAB_BOOKING SET uber_ride_id = ?, pickup_address = ?, dropoff_address = ?, " +
        "scheduled_datetime = ?, booking_status = ?, updated_at = ?, driver_name = ?, " +
        "vehicle_details = ?, estimated_arrival = ? WHERE id = ?";

    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, cabBooking.getUberRideId());
      stmt.setString(2, cabBooking.getPickupAddress());
      stmt.setString(3, cabBooking.getDropoffAddress());

      if (cabBooking.getScheduledDateTime() != null) {
        stmt.setTimestamp(4, Timestamp.valueOf(cabBooking.getScheduledDateTime()));
      } else {
        stmt.setTimestamp(4, null);
      }

      stmt.setString(5, cabBooking.getBookingStatus());
      stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
      stmt.setString(7, cabBooking.getDriverName());
      stmt.setString(8, cabBooking.getVehicleDetails());
      stmt.setString(9, cabBooking.getEstimatedArrival());
      stmt.setString(10, cabBooking.getId());

      stmt.executeUpdate();
      logger.log(Level.FINE, "Updated cab booking with id: " + cabBooking.getId());
    }
  }

  public CabBooking findById(String id) throws SQLException {
    String sql = "SELECT * FROM CAB_BOOKING WHERE id = ?";

    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return mapResultSetToCabBooking(rs);
        }
      }
    }
    return null;
  }

  public List<CabBooking> findByReservationId(String reservationId) throws SQLException {
    List<CabBooking> cabBookings = new ArrayList<>();
    String sql = "SELECT * FROM CAB_BOOKING WHERE reservation_id = ? ORDER BY created_at DESC";

    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, reservationId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          cabBookings.add(mapResultSetToCabBooking(rs));
        }
      }
    }
    return cabBookings;
  }

  private CabBooking mapResultSetToCabBooking(ResultSet rs) throws SQLException {
    CabBooking cabBooking = new CabBooking();
    cabBooking.setId(rs.getString("id"));
    cabBooking.setReservationId(rs.getString("reservation_id"));
    cabBooking.setUberRideId(rs.getString("uber_ride_id"));
    cabBooking.setPickupAddress(rs.getString("pickup_address"));
    cabBooking.setDropoffAddress(rs.getString("dropoff_address"));

    Timestamp scheduledTimestamp = rs.getTimestamp("scheduled_datetime");
    if (scheduledTimestamp != null) {
      cabBooking.setScheduledDateTime(scheduledTimestamp.toLocalDateTime());
    }

    cabBooking.setBookingStatus(rs.getString("booking_status"));
    cabBooking.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
    cabBooking.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
    cabBooking.setDriverName(rs.getString("driver_name"));
    cabBooking.setVehicleDetails(rs.getString("vehicle_details"));
    cabBooking.setEstimatedArrival(rs.getString("estimated_arrival"));

    return cabBooking;
  }
}
