package com.acme.modres;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
import com.acme.modres.mbean.reservation.ReservationCheckerData;
import com.acme.modres.mbean.reservation.Reservation;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import com.acme.modres.util.ZipValidator;

@WebServlet({ "/resorts/availability" })
public class AvailabilityCheckerServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(AvailabilityCheckerServlet.class.getName());

  private static InitialContext context;

  private ReservationCheckerData reservationCheckerData;

  // S3 bucket name read from environment variable for cloud-native configuration
  private static final String S3_BUCKET_NAME = System.getenv("S3_BUCKET_NAME") != null
      ? System.getenv("S3_BUCKET_NAME")
      : "modresorts-data";

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
          // Use java.time API (Instant/LocalDate) instead of java.util.Date for
          // cloud-safe UTC-based date comparisons (blocker-10, blocker-11)
          LocalDate fromDate = LocalDate.parse(reservation.getFromDate(), formatter);
          LocalDate toDate = LocalDate.parse(reservation.getToDate(), formatter);
          LocalDate selectedDate = reservationCheckerData.getSelectedDate()
              .atZone(ZoneOffset.UTC).toLocalDate();

          if (selectedDate.isAfter(fromDate) && selectedDate.isBefore(toDate)) {
            isAvailible = false;
            break;
          }
        } catch (Exception ex) {
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
   * Replaces local file system write operations (blocker-1, blocker-2, blocker-4, blocker-5)
   * with Amazon S3 object storage using AWS SDK for Java v2.
   * Uses try-with-resources for automatic resource management (blocker-5).
   */
  protected int exportRevervations(String selectedDateStr) {
    // Retrieve reservations content from S3 instead of local file system (blocker-1, blocker-4)
    String s3Key = "reservations.json";

    try (S3Client s3Client = S3Client.create()) {
      // Read reservations.json from S3 using try-with-resources (blocker-5)
      byte[] reservationBytes;
      try (software.amazon.awssdk.core.ResponseInputStream<software.amazon.awssdk.services.s3.model.GetObjectResponse> s3Object =
               s3Client.getObject(GetObjectRequest.builder()
                   .bucket(S3_BUCKET_NAME)
                   .key(s3Key)
                   .build())) {
        reservationBytes = s3Object.readAllBytes();
      }

      // Create ZIP in memory (no local file system dependency) (blocker-2)
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
        ZipEntry zipEntry = new ZipEntry("reservations.json");
        zipOut.putNextEntry(zipEntry);
        zipOut.write(reservationBytes);
        zipOut.closeEntry();
      }

      byte[] zipBytes = baos.toByteArray();

      // Upload ZIP to Amazon S3 instead of writing to local file system (blocker-2)
      String zipS3Key = "exports/reservations-" + Instant.now().toEpochMilli() + ".zip";
      s3Client.putObject(
          PutObjectRequest.builder()
              .bucket(S3_BUCKET_NAME)
              .key(zipS3Key)
              .contentType("application/zip")
              .build(),
          RequestBody.fromBytes(zipBytes));

      logger.info("Reservations ZIP exported to S3: s3://" + S3_BUCKET_NAME + "/" + zipS3Key);
      return 0;

    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

}
