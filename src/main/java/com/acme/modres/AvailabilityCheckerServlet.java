package com.acme.modres;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@WebServlet({ "/resorts/availability" })
public class AvailabilityCheckerServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(AvailabilityCheckerServlet.class.getName());

  private static InitialContext context;

  private ReservationCheckerData reservationCheckerData;

  /** S3 bucket name read from environment variable; falls back to a default for local dev. */
  private static final String S3_BUCKET_NAME =
      System.getenv("S3_BUCKET_NAME") != null ? System.getenv("S3_BUCKET_NAME") : "modresorts-bucket";

  /** S3 key for the source reservations JSON object. */
  private static final String RESERVATIONS_S3_KEY = "reservations.json";

  /** S3 key for the exported reservations ZIP object. */
  private static final String RESERVATIONS_ZIP_S3_KEY = "reservations.zip";

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

      for (Reservation reservation : reservations) {
        try {
          Date fromDate = new SimpleDateFormat(Constants.DATA_FORMAT).parse(reservation.getFromDate());
          Date toDate = new SimpleDateFormat(Constants.DATA_FORMAT).parse(reservation.getToDate());
          Date selectedDate = reservationCheckerData.getSelectedDate();

          if (selectedDate.after(fromDate) && selectedDate.before(toDate)) {
            isAvailible = false;
            break;
          }
        } catch (ParseException ex) {
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
   * Exports reservations by reading the reservations.json object from Amazon S3,
   * compressing it into a ZIP archive in memory, and uploading the resulting ZIP
   * back to Amazon S3 as reservations.zip.
   *
   * <p>All file-system dependencies have been eliminated. The S3 bucket name is
   * resolved from the {@code S3_BUCKET_NAME} environment variable so that no
   * absolute paths are hard-coded in the application.
   *
   * @param selectedDateStr the selected date string (reserved for future filtering)
   * @return 0 on success, -1 on failure
   */
  protected int exportRevervations(String selectedDateStr) {
    String awsRegion = System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION") : "us-east-1";

    try (S3Client s3Client = S3Client.builder()
            .region(Region.of(awsRegion))
            .build()) {

      // --- Read reservations.json from S3 (replaces FileInputStream on local File) ---
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(S3_BUCKET_NAME)
          .key(RESERVATIONS_S3_KEY)
          .build();

      ResponseBytes<GetObjectResponse> s3ObjectBytes = s3Client.getObjectAsBytes(getObjectRequest);
      byte[] reservationJsonBytes = s3ObjectBytes.asByteArray();

      // --- Build the ZIP archive in memory (replaces FileOutputStream to user.home) ---
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
        ZipEntry zipEntry = new ZipEntry(RESERVATIONS_S3_KEY);
        zipOut.putNextEntry(zipEntry);

        byte[] buffer = new byte[1024];
        ByteArrayInputStream bais = new ByteArrayInputStream(reservationJsonBytes);
        int length;
        while ((length = bais.read(buffer)) >= 0) {
          zipOut.write(buffer, 0, length);
        }
        bais.close();
      }

      byte[] zipBytes = baos.toByteArray();

      // --- Upload the ZIP archive to S3 (replaces writing to user.home/reservations.zip) ---
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(S3_BUCKET_NAME)
          .key(RESERVATIONS_ZIP_S3_KEY)
          .contentType("application/zip")
          .contentLength((long) zipBytes.length)
          .build();

      s3Client.putObject(putObjectRequest, RequestBody.fromBytes(zipBytes));

      // --- Verify the uploaded ZIP exists in S3 (replaces local ZipValidator) ---
      try {
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
            .bucket(S3_BUCKET_NAME)
            .key(RESERVATIONS_ZIP_S3_KEY)
            .build();
        s3Client.headObject(headObjectRequest);
        return 0;
      } catch (NoSuchKeyException e) {
        logger.warning("Uploaded ZIP not found in S3 after put: " + e.getMessage());
        return -1;
      }

    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
  }

}
