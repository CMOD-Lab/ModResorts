package com.acme.modres.mbean;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
 * Cloud-native refactoring:
 * - Removed all local file system write operations (java.io.File, FileOutputStream)
 * - Removed reliance on ephemeral local temporary directories (File.createTempFile)
 * - Resources are read directly from the classpath (no temp file intermediary)
 * - Write operations are delegated to Amazon S3 for durable, scalable storage
 */
public final class IOUtils {

  private static final Logger logger = Logger.getLogger(IOUtils.class.getName());

  // S3 bucket name sourced from environment variable for cloud-native config
  private static final String S3_BUCKET_NAME = System.getenv("S3_BUCKET_NAME") != null
      ? System.getenv("S3_BUCKET_NAME")
      : "modresorts-data";

  /**
   * Reads a classpath resource and returns its raw bytes.
   * Replaces the previous pattern of writing to a local temp file (File.createTempFile)
   * which relied on ephemeral local temporary storage incompatible with cloud containers.
   */
  public static byte[] getResourceBytes(String path) {
    try (InputStream stream = IOUtils.class.getClassLoader().getResourceAsStream(path)) {
      if (stream == null) {
        logger.warning("Resource not found on classpath: " + path);
        return null;
      }
      return stream.readAllBytes();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Writes data to Amazon S3 instead of the local file system.
   * Replaces local FileOutputStream writes to ensure data durability and
   * availability across container restarts and multi-instance deployments.
   */
  public static void writeToS3(String s3Key, byte[] data, String contentType) {
    try (S3Client s3 = S3Client.create()) {
      PutObjectRequest putRequest = PutObjectRequest.builder()
          .bucket(S3_BUCKET_NAME)
          .key(s3Key)
          .contentType(contentType)
          .build();
      s3.putObject(putRequest, RequestBody.fromBytes(data));
      logger.info("Data written to S3: s3://" + S3_BUCKET_NAME + "/" + s3Key);
    } catch (S3Exception e) {
      logger.severe("Failed to write to S3: " + e.awsErrorDetails().errorMessage());
      e.printStackTrace();
    }
  }

  /**
   * Reads a JSON resource from the classpath and parses it as OpMetadataList.
   * Uses try-with-resources on a stream-based JsonInputStream — no temp file needed.
   */
  public static OpMetadataList getOpListFromConfig() {
    byte[] bytes = getResourceBytes("ops.json");
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
   * Reads a JSON resource from the classpath and parses it as ReservationList.
   * Uses try-with-resources on a stream-based JsonInputStream — no temp file needed.
   */
  public static ReservationList getReservationListFromConfig() {
    byte[] bytes = getResourceBytes("reservations.json");
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

}
