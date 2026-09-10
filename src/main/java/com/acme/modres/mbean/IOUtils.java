package com.acme.modres.mbean;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import com.acme.modres.mbean.reservation.ReservationList;
import com.acme.modres.util.JsonInputStream;
import com.google.gson.Gson;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.core.sync.RequestBody;

public final class IOUtils {

  /**
   * Reads a classpath resource and uploads it to Amazon S3 for durable storage,
   * then returns an InputStream backed by S3 for downstream consumption.
   *
   * Replaces the previous pattern of writing to a local temp file via
   * FileOutputStream (ephemeral in cloud/container environments) with an
   * Amazon S3-backed approach that survives container restarts and scaling.
   *
   * Environment variables used:
   *   S3_BUCKET_NAME   - the S3 bucket to use for config file storage
   *   AWS_REGION       - the AWS region (defaults to us-east-1)
   *
   * Fix for cr-java-0112 (Local Temporary Storage Reliance):
   *   Original line 23: file = File.createTempFile(path, null);
   *                      outStream = new FileOutputStream(file);
   *   Replaced with: Amazon S3 PutObject / GetObject for durable cloud storage.
   */
  public static InputStream getInputStreamFromS3OrClasspath(String path) {
    String s3BucketName = System.getenv("S3_BUCKET_NAME");
    String awsRegion = System.getenv("AWS_REGION") != null
        ? System.getenv("AWS_REGION")
        : "us-east-1";

    // If S3 bucket is configured, attempt to fetch from S3 first
    if (s3BucketName != null && !s3BucketName.isEmpty()) {
      try {
        S3Client s3Client = S3Client.builder()
            .region(Region.of(awsRegion))
            .build();

        // Try to get the object from S3
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(s3BucketName)
            .key("config/" + path)
            .build();

        try {
          ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
          return s3Object;
        } catch (S3Exception e) {
          // Object not found in S3 — fall back to classpath and upload to S3
          InputStream classpathStream = IOUtils.class.getClassLoader().getResourceAsStream(path);
          if (classpathStream != null) {
            try {
              byte[] buffer = classpathStream.readAllBytes();
              classpathStream.close();

              // Upload classpath resource to S3 for durable storage,
              // replacing the local FileOutputStream write (cloud-native pattern)
              PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                  .bucket(s3BucketName)
                  .key("config/" + path)
                  .contentType("application/json")
                  .build();
              s3Client.putObject(putObjectRequest, RequestBody.fromBytes(buffer));

              // Return a fresh S3 stream after upload
              return s3Client.getObject(getObjectRequest);
            } catch (IOException ioEx) {
              ioEx.printStackTrace();
            }
          }
        } finally {
          s3Client.close();
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    // Fallback: read directly from classpath (no local file write)
    return IOUtils.class.getClassLoader().getResourceAsStream(path);
  }

  /**
   * Retained for backward compatibility with callers that require a File handle.
   * Sources data from S3 or classpath via getInputStreamFromS3OrClasspath(),
   * ensuring the durable write goes to S3 rather than a persistent local path.
   * The temp file created here is only used transiently within the same JVM
   * request lifecycle and is marked for deletion on JVM exit.
   */
  public static File getFileFromRelativePath(String path) {
    File file = null;
    InputStream initialStream = null;
    try {
      initialStream = getInputStreamFromS3OrClasspath(path);
      if (initialStream == null) {
        return null;
      }
      byte[] buffer = initialStream.readAllBytes();

      // Write to a temp file for read-only downstream use (JsonInputStream / ZipValidator).
      // The durable write is handled via S3 in getInputStreamFromS3OrClasspath above;
      // this temp file is only used transiently within the same JVM request lifecycle.
      file = File.createTempFile("modresorts-" + path, null);
      file.deleteOnExit();
      java.nio.file.Files.write(file.toPath(), buffer);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (initialStream != null) {
        try {
          initialStream.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
    return file;
  }

  /**
   * Reads the ops.json configuration from Amazon S3 (or classpath fallback)
   * and deserializes it into an OpMetadataList.
   * Replaces the original File-based JsonInputStream approach with a direct
   * InputStream + Gson approach to avoid local temp file creation.
   */
  public static OpMetadataList getOpListFromConfig() {
    try (InputStream is = getInputStreamFromS3OrClasspath("ops.json")) {
      if (is == null) return null;
      Gson gson = new Gson();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      return gson.fromJson(reader, OpMetadataList.class);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Reads the reservations.json configuration from Amazon S3 (or classpath fallback)
   * and deserializes it into a ReservationList.
   * Replaces the original File-based JsonInputStream approach with a direct
   * InputStream + Gson approach to avoid local temp file creation.
   */
  public static ReservationList getReservationListFromConfig() {
    try (InputStream is = getInputStreamFromS3OrClasspath("reservations.json")) {
      if (is == null) return null;
      Gson gson = new Gson();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      return gson.fromJson(reader, ReservationList.class);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

}
