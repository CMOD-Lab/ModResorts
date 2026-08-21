package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DefaultWeatherData class.
 */
public class DefaultWeatherDataTest {

    @Test
    void testConstructor_withNullCity_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () ->
            new DefaultWeatherData(null)
        );
    }

    @Test
    void testConstructor_withUnsupportedCity_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () ->
            new DefaultWeatherData("Tokyo")
        );
    }

    @Test
    void testConstructor_withUnsupportedCity_exceptionMessage() {
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () ->
            new DefaultWeatherData("Tokyo")
        );
        assertTrue(ex.getMessage().contains("City is invalid"));
    }

    @Test
    void testConstructor_withNullCity_exceptionMessage() {
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () ->
            new DefaultWeatherData(null)
        );
        assertEquals("City is not defined", ex.getMessage());
    }

    @Test
    void testConstructor_withEmptyCity_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () ->
            new DefaultWeatherData("")
        );
    }

    @Test
    void testGetCity_returnsCorrectCity() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.PARIS);
        assertEquals(Constants.PARIS, data.getCity());
    }

    @Test
    void testGetCity_forLasVegas() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.LAS_VEGAS);
        assertEquals(Constants.LAS_VEGAS, data.getCity());
    }

    @Test
    void testGetCity_forSanFrancisco() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.SAN_FRANCISCO);
        assertEquals(Constants.SAN_FRANCISCO, data.getCity());
    }

    @Test
    void testGetCity_forMiami() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.MIAMI);
        assertEquals(Constants.MIAMI, data.getCity());
    }

    @Test
    void testGetCity_forCork() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.CORK);
        assertEquals(Constants.CORK, data.getCity());
    }

    @Test
    void testGetCity_forBarcelona() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.BARCELONA);
        assertEquals(Constants.BARCELONA, data.getCity());
    }

    @Test
    void testConstructor_allSupportedCities_doNotThrow() {
        for (String city : Constants.SUPPORTED_CITIES) {
            assertDoesNotThrow(() -> new DefaultWeatherData(city),
                "Should not throw for city: " + city);
        }
    }

    @Test
    void testConstructor_caseSensitive_throwsForLowerCase() {
        assertThrows(UnsupportedOperationException.class, () ->
            new DefaultWeatherData("paris")
        );
    }
}
