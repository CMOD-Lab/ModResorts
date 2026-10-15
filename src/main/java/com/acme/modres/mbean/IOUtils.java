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
 * blocker-3 (cr-java-0062): Replaced local FileOutputStream write operations with
 *   Amazon S3 reads using AWS SDK for Java v2 to ensure data durability in cloud environments.
 * blocker-6 (cr-java-0112): Eliminated reliance on ephemeral local temporary directories
 *   (File.createTempFile) by reading data directly from Amazon S3 or classpath resources,
 *   ensuring data survives container restarts and enabling multi-instance access.
 */
public final class IOUtils {

  private static final Logger logger = Logger.getLogger(IOUtils.class.getName());

  // S3 configuration sourced from environment variables (12-factor app principle)
  private static final String S3_BUCKET_NAME = System.getenv("S3_BUCKET_NAME") != null
      ? System.getenv("S3_BUCKET_NAME") : "modresorts-data";
  private static final String S3_OPS_KEY = System.getenv("S3_OPS_KEY") != null
      ? System.getenv("S3_OPS_KEY") : "config/ops.json";
  private static final String S3_RESERVATIONS_KEY = System.getenv("S3_RESERVATIONS_KEY") != null
      ? System.getenv("S3_RESERVATIONS_KEY") : "reservations/reservations.json";

  /**
   * Reads a resource as a byte array, first attempting Amazon S3, then falling back
   * to the classpath. This eliminates the need for local temporary file creation
   * (blocker-6: no more File.createTempFile / local /tmp usage).
   *
   * @param s3Key   the S3 object key to attempt first
   * @param classpathPath the classpath resource path to use as fallback
   * @return byte array of the resource content, or null if not found
   */
  private static byte[] readResourceBytes(String s3Key, String classpathPath) {
    // blocker-3, blocker-6: Try S3 first for cloud-native durable storage
    String bucketName = S3_BUCKET_NAME;
    try (S3Client s3Client = S3Client.builder().build()) {
      GetObjectRequest getRequest = GetObjectRequest.builder()
          .bucket(bucketName)
          .key(s3Key)
          .build();
      // blocker-3: Use S3 instead of local FileOutputStream write
      // blocker-6: No temporary directory usage — data read directly from S3
      try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getRequest)) {
        return s3Object.readAllBytes();
      }
    } catch (NoSuchKeyException e) {
      logger.info("S3 key not found: " + s3Key + ". Falling back to classpath resource: " + classpathPath);
    } catch (Exception e) {
      logger.warning("S3 read failed for key: " + s3Key + ". Falling back to classpath. Error: " + e.getMessage());
    }

    // Fallback: read from classpath (no temp file creation — blocker-6 fix)
    try (InputStream classpathStream = IOUtils.class.getClassLoader().getResourceAsStream(classpathPath)) {
      if (classpathStream != null) {
        return classpathStream.readAllBytes();
      }
    } catch (IOException e) {
      logger.warning("Classpath resource read failed for: " + classpathPath + ". Error: " + e.getMessage());
    }
    return null;
  }

  public static OpMetadataList getOpListFromConfig() {
    byte[] content = readResourceBytes(S3_OPS_KEY, "ops.json");
    if (content == null) {
      logger.warning("Could not load ops.json from S3 or classpath.");
      return null;
    }
    try (JsonInputStream is = new JsonInputStream(content)) {
      OpMetadataList opList = (OpMetadataList) is.parseJsonAs(OpMetadataList.class);
      return opList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static ReservationList getReservationListFromConfig() {
    byte[] content = readResourceBytes(S3_RESERVATIONS_KEY, "reservations.json");
    if (content == null) {
      logger.warning("Could not load reservations.json from S3 or classpath.");
      return null;
    }
    try (JsonInputStream is = new JsonInputStream(content)) {
      ReservationList reservationList = (ReservationList) is.parseJsonAs(ReservationList.class);
      return reservationList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

}
