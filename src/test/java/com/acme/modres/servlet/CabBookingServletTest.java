package com.acme.modres.servlet;

import com.acme.modres.entity.CabBooking;
import com.acme.modres.repository.CabBookingRepository;
import com.acme.modres.service.UberApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.StringReader;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CabBookingServlet
 * Tests HTTP endpoints for cab booking operations
 */
public class CabBookingServletTest {

  @Mock
  private CabBookingRepository cabBookingRepository;

  @Mock
  private UberApiService uberApiService;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private ServletOutputStream outputStream;

  @InjectMocks
  private CabBookingServlet cabBookingServlet;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    when(response.getOutputStream()).thenReturn(outputStream);
  }

  @Test
  public void testDoPost_ValidRequest_Success() throws Exception {
    // Test successful cab booking creation
    String requestBody = "{\"reservationId\":\"RES-123\",\"destination\":\"Airport\",\"rideType\":\"uberX\"}";
    when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

    cabBookingServlet.doPost(request, response);

    verify(response).setStatus(HttpServletResponse.SC_OK);
  }

  @Test
  public void testDoPost_MissingReservationId_BadRequest() throws Exception {
    // Test request without reservation ID
    String requestBody = "{\"destination\":\"Airport\"}";
    when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

    cabBookingServlet.doPost(request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void testDoPost_MissingDestination_BadRequest() throws Exception {
    // Test request without destination
    String requestBody = "{\"reservationId\":\"RES-123\"}";
    when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

    cabBookingServlet.doPost(request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void testDoPost_InvalidPickupTime_BadRequest() throws Exception {
    // Test request with invalid pickup time format
    String requestBody = "{\"reservationId\":\"RES-123\",\"destination\":\"Airport\",\"pickupTime\":\"invalid\"}";
    when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

    cabBookingServlet.doPost(request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void testDoPost_DatabaseError_InternalServerError() throws Exception {
    // Test database error handling
    String requestBody = "{\"reservationId\":\"RES-123\",\"destination\":\"Airport\"}";
    when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));
    doThrow(new SQLException("Database error")).when(cabBookingRepository).save(any(CabBooking.class));

    cabBookingServlet.doPost(request, response);

    verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void testDoGet_WithReservationId_Success() throws Exception {
    // Test retrieving cab bookings by reservation ID
    when(request.getParameter("reservationId")).thenReturn("RES-123");
    when(request.getPathInfo()).thenReturn(null);

    cabBookingServlet.doGet(request, response);

    verify(cabBookingRepository).findByReservationId("RES-123");
  }

  @Test
  public void testDoGet_WithCabBookingId_Success() throws Exception {
    // Test retrieving specific cab booking by ID
    when(request.getPathInfo()).thenReturn("/CAB-123");
    when(request.getParameter("refresh")).thenReturn("false");
    CabBooking mockBooking = new CabBooking();
    mockBooking.setId("CAB-123");
    when(cabBookingRepository.findById("CAB-123")).thenReturn(mockBooking);

    cabBookingServlet.doGet(request, response);

    verify(cabBookingRepository).findById("CAB-123");
  }

  @Test
  public void testDoGet_WithRefresh_FetchesUberStatus() throws Exception {
    // Test refreshing ride status from Uber API
    when(request.getPathInfo()).thenReturn("/CAB-123");
    when(request.getParameter("refresh")).thenReturn("true");
    CabBooking mockBooking = new CabBooking();
    mockBooking.setId("CAB-123");
    mockBooking.setUberRideId("UBER-456");
    when(cabBookingRepository.findById("CAB-123")).thenReturn(mockBooking);

    cabBookingServlet.doGet(request, response);

    verify(uberApiService).getRideStatus("UBER-456");
  }

  @Test
  public void testDoGet_MissingReservationId_BadRequest() throws Exception {
    // Test request without reservation ID parameter
    when(request.getParameter("reservationId")).thenReturn(null);
    when(request.getPathInfo()).thenReturn(null);

    cabBookingServlet.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void testDoDelete_Success() throws Exception {
    // Test successful cab booking cancellation
    when(request.getPathInfo()).thenReturn("/CAB-123");
    CabBooking mockBooking = new CabBooking();
    mockBooking.setId("CAB-123");
    mockBooking.setUberRideId("UBER-456");
    mockBooking.setBookingStatus("REQUESTED");
    when(cabBookingRepository.findById("CAB-123")).thenReturn(mockBooking);
    when(uberApiService.cancelRide("UBER-456")).thenReturn(true);

    cabBookingServlet.doDelete(request, response);

    verify(uberApiService).cancelRide("UBER-456");
    verify(cabBookingRepository).update(mockBooking);
    verify(response).setStatus(HttpServletResponse.SC_OK);
  }

  @Test
  public void testDoDelete_MissingCabBookingId_BadRequest() throws Exception {
    // Test delete request without cab booking ID
    when(request.getPathInfo()).thenReturn(null);

    cabBookingServlet.doDelete(request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void testDoDelete_NotFound() throws Exception {
    // Test cancelling non-existent cab booking
    when(request.getPathInfo()).thenReturn("/CAB-123");
    when(cabBookingRepository.findById("CAB-123")).thenReturn(null);

    cabBookingServlet.doDelete(request, response);

    verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  public void testDoDelete_AlreadyInProgress_BadRequest() throws Exception {
    // Test cancelling a ride that is already in progress
    when(request.getPathInfo()).thenReturn("/CAB-123");
    CabBooking mockBooking = new CabBooking();
    mockBooking.setId("CAB-123");
    mockBooking.setBookingStatus("IN_PROGRESS");
    when(cabBookingRepository.findById("CAB-123")).thenReturn(mockBooking);

    cabBookingServlet.doDelete(request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void testDoDelete_AlreadyCompleted_BadRequest() throws Exception {
    // Test cancelling a completed ride
    when(request.getPathInfo()).thenReturn("/CAB-123");
    CabBooking mockBooking = new CabBooking();
    mockBooking.setId("CAB-123");
    mockBooking.setBookingStatus("COMPLETED");
    when(cabBookingRepository.findById("CAB-123")).thenReturn(mockBooking);

    cabBookingServlet.doDelete(request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }
}
