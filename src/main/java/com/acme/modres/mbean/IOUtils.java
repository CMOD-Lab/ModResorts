package com.acme.modres.mbean;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.logging.Logger;

import com.acme.modres.mbean.reservation.ReservationList;
import com.google.gson.Gson;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Utility class for reading application configuration data.
 *
 * <p>All local file-system write operations (previously using {@link java.io.FileOutputStream}
 * to write classpath resources to temporary files) have been replaced with Amazon S3 reads.
 * When the {@code S3_BUCKET_NAME} environment variable is set, configuration objects are
 * fetched directly from S3. When it is not set (e.g., local development), the implementation
 * falls back to reading the resource directly from the classpath without writing any local
 * temporary files, eliminating the ephemeral-storage dependency entirely.
 */
public final class IOUtils {

  private static final Logger logger = Logger.getLogger(IOUtils.class.getName());

  /** S3 bucket name resolved from the environment; empty string disables S3 and uses classpath fallback. */
  private static final String S3_BUCKET_NAME =
      System.getenv("S3_BUCKET_NAME") != null ? System.getenv("S3_BUCKET_NAME") : "";

  /** AWS region resolved from the environment; defaults to us-east-1. */
  private static final String AWS_REGION =
      System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION") : "us-east-1";

  private IOUtils() {
    // utility class – no instantiation
  }

  /**
   * Reads the named resource as an {@link InputStream}.
   *
   * <p>When {@code S3_BUCKET_NAME} is configured the object is fetched from Amazon S3.
   * Otherwise the resource is loaded from the application classpath. In both cases the
   * caller receives a plain {@link InputStream} and <strong>no data is ever written to
   * the local file system</strong>, making this method safe for containerised and
   * serverless cloud environments where the local file system is ephemeral.
   *
   * @param path the resource name / S3 object key (e.g. {@code "reservations.json"})
   * @return an open {@link InputStream} for the resource, or {@code null} on failure
   */
  public static InputStream getResourceAsStream(String path) {
    if (S3_BUCKET_NAME != null && !S3_BUCKET_NAME.isEmpty()) {
      try {
        S3Client s3Client = S3Client.builder()
            .region(Region.of(AWS_REGION))
            .build();

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(S3_BUCKET_NAME)
            .key(path)
            .build();

        ResponseInputStream<GetObjectResponse> s3Stream = s3Client.getObject(getObjectRequest);
        logger.info("Loaded resource '" + path + "' from S3 bucket '" + S3_BUCKET_NAME + "'");
        return s3Stream;
      } catch (NoSuchKeyException e) {
        logger.warning("Resource '" + path + "' not found in S3 bucket '" + S3_BUCKET_NAME
            + "'; falling back to classpath. " + e.getMessage());
      } catch (Exception e) {
        logger.warning("Failed to load resource '" + path + "' from S3; falling back to classpath. "
            + e.getMessage());
      }
    }

    // Classpath fallback – no local file writes, no ephemeral storage dependency
    InputStream classpathStream = IOUtils.class.getClassLoader().getResourceAsStream(path);
    if (classpathStream == null) {
      logger.severe("Resource '" + path + "' not found on classpath.");
    }
    return classpathStream;
  }

  /**
   * Parses a JSON resource into the given class using the stream returned by
   * {@link #getResourceAsStream(String)}.
   *
   * <p>Replaces the previous pattern of writing the classpath resource to a temporary
   * {@link java.io.File} via {@link java.io.FileOutputStream} and then reading it back
   * through {@code JsonInputStream}. No local file writes are performed.
   *
   * @param path the resource name / S3 object key
   * @param cls  the target class for JSON deserialisation
   * @return the deserialised object, or {@code null} on failure
   */
  private static <T> T parseJsonResource(String path, Class<T> cls) {
    try (InputStream is = getResourceAsStream(path)) {
      if (is == null) {
        return null;
      }
      Gson gson = new Gson();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      return gson.fromJson(reader, cls);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static OpMetadataList getOpListFromConfig() {
    OpMetadataList opList = parseJsonResource("ops.json", OpMetadataList.class);
    return opList != null ? opList : new OpMetadataList();
  }

  public static ReservationList getReservationListFromConfig() {
    ReservationList reservationList = parseJsonResource("reservations.json", ReservationList.class);
    return reservationList != null ? reservationList : new ReservationList();
  }

}
