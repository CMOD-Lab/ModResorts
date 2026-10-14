package com.acme.modres.weather;

import java.io.IOException;

import javax.servlet.ServletException;

/**
 * WeatherService defines the contract for retrieving weather data for a given city.
 *
 * <p>Decomposed from the monolithic WeatherServlet (cz-java-0082) to support
 * independent deployment as an EKS microservice. Implementations may delegate
 * to an external weather microservice via HTTP (configured through the
 * {@code WEATHER_SERVICE_URL} environment variable) or fall back to local
 * default data when the external service is unavailable.
 *
 * <p>In a containerised EKS architecture each implementation of this interface
 * can be packaged as its own Kubernetes Deployment, exposed via a Kubernetes
 * Service, and configured through a dedicated ConfigMap — decoupling the
 * weather concern from the booking/presentation layer entirely.
 */
public interface WeatherService {

    /**
     * Returns weather data (JSON string) for the requested city.
     *
     * @param city       the city name as accepted by the weather back-end
     * @param weatherApiKey optional API key; may be {@code null} or empty when
     *                   the service falls back to cached/default data
     * @return JSON string containing weather information
     * @throws ServletException if a servlet-level error occurs
     * @throws IOException      if an I/O error occurs while fetching data
     */
    String getWeatherData(String city, String weatherApiKey) throws ServletException, IOException;
}
