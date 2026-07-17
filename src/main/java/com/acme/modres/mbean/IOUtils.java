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
 * IOUtils - Cloud-native version using Amazon S3 for durable storage.
 *
 * Replaces local temporary file creation (File.createTempFile) and
 * FileOutputStream writes with in-memory processing and S3 operations,
 * eliminating ephemeral local storage dependencies (cr-java-0112, cr-java-0062).
 */
public final class IOUtils {

  private static final Logger logger = Logger.getLogger(IOUtils.class.getName());

  // S3 bucket name read from environment variable for cloud-native configuration
  private static final String S3_BUCKET_NAME = System.getenv("S3_BUCKET_NAME") != null
      ? System.getenv("S3_BUCKET_NAME") : "modresorts-data";

  /**
   * Reads a classpath resource as a byte array.
   * Replaces File.createTempFile + FileOutputStream pattern with in-memory
   * processing to avoid ephemeral local temporary storage (cr-java-0112).
   */
  public static byte[] getResourceAsBytes(String path) {
    // Use try-with-resources for automatic stream management
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
   * Uploads content to Amazon S3 for durable storage.
   * Replaces local FileOutputStream writes (cr-java-0062) with S3 persistence.
   */
  public static void uploadToS3(String s3Key, byte[] content, String contentType) {
    // Use try-with-resources for S3Client (AutoCloseable) to prevent resource leaks
    try (S3Client s3Client = S3Client.builder().build()) {
      PutObjectRequest putRequest = PutObjectRequest.builder()
          .bucket(S3_BUCKET_NAME)
          .key(s3Key)
          .contentType(contentType)
          .build();
      s3Client.putObject(putRequest, RequestBody.fromBytes(content));
      logger.info("Uploaded to S3: s3://" + S3_BUCKET_NAME + "/" + s3Key);
    } catch (S3Exception e) {
      logger.severe("Failed to upload to S3: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Downloads content from Amazon S3.
   * Provides cloud-native read operations as alternative to local file reads.
   */
  public static byte[] downloadFromS3(String s3Key) {
    // Use try-with-resources for both S3Client and ResponseInputStream
    try (S3Client s3Client = S3Client.builder().build();
         ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(
             GetObjectRequest.builder()
                 .bucket(S3_BUCKET_NAME)
                 .key(s3Key)
                 .build());
         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = s3Object.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
      }
      return baos.toByteArray();
    } catch (S3Exception | IOException e) {
      logger.severe("Failed to download from S3: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  public static OpMetadataList getOpListFromConfig() {
    // Read directly from classpath using InputStream — no temp file needed (cr-java-0112)
    InputStream is = IOUtils.class.getClassLoader().getResourceAsStream("ops.json");
    if (is == null) {
      logger.warning("ops.json not found on classpath");
      return null;
    }
    try (JsonInputStream jsonIs = new JsonInputStream(is)) {
      OpMetadataList opList = new OpMetadataList();
      opList = (OpMetadataList) jsonIs.parseJsonAs(OpMetadataList.class);
      return opList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static ReservationList getReservationListFromConfig() {
    // Read directly from classpath using InputStream — no temp file needed (cr-java-0112)
    InputStream is = IOUtils.class.getClassLoader().getResourceAsStream("reservations.json");
    if (is == null) {
      logger.warning("reservations.json not found on classpath");
      return null;
    }
    try (JsonInputStream jsonIs = new JsonInputStream(is)) {
      ReservationList reservationList = new ReservationList();
      reservationList = (ReservationList) jsonIs.parseJsonAs(ReservationList.class);
      return reservationList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

}
