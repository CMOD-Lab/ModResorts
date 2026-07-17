package com.acme.modres;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.acme.modres.mbean.IOUtils;
import com.acme.modres.mbean.reservation.ReservationCheckerData;
import com.acme.modres.mbean.reservation.Reservation;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@WebServlet({ "/resorts/availability" })
public class AvailabilityCheckerServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(AvailabilityCheckerServlet.class.getName());

  // S3 bucket name read from environment variable for cloud-native configuration
  private static final String S3_BUCKET_NAME = System.getenv("S3_BUCKET_NAME") != null
      ? System.getenv("S3_BUCKET_NAME")
      : "modresorts-data";

  private ReservationCheckerData reservationCheckerData;

  @Override
  public void init() {
    // load reserved dates
    this.reservationCheckerData = new ReservationCheckerData(IOUtils.getReservationListFromConfig());
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

    String methodName = "doGet";
    logger.entering(AvailabilityCheckerServlet.class.getName(), methodName);
    int statusCode = 200;

    String selectedDateStr = request.getParameter("date");
    boolean parsedDate = reservationCheckerData.setSelectedDate(selectedDateStr);
    if (!parsedDate || reservationCheckerData.getReservationList() == null) {
      statusCode = 500;
      reservationCheckerData.setAvailablility(false);
    } else {
      List<Reservation> reservations = reservationCheckerData.getReservationList().getReservations();
      boolean isAvailible = true;

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
      for (Reservation reservation : reservations) {
        try {
          // Use java.time API (UTC-based) instead of java.util.Date for cloud compatibility
          LocalDate fromDate = LocalDate.parse(reservation.getFromDate(), formatter);
          LocalDate toDate = LocalDate.parse(reservation.getToDate(), formatter);
          LocalDate selectedDate = reservationCheckerData.getSelectedDate();

          if (selectedDate.isAfter(fromDate) && selectedDate.isBefore(toDate)) {
            isAvailible = false;
            break;
          }
        } catch (DateTimeParseException ex) {
          ex.printStackTrace();
        }
      }

      reservationCheckerData.setAvailablility(isAvailible);

      // Adjust the status code based on availability
      if (!isAvailible) {
        statusCode = 201;
      }
    }

    // Send the response
    PrintWriter out = response.getWriter();
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    out.print("{\"availability\": \"" + String.valueOf(reservationCheckerData.isAvailible()) + "\"}");
    response.setStatus(statusCode);
  }

  /**
   * Returns the weather information for a given city
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    doGet(request, response);
  }

  /**
   * Exports reservations as a ZIP file and uploads to Amazon S3 instead of
   * writing to the local file system. Uses try-with-resources for automatic
   * resource management to prevent resource leaks in cloud environments.
   */
  protected int exportRevervations(String selectedDateStr) {
    // Read reservations content from classpath resource
    byte[] reservationsContent = IOUtils.getResourceBytes("reservations.json");
    if (reservationsContent == null) {
      logger.warning("Could not read reservations.json from classpath");
      return -1;
    }

    // Build ZIP in memory using try-with-resources for automatic resource management
    byte[] zipBytes;
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ZipOutputStream zipOut = new ZipOutputStream(baos)) {

      ZipEntry zipEntry = new ZipEntry("reservations.json");
      zipOut.putNextEntry(zipEntry);
      zipOut.write(reservationsContent);
      zipOut.closeEntry();
      zipOut.finish();
      zipBytes = baos.toByteArray();

    } catch (IOException e) {
      e.printStackTrace();
      return -1;
    }

    // Upload ZIP to Amazon S3 instead of writing to local file system
    String s3Key = "exports/reservations-" + Instant.now().toEpochMilli() + ".zip";
    try (S3Client s3 = S3Client.create()) {
      PutObjectRequest putRequest = PutObjectRequest.builder()
          .bucket(S3_BUCKET_NAME)
          .key(s3Key)
          .contentType("application/zip")
          .build();
      s3.putObject(putRequest, RequestBody.fromBytes(zipBytes));
      logger.info("Reservations ZIP uploaded to S3: s3://" + S3_BUCKET_NAME + "/" + s3Key);
      return 0;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

}
