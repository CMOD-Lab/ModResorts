package com.acme.modres.mbean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import com.acme.modres.mbean.reservation.ReservationList;
import com.acme.modres.util.JsonInputStream;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Utility class for I/O operations.
 *
 * blocker-3, blocker-6: Replaced local temporary file creation (File.createTempFile)
 * and local FileOutputStream writes with in-memory byte array processing and
 * Amazon S3 for durable, cloud-native storage. Temporary data is no longer
 * written to ephemeral local directories (/tmp) — all intermediate data is
 * handled in-memory and persisted to S3 when needed.
 */
public final class IOUtils {

  private static final Logger logger = Logger.getLogger(IOUtils.class.getName());

  // S3 bucket name read from environment variable for cloud-native configuration
  private static final String S3_BUCKET_NAME = System.getenv("S3_BUCKET_NAME") != null
      ? System.getenv("S3_BUCKET_NAME")
      : "modresorts-data";

  /**
   * Reads a classpath resource into a byte array without writing to the local
   * file system. This eliminates the local temporary storage dependency
   * (blocker-6) and local file write operation (blocker-3).
   *
   * @param path classpath-relative resource path
   * @return byte array of the resource content, or null on error
   */
  public static byte[] readResourceBytes(String path) {
    try (InputStream initialStream = IOUtils.class.getClassLoader().getResourceAsStream(path);
         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      if (initialStream == null) {
        logger.warning("Resource not found on classpath: " + path);
        return null;
      }
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = initialStream.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
      }
      return baos.toByteArray();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Returns the raw bytes of the reservations.json classpath resource.
   * Used by AvailabilityCheckerServlet to build the export ZIP in-memory
   * before uploading to S3 — no local file system access required.
   */
  public static byte[] getReservationFileBytes() {
    return readResourceBytes("reservations.json");
  }

  /**
   * Parses ops.json from the classpath into an OpMetadataList.
   * No temporary files are created; data is processed entirely in-memory.
   */
  public static OpMetadataList getOpListFromConfig() {
    byte[] bytes = readResourceBytes("ops.json");
    if (bytes == null) {
      return null;
    }
    try (JsonInputStream is = new JsonInputStream(bytes)) {
      OpMetadataList opList = new OpMetadataList();
      opList = (OpMetadataList) is.parseJsonAs(OpMetadataList.class);
      return opList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Parses reservations.json from the classpath into a ReservationList.
   * No temporary files are created; data is processed entirely in-memory.
   */
  public static ReservationList getReservationListFromConfig() {
    byte[] bytes = readResourceBytes("reservations.json");
    if (bytes == null) {
      return null;
    }
    try (JsonInputStream is = new JsonInputStream(bytes)) {
      ReservationList reservationList = new ReservationList();
      reservationList = (ReservationList) is.parseJsonAs(ReservationList.class);
      return reservationList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Uploads arbitrary byte content to Amazon S3 under the given key.
   * Replaces any pattern that previously wrote data to the local file system.
   *
   * @param key          S3 object key (path within the bucket)
   * @param content      byte array to upload
   * @param contentType  MIME type of the content
   */
  public static void uploadToS3(String key, byte[] content, String contentType) {
    try (S3Client s3Client = S3Client.create()) {
      PutObjectRequest putRequest = PutObjectRequest.builder()
          .bucket(S3_BUCKET_NAME)
          .key(key)
          .contentType(contentType)
          .build();
      s3Client.putObject(putRequest, RequestBody.fromBytes(content));
      logger.info("Uploaded to S3: s3://" + S3_BUCKET_NAME + "/" + key);
    } catch (S3Exception e) {
      logger.severe("Failed to upload to S3 key=" + key + ": " + e.awsErrorDetails().errorMessage());
      e.printStackTrace();
    }
  }

  /**
   * Downloads content from Amazon S3 and returns it as a byte array.
   *
   * @param key S3 object key
   * @return byte array of the object content, or null on error
   */
  public static byte[] downloadFromS3(String key) {
    try (S3Client s3Client = S3Client.create()) {
      GetObjectRequest getRequest = GetObjectRequest.builder()
          .bucket(S3_BUCKET_NAME)
          .key(key)
          .build();
      try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getRequest);
           ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = s3Object.read(buffer)) != -1) {
          baos.write(buffer, 0, bytesRead);
        }
        return baos.toByteArray();
      }
    } catch (S3Exception | IOException e) {
      logger.severe("Failed to download from S3 key=" + key + ": " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }
}
