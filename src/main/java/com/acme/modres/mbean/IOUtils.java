package com.acme.modres.mbean;

import java.io.IOException;
import java.io.InputStream;

import com.acme.modres.mbean.reservation.ReservationList;
import com.acme.modres.util.JsonInputStream;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * IOUtils provides utility methods for reading configuration and resource files.
 *
 * Cloud-native changes:
 * - Removed local temporary file creation (File.createTempFile) to eliminate
 *   reliance on ephemeral local temporary directories (cr-java-0112).
 * - Removed FileOutputStream writes to local disk (cr-java-0062).
 * - Resources are now read directly from the classpath InputStream without
 *   materializing them as local files.
 * - Added S3 upload/download helpers for durable cloud storage (cr-java-0062).
 */
public final class IOUtils {

  // S3 bucket name read from environment variable for cloud-native configuration
  private static final String S3_BUCKET_NAME = System.getenv("S3_BUCKET_NAME") != null
      ? System.getenv("S3_BUCKET_NAME")
      : "modresorts-data";

  /**
   * Reads a classpath resource and returns its bytes.
   * Replaces the previous pattern that wrote to a local temp file.
   */
  public static byte[] getResourceBytes(String path) {
    try (InputStream initialStream = IOUtils.class.getClassLoader().getResourceAsStream(path)) {
      if (initialStream == null) {
        return null;
      }
      byte[] buffer = new byte[initialStream.available()];
      initialStream.read(buffer);
      return buffer;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Returns an InputStream for a classpath resource.
   * Replaces the previous pattern that created a local temp file and returned
   * a java.io.File reference (cr-java-0063, cr-java-0062, cr-java-0112).
   */
  public static InputStream getInputStreamFromRelativePath(String path) {
    return IOUtils.class.getClassLoader().getResourceAsStream(path);
  }

  public static OpMetadataList getOpListFromConfig() {
    // Read directly from classpath InputStream — no local temp file needed
    try (InputStream is = getInputStreamFromRelativePath("ops.json");
         JsonInputStream jsonIs = new JsonInputStream(is)) {
      OpMetadataList opList = new OpMetadataList();
      opList = (OpMetadataList) jsonIs.parseJsonAs(OpMetadataList.class);
      return opList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static ReservationList getReservationListFromConfig() {
    // Read directly from classpath InputStream — no local temp file needed
    try (InputStream is = getInputStreamFromRelativePath("reservations.json");
         JsonInputStream jsonIs = new JsonInputStream(is)) {
      ReservationList reservationList = new ReservationList();
      reservationList = (ReservationList) jsonIs.parseJsonAs(ReservationList.class);
      return reservationList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Uploads data to Amazon S3 for durable cloud storage.
   * Replaces local file write operations (cr-java-0062).
   */
  public static void writeToS3(String key, byte[] content) {
    try (S3Client s3 = S3Client.builder().build()) {
      PutObjectRequest putRequest = PutObjectRequest.builder()
          .bucket(S3_BUCKET_NAME)
          .key(key)
          .build();
      s3.putObject(putRequest, RequestBody.fromBytes(content));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Downloads data from Amazon S3.
   */
  public static byte[] readFromS3(String key) {
    try (S3Client s3 = S3Client.builder().build()) {
      GetObjectRequest getRequest = GetObjectRequest.builder()
          .bucket(S3_BUCKET_NAME)
          .key(key)
          .build();
      try (ResponseInputStream<GetObjectResponse> s3Object = s3.getObject(getRequest)) {
        return s3Object.readAllBytes();
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

}
