package com.acme.modres;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.acme.modres.mbean.IOUtils;
import com.acme.modres.mbean.reservation.DateChecker;
import com.acme.modres.mbean.reservation.Reservation;
import com.acme.modres.mbean.reservation.ReservationCheckerData;
import com.acme.modres.util.ZipValidator;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@WebServlet({ "/resorts/availability" })
public class AvailabilityCheckerServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(AvailabilityCheckerServlet.class.getName());

  private static InitialContext context;

  private ReservationCheckerData reservationCheckerData;

  // S3 configuration from environment variables
  private static final String S3_BUCKET_NAME = System.getenv("S3_BUCKET_NAME") != null
      ? System.getenv("S3_BUCKET_NAME") : "modresorts-data";
  private static final String S3_RESERVATIONS_KEY = System.getenv("S3_RESERVATIONS_KEY") != null
      ? System.getenv("S3_RESERVATIONS_KEY") : "reservations.json";

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
          LocalDate fromDate = LocalDate.parse(reservation.getFromDate(), formatter);
          LocalDate toDate = LocalDate.parse(reservation.getToDate(), formatter);
          LocalDate selectedDate = reservationCheckerData.getSelectedDate()
              .toInstant().atZone(ZoneOffset.UTC).toLocalDate();

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
   * Exports reservations by reading from S3 and writing the zipped result back
   * to S3. Replaces local file system operations with Amazon S3 for durable,
   * cloud-native storage. Uses try-with-resources for automatic resource
   * management to prevent resource leaks in containerized AWS environments.
   */
  protected int exportRevervations(String selectedDateStr) {
    String bucketName = S3_BUCKET_NAME;
    String sourceKey = S3_RESERVATIONS_KEY;
    String zipKey = "reservations.zip";

    try (S3Client s3 = S3Client.create()) {
      // Read reservations.json from S3 using try-with-resources
      byte[] sourceBytes;
      try (InputStream s3InputStream = s3.getObject(
          GetObjectRequest.builder().bucket(bucketName).key(sourceKey).build())) {
        sourceBytes = s3InputStream.readAllBytes();
      }

      // Create zip in memory using try-with-resources
      byte[] zipBytes;
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
           ZipOutputStream zipOut = new ZipOutputStream(baos)) {
        ZipEntry zipEntry = new ZipEntry("reservations.json");
        zipOut.putNextEntry(zipEntry);
        zipOut.write(sourceBytes);
        zipOut.closeEntry();
        zipOut.finish();
        zipBytes = baos.toByteArray();
      }

      // Upload zip to S3 using try-with-resources pattern (PutObjectRequest is not AutoCloseable,
      // but the S3Client itself is managed by the outer try-with-resources)
      s3.putObject(
          PutObjectRequest.builder().bucket(bucketName).key(zipKey).build(),
          RequestBody.fromBytes(zipBytes));

      // Validate the zip from S3
      try (InputStream zipInputStream = s3.getObject(
          GetObjectRequest.builder().bucket(bucketName).key(zipKey).build())) {
        byte[] uploadedZipBytes = zipInputStream.readAllBytes();
        ZipValidator zipValidator = new ZipValidator(uploadedZipBytes);
        if (zipValidator.isValid()) {
          return 0;
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return -1;
  }

}
