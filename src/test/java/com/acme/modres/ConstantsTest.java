package com.acme.modres;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Constants class.
 */
public class ConstantsTest {

    @Test
    void testCityConstants_notNull() {
        assertNotNull(Constants.BARCELONA);
        assertNotNull(Constants.CORK);
        assertNotNull(Constants.MIAMI);
        assertNotNull(Constants.SAN_FRANCISCO);
        assertNotNull(Constants.PARIS);
        assertNotNull(Constants.LAS_VEGAS);
    }

    @Test
    void testCityConstants_values() {
        assertEquals("Barcelona", Constants.BARCELONA);
        assertEquals("Cork", Constants.CORK);
        assertEquals("Miami", Constants.MIAMI);
        assertEquals("San_Francisco", Constants.SAN_FRANCISCO);
        assertEquals("Paris", Constants.PARIS);
        assertEquals("Las_Vegas", Constants.LAS_VEGAS);
    }

    @Test
    void testSupportedCities_notNull() {
        assertNotNull(Constants.SUPPORTED_CITIES);
    }

    @Test
    void testSupportedCities_length() {
        assertEquals(6, Constants.SUPPORTED_CITIES.length);
    }

    @Test
    void testSupportedCities_containsAllCities() {
        String[] cities = Constants.SUPPORTED_CITIES;
        boolean foundParis = false, foundLasVegas = false, foundSanFrancisco = false;
        boolean foundMiami = false, foundCork = false, foundBarcelona = false;
        for (String city : cities) {
            if (Constants.PARIS.equals(city)) foundParis = true;
            if (Constants.LAS_VEGAS.equals(city)) foundLasVegas = true;
            if (Constants.SAN_FRANCISCO.equals(city)) foundSanFrancisco = true;
            if (Constants.MIAMI.equals(city)) foundMiami = true;
            if (Constants.CORK.equals(city)) foundCork = true;
            if (Constants.BARCELONA.equals(city)) foundBarcelona = true;
        }
        assertTrue(foundParis);
        assertTrue(foundLasVegas);
        assertTrue(foundSanFrancisco);
        assertTrue(foundMiami);
        assertTrue(foundCork);
        assertTrue(foundBarcelona);
    }

    @Test
    void testWeatherFileConstants_notNull() {
        assertNotNull(Constants.BACELONA_WEATHER_FILE);
        assertNotNull(Constants.CORK_WEATHER_FILE);
        assertNotNull(Constants.LAS_VEGAS_WEATHER_FILE);
        assertNotNull(Constants.MIAMI_WEATHER_FILE);
        assertNotNull(Constants.PARIS_WEATHER_FILE);
        assertNotNull(Constants.SAN_FRANCESCO_WEATHER_FILE);
    }

    @Test
    void testWeatherFileConstants_values() {
        assertEquals("barcelona.json", Constants.BACELONA_WEATHER_FILE);
        assertEquals("cork.json", Constants.CORK_WEATHER_FILE);
        assertEquals("nv.json", Constants.LAS_VEGAS_WEATHER_FILE);
        assertEquals("miami.json", Constants.MIAMI_WEATHER_FILE);
        assertEquals("paris.json", Constants.PARIS_WEATHER_FILE);
        assertEquals("sanfran.json", Constants.SAN_FRANCESCO_WEATHER_FILE);
    }

    @Test
    void testWundergroundApiConstants_notNull() {
        assertNotNull(Constants.WUNDERGROUND_API_PREFIX);
        assertNotNull(Constants.WUNDERGROUND_API_PART);
    }

    @Test
    void testWundergroundApiPrefix_value() {
        assertEquals("http://api.wunderground.com/api/", Constants.WUNDERGROUND_API_PREFIX);
    }

    @Test
    void testWundergroundApiPart_value() {
        assertEquals("/forecast/geolookup/conditions/q/", Constants.WUNDERGROUND_API_PART);
    }

    @Test
    void testDataFormat_notNull() {
        assertNotNull(Constants.DATA_FORMAT);
    }

    @Test
    void testDataFormat_value() {
        assertEquals("MM/dd/yyyy", Constants.DATA_FORMAT);
    }
}
