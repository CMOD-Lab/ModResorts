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

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@WebServlet({ "/resorts/availability" })
public class AvailabilityCheckerServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(AvailabilityCheckerServlet.class.getName());

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

      // blocker-10, blocker-11: replaced java.util.Date/SimpleDateFormat with
      // java.time API (LocalDate) standardized on UTC
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
      for (Reservation reservation : reservations) {
        try {
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
   *
   * blocker-1: replaced hard-coded file path with S3 object storage.
   * blocker-2: replaced local file write with Amazon S3 durable storage.
   * blocker-4: replaced java.io.File-based storage with Amazon S3 client calls.
   * blocker-5: implemented try-with-resources for automatic resource management.
   */
  protected int exportRevervations(String selectedDateStr) {
    // blocker-2, blocker-4: read reservations content from classpath via IOUtils
    // (no local File dependency for the source data)
    byte[] reservationsContent = IOUtils.getReservationFileBytes();
    if (reservationsContent == null) {
      return -1;
    }

    // blocker-5: use try-with-resources to ensure all streams are automatically closed
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ZipOutputStream zipOut = new ZipOutputStream(baos)) {

      ZipEntry zipEntry = new ZipEntry("reservations.json");
      zipOut.putNextEntry(zipEntry);
      zipOut.write(reservationsContent);
      zipOut.closeEntry();
      zipOut.finish();

      byte[] zipBytes = baos.toByteArray();

      // blocker-1, blocker-2: upload ZIP to Amazon S3 instead of writing to local
      // file system. S3 key avoids any hard-coded host file system reference.
      String s3Key = "exports/reservations.zip";
      try (S3Client s3Client = S3Client.create()) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(S3_BUCKET_NAME)
            .key(s3Key)
            .contentType("application/zip")
            .build();
        s3Client.putObject(putRequest, RequestBody.fromBytes(zipBytes));
        logger.info("Reservations ZIP uploaded to S3: s3://" + S3_BUCKET_NAME + "/" + s3Key);
      }

      return 0;
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return -1;
  }

}
