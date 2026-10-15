package com.acme.modres;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
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
import com.acme.modres.util.ZipValidator;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@WebServlet({ "/resorts/availability" })
public class AvailabilityCheckerServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(AvailabilityCheckerServlet.class.getName());

  private ReservationCheckerData reservationCheckerData;

  // S3 configuration from environment variables (12-factor app principle)
  private static final String S3_BUCKET_NAME = System.getenv("S3_BUCKET_NAME") != null
      ? System.getenv("S3_BUCKET_NAME") : "modresorts-data";
  private static final String S3_RESERVATIONS_KEY = System.getenv("S3_RESERVATIONS_KEY") != null
      ? System.getenv("S3_RESERVATIONS_KEY") : "reservations/reservations.json";

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

      // blocker-10, blocker-11: Use java.time DateTimeFormatter instead of SimpleDateFormat
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
      for (Reservation reservation : reservations) {
        try {
          // blocker-10, blocker-11: Replace java.util.Date with java.time LocalDate (UTC-aligned)
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
   * Exports reservations as a ZIP file and uploads to Amazon S3.
   * blocker-1 (cr-java-0061): Replace hard-coded file paths with Amazon S3 object storage.
   * blocker-2 (cr-java-0062): Replace local file writes with Amazon S3 for durable storage.
   * blocker-4 (cr-java-0063): Migrate java.io.File operations to Amazon S3 using AWS SDK v2.
   * blocker-5 (cr-java-0098): Implement try-with-resources for automatic resource management.
   */
  protected int exportRevervations(String selectedDateStr) {
    // blocker-2, blocker-4: Read reservations content from S3 instead of local file
    byte[] reservationContent = readReservationsFromS3();
    if (reservationContent == null) {
      return -1;
    }

    // blocker-1, blocker-5: Use try-with-resources; write ZIP directly to S3
    // instead of local file system path (e.g. userDirectory + "/reservations.zip")
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ZipOutputStream zipOut = new ZipOutputStream(baos)) {

      ZipEntry zipEntry = new ZipEntry("reservations.json");
      zipOut.putNextEntry(zipEntry);
      zipOut.write(reservationContent);
      zipOut.closeEntry();
      zipOut.finish();

      byte[] zipBytes = baos.toByteArray();

      // blocker-2: Upload ZIP to Amazon S3 instead of writing to local file system
      String zipS3Key = System.getenv("S3_RESERVATIONS_ZIP_KEY") != null
          ? System.getenv("S3_RESERVATIONS_ZIP_KEY") : "exports/reservations.zip";

      // blocker-5: try-with-resources ensures S3Client is closed automatically
      try (S3Client s3Client = S3Client.builder().build()) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(S3_BUCKET_NAME)
            .key(zipS3Key)
            .contentType("application/zip")
            .build();
        s3Client.putObject(putRequest, RequestBody.fromBytes(zipBytes));
      }

      // Validate the zip content in-memory (no local file needed)
      ZipValidator zipValidator = new ZipValidator(zipBytes);
      if (zipValidator.isValid()) {
        return 0;
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return -1;
  }

  /**
   * Reads reservations JSON content from Amazon S3.
   * blocker-4: Replaces java.io.File-based read with S3 object retrieval.
   * blocker-5: Uses try-with-resources for automatic resource cleanup.
   */
  private byte[] readReservationsFromS3() {
    // blocker-5: try-with-resources ensures S3Client (AutoCloseable) is closed automatically
    try (S3Client s3Client = S3Client.builder().build()) {
      GetObjectRequest getRequest = GetObjectRequest.builder()
          .bucket(S3_BUCKET_NAME)
          .key(S3_RESERVATIONS_KEY)
          .build();
      // blocker-5: try-with-resources ensures ResponseInputStream is closed automatically
      try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getRequest)) {
        return s3Object.readAllBytes();
      }
    } catch (Exception e) {
      logger.warning("Failed to read reservations from S3: " + e.getMessage());
      return null;
    }
  }

}
