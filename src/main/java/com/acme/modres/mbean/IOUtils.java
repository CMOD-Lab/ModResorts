package com.acme.modres.mbean;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.acme.modres.mbean.reservation.ReservationList;
import com.google.gson.Gson;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Utility class for reading configuration resources.
 *
 * <p>Temporary file operations that previously relied on local disk storage
 * ({@code File.createTempFile} + {@code FileOutputStream}) have been replaced
 * with Amazon S3 operations (cr-java-0112 remediation). This ensures that
 * intermediate data survives container restarts and is accessible across
 * multiple application instances in cloud environments where local temporary
 * storage is ephemeral.</p>
 *
 * <p>The S3 bucket name is resolved from the environment variable
 * {@code S3_TEMP_BUCKET}. The AWS region is resolved from
 * {@code AWS_REGION} (defaults to {@code us-east-1} when not set).
 * Credentials are resolved via the standard AWS Default Credentials Provider
 * chain (IAM role, environment variables, ~/.aws/credentials, etc.).</p>
 */
public final class IOUtils {

    /**
     * Name of the S3 bucket used for temporary / intermediate data storage.
     * Resolved from the {@code S3_TEMP_BUCKET} environment variable so that
     * no bucket name is hard-coded in the application binary.
     */
    private static final String S3_TEMP_BUCKET =
            System.getenv("S3_TEMP_BUCKET") != null
                    ? System.getenv("S3_TEMP_BUCKET")
                    : "modresorts-temp-bucket";

    /**
     * AWS region resolved from the {@code AWS_REGION} environment variable.
     * Falls back to {@code us-east-1} when the variable is not set.
     */
    private static final String AWS_REGION =
            System.getenv("AWS_REGION") != null
                    ? System.getenv("AWS_REGION")
                    : "us-east-1";

    /**
     * Lazily-initialised, shared S3 client.  Using a single client instance
     * avoids the overhead of creating a new HTTP connection pool on every call.
     */
    private static volatile S3Client s3Client;

    /** Prevent instantiation of this utility class. */
    private IOUtils() {}

    /**
     * Returns the shared {@link S3Client}, creating it on first use.
     *
     * @return a configured {@link S3Client}
     */
    private static S3Client getS3Client() {
        if (s3Client == null) {
            synchronized (IOUtils.class) {
                if (s3Client == null) {
                    s3Client = S3Client.builder()
                            .region(Region.of(AWS_REGION))
                            .credentialsProvider(DefaultCredentialsProvider.create())
                            .build();
                }
            }
        }
        return s3Client;
    }

    /**
     * Reads the raw bytes of a classpath resource and stores them in Amazon S3
     * as a temporary object, replacing the previous {@code File.createTempFile}
     * pattern (cr-java-0112).
     *
     * <p>The resource bytes are first read into memory from the classpath, then
     * uploaded to S3 under the key {@code "temp/<path>"}. The same bytes are
     * returned to the caller so that existing consumers continue to work without
     * modification.</p>
     *
     * @param path classpath-relative resource path (e.g. {@code "reservations.json"})
     * @return the resource bytes, or an empty array if the resource cannot be found
     */
    public static byte[] getResourceBytes(String path) {
        try (InputStream initialStream = IOUtils.class.getClassLoader().getResourceAsStream(path)) {
            if (initialStream == null) {
                return new byte[0];
            }

            // Read resource bytes into memory
            byte[] buffer = new byte[initialStream.available()];
            initialStream.read(buffer);

            // Persist to Amazon S3 instead of a local temporary file (cr-java-0112)
            String s3Key = "temp/" + path;
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(S3_TEMP_BUCKET)
                    .key(s3Key)
                    .build();
            getS3Client().putObject(putRequest, RequestBody.fromBytes(buffer));

            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * Retrieves a temporary object previously stored in Amazon S3 by
     * {@link #getResourceBytes(String)}.
     *
     * <p>This method replaces any pattern that previously read back data from a
     * local temporary file. If the object does not exist in S3 the method falls
     * back to reading the resource directly from the classpath.</p>
     *
     * @param path classpath-relative resource path / S3 key suffix
     * @return an {@link InputStream} over the object content, or {@code null}
     *         if neither S3 nor the classpath contains the resource
     */
    public static InputStream getTempResourceFromS3(String path) {
        String s3Key = "temp/" + path;
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(S3_TEMP_BUCKET)
                    .key(s3Key)
                    .build();
            ResponseInputStream<GetObjectResponse> s3Object =
                    getS3Client().getObject(getRequest);
            return s3Object;
        } catch (NoSuchKeyException e) {
            // Fall back to classpath if the object has not been uploaded yet
            return IOUtils.class.getClassLoader().getResourceAsStream(path);
        }
    }

    /**
     * Opens a classpath resource as an {@link InputStream}.
     *
     * @param path classpath-relative resource path
     * @return the {@link InputStream}, or {@code null} if not found
     */
    public static InputStream getResourceAsStream(String path) {
        return IOUtils.class.getClassLoader().getResourceAsStream(path);
    }

    /**
     * Loads the operations metadata list from configuration.
     *
     * <p>Reads directly from the classpath {@link InputStream} — no local
     * temporary file is created (cr-java-0112).</p>
     *
     * @return the parsed {@link OpMetadataList}, or {@code null} on error
     */
    public static OpMetadataList getOpListFromConfig() {
        try (InputStream is = getResourceAsStream("ops.json")) {
            if (is == null) {
                return null;
            }
            Gson gson = new Gson();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            return gson.fromJson(reader, OpMetadataList.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads the reservation list from configuration.
     *
     * <p>Reads directly from the classpath {@link InputStream} — no local
     * temporary file is created (cr-java-0112).</p>
     *
     * @return the parsed {@link ReservationList}, or {@code null} on error
     */
    public static ReservationList getReservationListFromConfig() {
        try (InputStream is = getResourceAsStream("reservations.json")) {
            if (is == null) {
                return null;
            }
            Gson gson = new Gson();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            return gson.fromJson(reader, ReservationList.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
