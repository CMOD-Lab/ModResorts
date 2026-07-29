package com.acme.modres.mbean;

import java.io.IOException;
import java.io.InputStream;

import com.acme.modres.mbean.reservation.ReservationList;
import com.acme.modres.util.JsonInputStream;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Utility class for I/O operations.
 *
 * Cloud-native migration: All local temporary file write operations have been
 * replaced with Amazon S3 object storage to ensure data durability and
 * availability across container restarts and multi-instance deployments.
 * Temporary local file creation (File.createTempFile) has been eliminated;
 * resources are now streamed directly from S3 or the classpath.
 *
 * Fixes:
 *   - blocker-3 (cr-java-0062): Local File System Write Operations — FileOutputStream
 *     to temp file replaced with S3 streaming.
 *   - blocker-6 (cr-java-0112): Local Temporary Storage Reliance — File.createTempFile
 *     usage eliminated; data is read directly from S3 or classpath InputStream.
 */
public final class IOUtils {

  // S3 configuration sourced from environment variables (12-factor app principle)
  private static final String S3_BUCKET_NAME = System.getenv("S3_BUCKET_NAME") != null
      ? System.getenv("S3_BUCKET_NAME") : "modresorts-data";

  /**
   * Reads a resource by key, first attempting Amazon S3, then falling back to
   * the classpath. This eliminates reliance on ephemeral local temporary
   * directories and local file write operations.
   *
   * @param resourceKey the S3 object key / classpath resource name
   * @return an InputStream for the resource, or null if not found
   */
  public static InputStream getResourceAsStream(String resourceKey) {
    String bucketName = S3_BUCKET_NAME;
    try {
      S3Client s3 = S3Client.create();
      ResponseInputStream<GetObjectResponse> s3Stream = s3.getObject(
          GetObjectRequest.builder().bucket(bucketName).key(resourceKey).build());
      return s3Stream;
    } catch (NoSuchKeyException e) {
      // Key not found in S3 — fall back to classpath
    } catch (Exception e) {
      // S3 unavailable or misconfigured — fall back to classpath
    }

    // Fallback: load from classpath (for local development / testing)
    return IOUtils.class.getClassLoader().getResourceAsStream(resourceKey);
  }

  public static OpMetadataList getOpListFromConfig() {
    try (InputStream is = getResourceAsStream("ops.json");
         JsonInputStream jsonIs = new JsonInputStream(is)) {
      OpMetadataList opList = (OpMetadataList) jsonIs.parseJsonAs(OpMetadataList.class);
      return opList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static ReservationList getReservationListFromConfig() {
    try (InputStream is = getResourceAsStream("reservations.json");
         JsonInputStream jsonIs = new JsonInputStream(is)) {
      ReservationList reservationList = (ReservationList) jsonIs.parseJsonAs(ReservationList.class);
      return reservationList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

}
