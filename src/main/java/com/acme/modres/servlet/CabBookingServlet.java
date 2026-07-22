package com.acme.modres.servlet;

import com.acme.modres.Constants;
import com.acme.modres.dto.CabBookingRequest;
import com.acme.modres.dto.CabBookingResponse;
import com.acme.modres.dto.UberRideResponse;
import com.acme.modres.entity.CabBooking;
import com.acme.modres.repository.CabBookingRepository;
import com.acme.modres.service.UberApiService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet({"/resorts/cabs", "/resorts/cabs/*"})
public class CabBookingServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(CabBookingServlet.class.getName());
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

  @Inject
  private CabBookingRepository cabBookingRepository;

  @Inject
  private UberApiService uberApiService;

  private final Gson gson = new Gson();

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    logger.entering(CabBookingServlet.class.getName(), "doPost");

    try {
      CabBookingRequest bookingRequest = parseRequestBody(request, CabBookingRequest.class);

      if (bookingRequest.getReservationId() == null || bookingRequest.getReservationId().trim().isEmpty()) {
        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Reservation ID is required");
        return;
      }

      if (bookingRequest.getDestination() == null || bookingRequest.getDestination().trim().isEmpty()) {
        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Destination address is required");
        return;
      }

      LocalDateTime scheduledDateTime = null;
      Long scheduledTimeEpoch = null;
      if (bookingRequest.getPickupTime() != null && !bookingRequest.getPickupTime().trim().isEmpty()) {
        try {
          scheduledDateTime = LocalDateTime.parse(bookingRequest.getPickupTime(), DATE_TIME_FORMATTER);
          if (scheduledDateTime.isBefore(LocalDateTime.now())) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                "Scheduled pickup time must be in the future");
            return;
          }
          scheduledTimeEpoch = scheduledDateTime.atZone(ZoneId.of(Constants.RESORT_TIMEZONE))
              .toInstant().getEpochSecond();
        } catch (DateTimeParseException e) {
          sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
              "Invalid pickup time format. Use MM/dd/yyyy HH:mm");
          return;
        }
      }

      double pickupLat = Constants.RESORT_LATITUDE;
      double pickupLng = Constants.RESORT_LONGITUDE;

      double[] destCoords = geocodeAddress(bookingRequest.getDestination());

      String productId = bookingRequest.getRideType() != null ?
          bookingRequest.getRideType() : Constants.DEFAULT_UBER_PRODUCT;

      UberRideResponse uberResponse = uberApiService.requestRide(
          pickupLat, pickupLng, destCoords[0], destCoords[1], productId, scheduledTimeEpoch);

      CabBooking cabBooking = new CabBooking();
      cabBooking.setId(UUID.randomUUID().toString());
      cabBooking.setReservationId(bookingRequest.getReservationId());
      cabBooking.setUberRideId(uberResponse.getRequestId());
      cabBooking.setPickupAddress(Constants.RESORT_ADDRESS);
      cabBooking.setDropoffAddress(bookingRequest.getDestination());
      cabBooking.setScheduledDateTime(scheduledDateTime);
      cabBooking.setBookingStatus("REQUESTED");
      cabBooking.setCreatedAt(LocalDateTime.now());
      cabBooking.setUpdatedAt(LocalDateTime.now());
      cabBooking.setEstimatedArrival(uberResponse.getEta());

      cabBookingRepository.save(cabBooking);

      CabBookingResponse bookingResponse = buildCabBookingResponse(cabBooking);
      sendJsonResponse(response, HttpServletResponse.SC_OK, bookingResponse);

      logger.log(Level.INFO, "Created cab booking: " + cabBooking.getId());

    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error creating cab booking", e);
      sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Unable to process cab booking, please try again");
    } catch (IOException e) {
      logger.log(Level.WARNING, "Uber API error: " + e.getMessage(), e);
      sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Uber service error: " + e.getMessage());
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Unexpected error creating cab booking", e);
      sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "An unexpected error occurred");
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    logger.entering(CabBookingServlet.class.getName(), "doGet");

    String pathInfo = request.getPathInfo();

    try {
      if (pathInfo != null && pathInfo.length() > 1) {
        String cabBookingId = pathInfo.substring(1);
        handleGetCabBookingById(cabBookingId, request, response);
      } else {
        String reservationId = request.getParameter("reservationId");
        if (reservationId == null || reservationId.trim().isEmpty()) {
          sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Reservation ID is required");
          return;
        }
        handleGetCabBookingsByReservation(reservationId, response);
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error retrieving cab bookings", e);
      sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Unable to retrieve cab bookings");
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Unexpected error retrieving cab bookings", e);
      sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "An unexpected error occurred");
    }
  }

  @Override
  protected void doDelete(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    logger.entering(CabBookingServlet.class.getName(), "doDelete");

    String pathInfo = request.getPathInfo();
    if (pathInfo == null || pathInfo.length() <= 1) {
      sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Cab booking ID is required");
      return;
    }

    String cabBookingId = pathInfo.substring(1);

    try {
      CabBooking cabBooking = cabBookingRepository.findById(cabBookingId);
      if (cabBooking == null) {
        sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Cab booking not found");
        return;
      }

      String status = cabBooking.getBookingStatus();
      if ("IN_PROGRESS".equals(status) || "COMPLETED".equals(status)) {
        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
            "Cannot cancel ride that is " + status);
        return;
      }

      if (!"CANCELLED".equals(status)) {
        boolean cancelled = uberApiService.cancelRide(cabBooking.getUberRideId());
        if (cancelled) {
          cabBooking.setBookingStatus("CANCELLED");
          cabBookingRepository.update(cabBooking);
        }
      }

      CabBookingResponse bookingResponse = new CabBookingResponse();
      bookingResponse.setMessage("Cab booking cancelled successfully");
      bookingResponse.setStatus("CANCELLED");
      bookingResponse.setRefundInfo("Please check Uber app for cancellation policy details");

      sendJsonResponse(response, HttpServletResponse.SC_OK, bookingResponse);
      logger.log(Level.INFO, "Cancelled cab booking: " + cabBookingId);

    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error cancelling cab booking", e);
      sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Unable to cancel cab booking");
    } catch (IOException e) {
      logger.log(Level.WARNING, "Uber API error cancelling ride: " + e.getMessage(), e);
      sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Error cancelling ride: " + e.getMessage());
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Unexpected error cancelling cab booking", e);
      sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "An unexpected error occurred");
    }
  }

  private void handleGetCabBookingById(String cabBookingId, HttpServletRequest request,
                                        HttpServletResponse response) throws SQLException, IOException {
    CabBooking cabBooking = cabBookingRepository.findById(cabBookingId);
    if (cabBooking == null) {
      sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Cab booking not found");
      return;
    }

    String refresh = request.getParameter("refresh");
    if ("true".equalsIgnoreCase(refresh) && cabBooking.getUberRideId() != null) {
      try {
        UberRideResponse uberResponse = uberApiService.getRideStatus(cabBooking.getUberRideId());
        updateCabBookingFromUberResponse(cabBooking, uberResponse);
        cabBookingRepository.update(cabBooking);
      } catch (IOException e) {
        logger.log(Level.WARNING, "Could not refresh ride status from Uber: " + e.getMessage());
      }
    }

    CabBookingResponse bookingResponse = buildCabBookingResponse(cabBooking);
    sendJsonResponse(response, HttpServletResponse.SC_OK, bookingResponse);
  }

  private void handleGetCabBookingsByReservation(String reservationId, HttpServletResponse response)
      throws SQLException, IOException {
    List<CabBooking> cabBookings = cabBookingRepository.findByReservationId(reservationId);

    JsonObject responseObj = new JsonObject();
    responseObj.add("cabBookings", gson.toJsonTree(
        cabBookings.stream().map(this::buildCabBookingResponse).toArray()));

    sendJsonResponse(response, HttpServletResponse.SC_OK, responseObj);
  }

  private void updateCabBookingFromUberResponse(CabBooking cabBooking, UberRideResponse uberResponse) {
    if (uberResponse.getStatus() != null) {
      cabBooking.setBookingStatus(mapUberStatusToBookingStatus(uberResponse.getStatus()));
    }
    if (uberResponse.getDriverName() != null) {
      cabBooking.setDriverName(uberResponse.getDriverName());
    }
    if (uberResponse.getVehicleDetails() != null) {
      cabBooking.setVehicleDetails(uberResponse.getVehicleDetails());
    }
    if (uberResponse.getEta() != null) {
      cabBooking.setEstimatedArrival(uberResponse.getEta());
    }
  }

  private String mapUberStatusToBookingStatus(String uberStatus) {
    switch (uberStatus.toLowerCase()) {
      case "accepted":
      case "arriving":
        return "CONFIRMED";
      case "in_progress":
        return "IN_PROGRESS";
      case "completed":
        return "COMPLETED";
      case "cancelled":
        return "CANCELLED";
      default:
        return "REQUESTED";
    }
  }

  private CabBookingResponse buildCabBookingResponse(CabBooking cabBooking) {
    CabBookingResponse response = new CabBookingResponse();
    response.setCabBookingId(cabBooking.getId());
    response.setUberRideId(cabBooking.getUberRideId());
    response.setStatus(cabBooking.getBookingStatus());
    response.setPickupAddress(cabBooking.getPickupAddress());
    response.setDropoffAddress(cabBooking.getDropoffAddress());
    response.setEstimatedArrival(cabBooking.getEstimatedArrival());
    response.setDriverName(cabBooking.getDriverName());
    response.setVehicleDetails(cabBooking.getVehicleDetails());

    if (cabBooking.getScheduledDateTime() != null) {
      response.setScheduledTime(cabBooking.getScheduledDateTime().toString());
    }
    if (cabBooking.getCreatedAt() != null) {
      response.setCreatedAt(cabBooking.getCreatedAt().toString());
    }

    return response;
  }

  private double[] geocodeAddress(String address) {
    return new double[]{37.7749, -122.4194};
  }

  private <T> T parseRequestBody(HttpServletRequest request, Class<T> clazz) throws IOException {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader reader = request.getReader()) {
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }
    }
    return gson.fromJson(sb.toString(), clazz);
  }

  private void sendJsonResponse(HttpServletResponse response, int statusCode, Object data) throws IOException {
    response.setStatus(statusCode);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    try (ServletOutputStream out = response.getOutputStream()) {
      out.print(gson.toJson(data));
    }
  }

  private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
    JsonObject error = new JsonObject();
    error.addProperty("error", message);
    sendJsonResponse(response, statusCode, error);
  }
}
