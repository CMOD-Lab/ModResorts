package com.acme.modres;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
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
import com.acme.modres.mbean.reservation.ReservationCheckerData;
import com.acme.modres.mbean.reservation.Reservation;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@WebServlet({ "/resorts/availability" })
public class AvailabilityCheckerServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(AvailabilityCheckerServlet.class.getName());

  private static InitialContext context;

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

  protected int exportRevervations(String selectedDateStr) {
    // Retrieve S3 bucket name and object key from environment variables
    // instead of using a hard-coded local file path (cloud-native pattern)
    String s3BucketName = System.getenv("S3_BUCKET_NAME");
    String s3ObjectKey = System.getenv("S3_RESERVATIONS_ZIP_KEY") != null
        ? System.getenv("S3_RESERVATIONS_ZIP_KEY")
        : "exports/reservations.zip";
    String awsRegion = System.getenv("AWS_REGION") != null
        ? System.getenv("AWS_REGION")
        : "us-east-1";

    try {
      // Read the reservations.json source content via S3/classpath InputStream,
      // replacing java.io.File-based local file access (cloud-native pattern).
      InputStream sourceStream = IOUtils.getInputStreamFromS3OrClasspath("reservations.json");
      if (sourceStream == null) {
        logger.warning("Could not obtain reservations.json input stream from S3 or classpath");
        return -1;
      }

      // Build the zip content in memory using a ByteArrayOutputStream
      // to avoid writing to the ephemeral local file system.
      // This replaces: new File(zipPath) / new FileOutputStream(zipPath) / ZipValidator(new File(zipPath))
      byte[] zipBytes;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (InputStream src = sourceStream;
           ZipOutputStream zipOut = new ZipOutputStream(baos)) {
        ZipEntry zipEntry = new ZipEntry("reservations.json");
        zipOut.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = src.read(bytes)) >= 0) {
          zipOut.write(bytes, 0, length);
        }
      }
      zipBytes = baos.toByteArray();

      // Validate the in-memory zip by reading it back via a ByteArrayInputStream,
      // replacing the previous ZipValidator(new File(zipPath)) local-file validation.
      try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
              new ByteArrayInputStream(zipBytes))) {
        // Attempt to read at least one entry to confirm the zip is well-formed
        zis.getNextEntry();
      }

      // Upload the zip content directly to Amazon S3 using AWS SDK v2,
      // replacing the hard-coded local file path: userDirectory + "/reservations.zip"
      try (S3Client s3Client = S3Client.builder()
               .region(Region.of(awsRegion))
               .build()) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(s3BucketName)
            .key(s3ObjectKey)
            .contentType("application/zip")
            .build();

        s3Client.putObject(putObjectRequest,
            RequestBody.fromInputStream(new ByteArrayInputStream(zipBytes), zipBytes.length));

        logger.info("Reservations zip uploaded to S3: s3://" + s3BucketName + "/" + s3ObjectKey);
      }
      return 0;

    } catch (S3Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (Throwable e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return -1;
  }

}
