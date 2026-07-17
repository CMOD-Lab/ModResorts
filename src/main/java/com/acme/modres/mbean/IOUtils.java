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
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Utility class for I/O operations.
 * Replaces local temporary file storage with Amazon S3 for durable,
 * cloud-native storage that survives container restarts.
 */
public final class IOUtils {

  private static final Logger logger = Logger.getLogger(IOUtils.class.getName());

  // S3 bucket name read from environment variable for cloud-native configuration
  private static final String S3_BUCKET_NAME = System.getenv("S3_BUCKET_NAME") != null
      ? System.getenv("S3_BUCKET_NAME")
      : "modresorts-data";

  /**
   * Reads a classpath resource and returns its bytes.
   * Replaces File.createTempFile() usage — no local temporary storage needed.
   */
  public static byte[] getResourceBytes(String path) {
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
   * Writes data to Amazon S3 instead of the local file system.
   * Eliminates reliance on ephemeral local temporary directories.
   */
  public static void writeToS3(String s3Key, byte[] data) {
    try (S3Client s3 = S3Client.create()) {
      PutObjectRequest putRequest = PutObjectRequest.builder()
          .bucket(S3_BUCKET_NAME)
          .key(s3Key)
          .build();
      s3.putObject(putRequest, RequestBody.fromBytes(data));
      logger.info("Data written to S3: s3://" + S3_BUCKET_NAME + "/" + s3Key);
    } catch (Exception e) {
      logger.severe("Failed to write to S3 key " + s3Key + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Reads data from Amazon S3.
   * Falls back to classpath resource if S3 key is not found.
   */
  public static InputStream readFromS3OrClasspath(String s3Key, String classpathFallback) {
    try {
      S3Client s3 = S3Client.create();
      GetObjectRequest getRequest = GetObjectRequest.builder()
          .bucket(S3_BUCKET_NAME)
          .key(s3Key)
          .build();
      ResponseInputStream<GetObjectResponse> s3Stream = s3.getObject(getRequest);
      logger.info("Reading from S3: s3://" + S3_BUCKET_NAME + "/" + s3Key);
      return s3Stream;
    } catch (NoSuchKeyException e) {
      logger.info("S3 key not found, falling back to classpath: " + classpathFallback);
      return IOUtils.class.getClassLoader().getResourceAsStream(classpathFallback);
    } catch (Exception e) {
      logger.warning("S3 read failed, falling back to classpath: " + e.getMessage());
      return IOUtils.class.getClassLoader().getResourceAsStream(classpathFallback);
    }
  }

  public static OpMetadataList getOpListFromConfig() {
    // Try S3 first, fall back to classpath resource — no local temp files
    try (InputStream is = readFromS3OrClasspath("config/ops.json", "ops.json");
         JsonInputStream jsonIs = new JsonInputStream(is)) {
      if (is == null) {
        logger.warning("ops.json not found in S3 or classpath");
        return new OpMetadataList();
      }
      OpMetadataList opList = (OpMetadataList) jsonIs.parseJsonAs(OpMetadataList.class);
      return opList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static ReservationList getReservationListFromConfig() {
    // Try S3 first, fall back to classpath resource — no local temp files
    try (InputStream is = readFromS3OrClasspath("config/reservations.json", "reservations.json");
         JsonInputStream jsonIs = new JsonInputStream(is)) {
      if (is == null) {
        logger.warning("reservations.json not found in S3 or classpath");
        return new ReservationList();
      }
      ReservationList reservationList = (ReservationList) jsonIs.parseJsonAs(ReservationList.class);
      return reservationList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

}
