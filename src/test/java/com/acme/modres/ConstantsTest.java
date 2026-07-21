package com.acme.modres;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConstantsTest {

    @Test
    void testCityConstants() {
        assertEquals("Barcelona", Constants.BARCELONA);
        assertEquals("Cork", Constants.CORK);
        assertEquals("Miami", Constants.MIAMI);
        assertEquals("San_Francisco", Constants.SAN_FRANCISCO);
        assertEquals("Paris", Constants.PARIS);
        assertEquals("Las_Vegas", Constants.LAS_VEGAS);
    }

    @Test
    void testSupportedCitiesArray() {
        assertNotNull(Constants.SUPPORTED_CITIES);
        assertEquals(6, Constants.SUPPORTED_CITIES.length);
    }

    @Test
    void testSupportedCitiesContainsAllCities() {
        String[] cities = Constants.SUPPORTED_CITIES;
        boolean foundParis = false, foundLasVegas = false, foundSanFrancisco = false;
        boolean foundMiami = false, foundCork = false, foundBarcelona = false;
        for (String city : cities) {
            if ("Paris".equals(city)) foundParis = true;
            if ("Las_Vegas".equals(city)) foundLasVegas = true;
            if ("San_Francisco".equals(city)) foundSanFrancisco = true;
            if ("Miami".equals(city)) foundMiami = true;
            if ("Cork".equals(city)) foundCork = true;
            if ("Barcelona".equals(city)) foundBarcelona = true;
        }
        assertTrue(foundParis);
        assertTrue(foundLasVegas);
        assertTrue(foundSanFrancisco);
        assertTrue(foundMiami);
        assertTrue(foundCork);
        assertTrue(foundBarcelona);
    }

    @Test
    void testWeatherFileConstants() {
        assertEquals("barcelona.json", Constants.BACELONA_WEATHER_FILE);
        assertEquals("cork.json", Constants.CORK_WEATHER_FILE);
        assertEquals("nv.json", Constants.LAS_VEGAS_WEATHER_FILE);
        assertEquals("miami.json", Constants.MIAMI_WEATHER_FILE);
        assertEquals("paris.json", Constants.PARIS_WEATHER_FILE);
        assertEquals("sanfran.json", Constants.SAN_FRANCESCO_WEATHER_FILE);
    }

    @Test
    void testApiConstants() {
        assertEquals("http://api.wunderground.com/api/", Constants.WUNDERGROUND_API_PREFIX);
        assertEquals("/forecast/geolookup/conditions/q/", Constants.WUNDERGROUND_API_PART);
    }

    @Test
    void testDataFormat() {
        assertEquals("MM/dd/yyyy", Constants.DATA_FORMAT);
    }
}
