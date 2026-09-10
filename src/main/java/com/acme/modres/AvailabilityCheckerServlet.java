package com.acme.modres;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
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

  /**
   * Exports reservations as a zip archive and uploads it to Amazon S3.
   *
   * <p>The local file system write operation (cr-java-0062) has been replaced with
   * Amazon S3 object storage using AWS SDK for Java v2. The reservations resource
   * is read directly from the classpath {@link java.io.InputStream} — no temporary
   * file is created on the local disk. The S3 bucket name and object key are read
   * from environment variables (RESERVATIONS_S3_BUCKET and RESERVATIONS_S3_KEY)
   * so that no absolute paths are embedded in the code. The zip archive is built
   * entirely in memory and uploaded directly to S3, eliminating any dependency on
   * the host file system.</p>
   */
  protected int exportRevervations(String selectedDateStr) {
    // Read S3 destination from environment variables — no hard-coded paths.
    String s3BucketName = System.getenv("RESERVATIONS_S3_BUCKET");
    String s3ObjectKey = System.getenv().getOrDefault("RESERVATIONS_S3_KEY", "reservations/reservations.zip");

    try {
      // Read the reservations resource directly from the classpath — no local file
      // write (cr-java-0062 remediation: replace local file writes with Amazon S3).
      byte[] resourceBytes = IOUtils.getResourceBytes("reservations.json");
      if (resourceBytes == null || resourceBytes.length == 0) {
        return -1;
      }

      // Build the zip archive entirely in memory to avoid local file system writes.
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
        ZipEntry zipEntry = new ZipEntry("reservations.json");
        zipOut.putNextEntry(zipEntry);
        zipOut.write(resourceBytes);
        zipOut.closeEntry();
      }

      byte[] zipBytes = baos.toByteArray();

      // Validate the in-memory zip using ZipInputStream before uploading.
      boolean zipValid = isZipValid(zipBytes);
      if (!zipValid) {
        return -1;
      }

      // Upload the zip archive to Amazon S3 using AWS SDK for Java v2.
      try (S3Client s3Client = S3Client.create()) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(s3BucketName)
            .key(s3ObjectKey)
            .contentType("application/zip")
            .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(zipBytes));
      }

      return 0;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (Throwable e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return -1;
  }

  /**
   * Validates an in-memory zip archive by attempting to read its entries.
   *
   * @param zipBytes the raw bytes of the zip archive
   * @return {@code true} if the archive is a valid (possibly empty) zip file
   */
  private boolean isZipValid(byte[] zipBytes) {
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
      // Attempt to read the first entry; a valid zip will return null (empty) or an entry.
      zis.getNextEntry();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

}
