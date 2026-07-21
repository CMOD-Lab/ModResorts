package com.acme.modres.mbean;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import com.acme.modres.mbean.reservation.ReservationList;
import com.acme.modres.util.JsonInputStream;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * IOUtils provides utility methods for reading configuration and reservation data.
 *
 * Blocker-3 (cr-java-0062) and Blocker-6 (cr-java-0112):
 * Replaced local temporary file creation (File.createTempFile) and FileOutputStream writes
 * with Amazon S3 object storage using AWS SDK for Java v2.
 * Data is read directly from S3 (or falls back to classpath resources) without
 * writing to the ephemeral local file system.
 */
public final class IOUtils {

  private static final Logger logger = Logger.getLogger(IOUtils.class.getName());

  // S3 bucket name read from environment variable for cloud-native configuration
  private static final String S3_BUCKET_NAME = System.getenv("S3_BUCKET_NAME") != null
      ? System.getenv("S3_BUCKET_NAME")
      : "modresorts-data";

  /**
   * Reads a resource file as a byte array.
   * First attempts to read from Amazon S3 (cloud-native, durable storage).
   * Falls back to classpath resource if S3 is unavailable (e.g., local dev).
   * Eliminates local temporary file creation (blocker-3, blocker-6).
   */
  public static byte[] readResourceBytes(String path) {
    // Attempt to read from S3 first (cloud-native durable storage)
    try (S3Client s3Client = S3Client.create();
         ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(
             GetObjectRequest.builder()
                 .bucket(S3_BUCKET_NAME)
                 .key(path)
                 .build())) {
      byte[] data = s3Object.readAllBytes();
      logger.info("Successfully read resource from S3: s3://" + S3_BUCKET_NAME + "/" + path);
      return data;
    } catch (NoSuchKeyException e) {
      logger.warning("Resource not found in S3, falling back to classpath: " + path);
    } catch (Exception e) {
      logger.warning("Could not read from S3, falling back to classpath resource: " + path + " - " + e.getMessage());
    }

    // Fallback: read from classpath resource (for local development / initial bootstrap)
    try (InputStream initialStream = IOUtils.class.getClassLoader().getResourceAsStream(path)) {
      if (initialStream == null) {
        logger.severe("Resource not found on classpath: " + path);
        return null;
      }
      return initialStream.readAllBytes();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static OpMetadataList getOpListFromConfig() {
    byte[] data = readResourceBytes("ops.json");
    if (data == null) {
      return null;
    }
    try (JsonInputStream is = new JsonInputStream(data)) {
      OpMetadataList opList = new OpMetadataList();
      opList = (OpMetadataList) is.parseJsonAs(OpMetadataList.class);
      return opList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static ReservationList getReservationListFromConfig() {
    byte[] data = readResourceBytes("reservations.json");
    if (data == null) {
      return null;
    }
    try (JsonInputStream is = new JsonInputStream(data)) {
      ReservationList reservationList = new ReservationList();
      reservationList = (ReservationList) is.parseJsonAs(ReservationList.class);
      return reservationList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

}
