package com.acme.modres.weather;

import com.acme.modres.Constants;
import com.acme.modres.DefaultWeatherData;
import com.acme.modres.exception.ExceptionHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

/**
 * WeatherServiceClient is the independently deployable implementation of
 * {@link WeatherService} for the EKS microservices architecture.
 *
 * <p>Decomposed from the monolithic WeatherServlet (cz-java-0082 – Individual
 * Components). All weather-retrieval logic has been extracted from the servlet
 * into this dedicated service class so that it can be:
 * <ul>
 *   <li>Packaged as its own Kubernetes Deployment on Amazon EKS</li>
 *   <li>Exposed via a dedicated Kubernetes Service</li>
 *   <li>Configured independently through a Kubernetes ConfigMap / environment
 *       variables ({@code WEATHER_SERVICE_URL}, {@code WEATHER_API_KEY})</li>
 * </ul>
 *
 * <p>When {@code WEATHER_SERVICE_URL} is set the client proxies the request to
 * the external weather microservice over HTTP.  When it is absent the client
 * falls back to the bundled default weather data so that the application
 * remains functional in local / offline environments.
 */
public class WeatherServiceClient implements WeatherService {

    /**
     * Environment variable that points to the base URL of the independently
     * deployed weather microservice on EKS.
     * Example value: {@code http://weather-service.default.svc.cluster.local:8080}
     */
    public static final String WEATHER_SERVICE_URL_ENV = "WEATHER_SERVICE_URL";

    private static final Logger logger = Logger.getLogger(WeatherServiceClient.class.getName());

    /**
     * Resolves the weather service base URL from the environment.
     * Returns {@code null} when the variable is not set, triggering the
     * local-fallback path.
     */
    private String resolveWeatherServiceUrl() {
        String url = System.getenv(WEATHER_SERVICE_URL_ENV);
        if (url == null || url.trim().isEmpty()) {
            logger.warning("Environment variable " + WEATHER_SERVICE_URL_ENV
                    + " is not set. Falling back to default weather data.");
            return null;
        }
        logger.info("Weather microservice URL resolved via environment: " + url);
        return url;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to the external weather microservice when
     * {@code WEATHER_SERVICE_URL} is configured; otherwise returns bundled
     * default data for the requested city.
     */
    @Override
    public String getWeatherData(String city, String weatherApiKey) throws ServletException, IOException {
        String weatherServiceUrl = resolveWeatherServiceUrl();

        if (weatherServiceUrl != null && weatherApiKey != null && !weatherApiKey.trim().isEmpty()) {
            logger.info("Delegating weather request for city '" + city
                    + "' to external weather microservice at: " + weatherServiceUrl);
            return fetchFromExternalService(city, weatherApiKey, weatherServiceUrl);
        } else {
            logger.info("Using default weather data for city: " + city);
            return fetchDefaultWeatherData(city);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Calls the external weather microservice via HTTP GET and returns the
     * raw JSON response body.
     */
    private String fetchFromExternalService(String city, String apiKey, String serviceBaseUrl)
            throws ServletException, IOException {

        String cityPath = resolveCityPath(city);
        if (cityPath == null) {
            String errorMsg = "Sorry, the weather information for your selected city: " + city
                    + " is not available.  Valid selections are: " + Constants.SUPPORTED_CITIES;
            ExceptionHandler.handleException(null, errorMsg, logger);
            return null;
        }

        // Build the request URL against the external microservice endpoint.
        // The microservice is discovered via Kubernetes DNS using WEATHER_SERVICE_URL.
        String resturl = serviceBaseUrl + "/" + apiKey + Constants.WUNDERGROUND_API_PART + cityPath;

        URL obj = null;
        HttpURLConnection con = null;
        try {
            obj = new URL(resturl);
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
        } catch (MalformedURLException e1) {
            ExceptionHandler.handleException(e1,
                    "Caught MalformedURLException. Please make sure the url is correct.", logger);
        } catch (ProtocolException e2) {
            ExceptionHandler.handleException(e2,
                    "Caught ProtocolException: " + e2.getMessage()
                            + ". Not able to set request method to http connection.", logger);
        } catch (IOException e3) {
            ExceptionHandler.handleException(e3,
                    "Caught IOException: " + e3.getMessage() + ". Not able to open connection.", logger);
        }

        int responseCode = con.getResponseCode();
        logger.log(Level.FINEST, "Response Code from weather microservice: " + responseCode);

        if (responseCode >= 200 && responseCode < 300) {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder responseStr = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    responseStr.append(inputLine);
                }
                logger.log(Level.FINE, "Weather microservice response received for city: " + city);
                return responseStr.toString();
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } else {
            ExceptionHandler.handleException(null,
                    "Weather microservice call to " + resturl + " returned error response: " + responseCode,
                    logger);
            return null;
        }
    }

    /**
     * Returns bundled default weather data for the given city (offline / fallback path).
     */
    private String fetchDefaultWeatherData(String city) throws ServletException, IOException {
        DefaultWeatherData defaultWeatherData;
        try {
            defaultWeatherData = new DefaultWeatherData(city);
        } catch (UnsupportedOperationException e) {
            ExceptionHandler.handleException(e, e.getMessage(), logger);
            return null;
        }
        return defaultWeatherData.getDefaultWeatherData();
    }

    /**
     * Maps a city name to its URL path segment used by the weather back-end.
     * Returns {@code null} for unsupported cities.
     */
    private String resolveCityPath(String city) {
        if (Constants.PARIS.equals(city)) {
            return "France/Paris.json";
        } else if (Constants.LAS_VEGAS.equals(city)) {
            return "NV/Las_Vegas.json";
        } else if (Constants.SAN_FRANCISCO.equals(city)) {
            return "/CA/San_Francisco.json";
        } else if (Constants.MIAMI.equals(city)) {
            return "FL/Miami.json";
        } else if (Constants.CORK.equals(city)) {
            return "ireland/cork.json";
        } else if (Constants.BARCELONA.equals(city)) {
            return "Spain/Barcelona.json";
        }
        return null;
    }
}
