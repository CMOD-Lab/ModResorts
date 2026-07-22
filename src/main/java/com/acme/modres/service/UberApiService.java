package com.acme.modres.service;

import com.acme.modres.Constants;
import com.acme.modres.dto.UberRideRequest;
import com.acme.modres.dto.UberRideResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class UberApiService {

  private static final Logger logger = Logger.getLogger(UberApiService.class.getName());
  private static final String UBER_CLIENT_ID_SECRET_NAME_ENV = "UBER_CLIENT_ID_SECRET_NAME";
  private static final String UBER_CLIENT_SECRET_SECRET_NAME_ENV = "UBER_CLIENT_SECRET_SECRET_NAME";
  private static final int CONNECTION_TIMEOUT = 10000;
  private static final int READ_TIMEOUT = 15000;
  private static final int MAX_RETRIES = 3;

  private final ConcurrentHashMap<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();
  private final Gson gson = new Gson();

  private static class TokenInfo {
    String accessToken;
    long expiryTime;

    TokenInfo(String accessToken, long expiresIn) {
      this.accessToken = accessToken;
      this.expiryTime = System.currentTimeMillis() + (expiresIn * 1000) - 60000;
    }

    boolean isExpired() {
      return System.currentTimeMillis() >= expiryTime;
    }
  }

  public UberRideResponse requestRide(double pickupLat, double pickupLng, double dropoffLat,
                                       double dropoffLng, String productId, Long scheduledTime)
      throws IOException {
    String accessToken = getAccessToken();

    UberRideRequest request = new UberRideRequest();
    request.setStartLatitude(pickupLat);
    request.setStartLongitude(pickupLng);
    request.setEndLatitude(dropoffLat);
    request.setEndLongitude(dropoffLng);
    request.setProductId(productId != null ? productId : Constants.DEFAULT_UBER_PRODUCT);
    request.setScheduledTime(scheduledTime);

    String requestBody = gson.toJson(request);
    logger.log(Level.FINE, "Requesting Uber ride with payload: " + requestBody);

    String urlString = Constants.UBER_API_BASE_URL + "/v1.2/requests";

    return executeWithRetry(() -> {
      HttpURLConnection conn = createConnection(urlString, "POST", accessToken);

      try (OutputStream os = conn.getOutputStream()) {
        byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }

      int responseCode = conn.getResponseCode();
      logger.log(Level.FINE, "Uber API response code: " + responseCode);

      if (responseCode == 401) {
        tokenCache.clear();
        throw new IOException("Authentication failed - token may be expired");
      }

      if (responseCode == 429) {
        throw new RateLimitException("Rate limit exceeded");
      }

      if (responseCode >= 200 && responseCode < 300) {
        String response = readResponse(conn);
        logger.log(Level.FINE, "Uber API response: " + sanitizeLog(response));
        return parseRideResponse(response);
      } else {
        String errorResponse = readErrorResponse(conn);
        throw new IOException("Uber API error " + responseCode + ": " + errorResponse);
      }
    });
  }

  public UberRideResponse getRideStatus(String rideId) throws IOException {
    String accessToken = getAccessToken();
    String urlString = Constants.UBER_API_BASE_URL + "/v1.2/requests/" + rideId;

    return executeWithRetry(() -> {
      HttpURLConnection conn = createConnection(urlString, "GET", accessToken);
      int responseCode = conn.getResponseCode();

      if (responseCode == 401) {
        tokenCache.clear();
        throw new IOException("Authentication failed - token may be expired");
      }

      if (responseCode >= 200 && responseCode < 300) {
        String response = readResponse(conn);
        return parseRideResponse(response);
      } else if (responseCode == 404) {
        throw new IOException("Ride not found");
      } else {
        String errorResponse = readErrorResponse(conn);
        throw new IOException("Uber API error " + responseCode + ": " + errorResponse);
      }
    });
  }

  public boolean cancelRide(String rideId) throws IOException {
    String accessToken = getAccessToken();
    String urlString = Constants.UBER_API_BASE_URL + "/v1.2/requests/" + rideId;

    return executeWithRetry(() -> {
      HttpURLConnection conn = createConnection(urlString, "DELETE", accessToken);
      int responseCode = conn.getResponseCode();

      if (responseCode == 401) {
        tokenCache.clear();
        throw new IOException("Authentication failed - token may be expired");
      }

      if (responseCode >= 200 && responseCode < 300 || responseCode == 204) {
        logger.log(Level.INFO, "Successfully cancelled ride: " + rideId);
        return true;
      } else if (responseCode == 404) {
        throw new IOException("Ride not found");
      } else if (responseCode == 409) {
        throw new IOException("Ride cannot be cancelled - already in progress or completed");
      } else {
        String errorResponse = readErrorResponse(conn);
        throw new IOException("Uber API error " + responseCode + ": " + errorResponse);
      }
    });
  }

  private String getAccessToken() throws IOException {
    TokenInfo cachedToken = tokenCache.get("uber_token");
    if (cachedToken != null && !cachedToken.isExpired()) {
      return cachedToken.accessToken;
    }

    String clientId = resolveSecret(UBER_CLIENT_ID_SECRET_NAME_ENV);
    String clientSecret = resolveSecret(UBER_CLIENT_SECRET_SECRET_NAME_ENV);

    if (clientId == null || clientSecret == null) {
      throw new IOException("Uber API credentials not found in AWS Secrets Manager");
    }

    String requestBody = "grant_type=client_credentials&client_id=" + clientId +
                         "&client_secret=" + clientSecret;

    HttpURLConnection conn = null;
    try {
      URL url = new URL(Constants.UBER_AUTH_URL);
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      conn.setDoOutput(true);
      conn.setConnectTimeout(CONNECTION_TIMEOUT);
      conn.setReadTimeout(READ_TIMEOUT);

      try (OutputStream os = conn.getOutputStream()) {
        byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }

      int responseCode = conn.getResponseCode();
      if (responseCode >= 200 && responseCode < 300) {
        String response = readResponse(conn);
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        String accessToken = jsonResponse.get("access_token").getAsString();
        long expiresIn = jsonResponse.get("expires_in").getAsLong();

        tokenCache.put("uber_token", new TokenInfo(accessToken, expiresIn));
        logger.log(Level.INFO, "Successfully obtained Uber access token");
        return accessToken;
      } else {
        String errorResponse = readErrorResponse(conn);
        throw new IOException("Failed to obtain Uber access token: " + errorResponse);
      }
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  private String resolveSecret(String secretNameEnv) {
    String secretName = System.getenv(secretNameEnv);
    if (secretName != null && !secretName.trim().isEmpty()) {
      try {
        String awsRegion = System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION") : "us-east-1";
        try (SecretsManagerClient secretsClient = SecretsManagerClient.builder()
            .region(Region.of(awsRegion))
            .build()) {
          GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
              .secretId(secretName)
              .build();
          GetSecretValueResponse secretValueResponse = secretsClient.getSecretValue(getSecretValueRequest);
          return secretValueResponse.secretString();
        }
      } catch (SecretsManagerException e) {
        logger.warning("Failed to retrieve secret " + secretName + ": " + e.awsErrorDetails().errorMessage());
      }
    }
    return null;
  }

  private HttpURLConnection createConnection(String urlString, String method, String accessToken)
      throws IOException {
    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod(method);
    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setConnectTimeout(CONNECTION_TIMEOUT);
    conn.setReadTimeout(READ_TIMEOUT);

    if ("POST".equals(method) || "PUT".equals(method)) {
      conn.setDoOutput(true);
    }

    return conn;
  }

  private String readResponse(HttpURLConnection conn) throws IOException {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
      StringBuilder response = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        response.append(line);
      }
      return response.toString();
    }
  }

  private String readErrorResponse(HttpURLConnection conn) {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
      StringBuilder response = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        response.append(line);
      }
      return response.toString();
    } catch (Exception e) {
      return "Unknown error";
    }
  }

  private UberRideResponse parseRideResponse(String response) {
    JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
    UberRideResponse rideResponse = new UberRideResponse();

    if (jsonResponse.has("request_id")) {
      rideResponse.setRequestId(jsonResponse.get("request_id").getAsString());
    }
    if (jsonResponse.has("status")) {
      rideResponse.setStatus(jsonResponse.get("status").getAsString());
    }
    if (jsonResponse.has("eta")) {
      rideResponse.setEta(jsonResponse.get("eta").getAsString());
    }
    if (jsonResponse.has("driver") && !jsonResponse.get("driver").isJsonNull()) {
      JsonObject driver = jsonResponse.getAsJsonObject("driver");
      if (driver.has("name")) {
        rideResponse.setDriverName(driver.get("name").getAsString());
      }
    }
    if (jsonResponse.has("vehicle") && !jsonResponse.get("vehicle").isJsonNull()) {
      JsonObject vehicle = jsonResponse.getAsJsonObject("vehicle");
      rideResponse.setVehicleDetails(gson.toJson(vehicle));
    }
    if (jsonResponse.has("product_id")) {
      rideResponse.setProductId(jsonResponse.get("product_id").getAsString());
    }

    return rideResponse;
  }

  private <T> T executeWithRetry(RetryableOperation<T> operation) throws IOException {
    int attempt = 0;
    while (attempt < MAX_RETRIES) {
      try {
        return operation.execute();
      } catch (RateLimitException e) {
        attempt++;
        if (attempt >= MAX_RETRIES) {
          throw new IOException("Max retries exceeded due to rate limiting");
        }
        long waitTime = (long) Math.pow(2, attempt) * 1000;
        logger.log(Level.WARNING, "Rate limited, retrying in " + waitTime + "ms (attempt " + attempt + ")");
        try {
          Thread.sleep(waitTime);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new IOException("Retry interrupted", ie);
        }
      }
    }
    throw new IOException("Operation failed after retries");
  }

  private String sanitizeLog(String log) {
    if (log == null) return null;
    return log.replaceAll("\"access_token\"\\s*:\\s*\"[^\"]+\"", "\"access_token\":\"***\"")
              .replaceAll("\"client_secret\"\\s*:\\s*\"[^\"]+\"", "\"client_secret\":\"***\"");
  }

  @FunctionalInterface
  private interface RetryableOperation<T> {
    T execute() throws IOException;
  }

  private static class RateLimitException extends IOException {
    public RateLimitException(String message) {
      super(message);
    }
  }
}
