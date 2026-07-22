package com.acme.modres;

public class Constants {

  public static final String BARCELONA = "Barcelona";
  public static final String CORK = "Cork";
  public static final String MIAMI = "Miami";
  public static final String SAN_FRANCISCO = "San_Francisco";
  public final static String PARIS = "Paris";
  public static final String LAS_VEGAS = "Las_Vegas";

  public final static String[] SUPPORTED_CITIES = { PARIS, LAS_VEGAS, SAN_FRANCISCO, MIAMI, CORK, BARCELONA };

  public final static String BACELONA_WEATHER_FILE = "barcelona.json";
  public final static String CORK_WEATHER_FILE = "cork.json";
  public final static String LAS_VEGAS_WEATHER_FILE = "nv.json";
  public final static String MIAMI_WEATHER_FILE = "miami.json";
  public final static String PARIS_WEATHER_FILE = "paris.json";
  public final static String SAN_FRANCESCO_WEATHER_FILE = "sanfran.json";

  // constants used to construct Weather Underground API
  public final static String WUNDERGROUND_API_PREFIX = "http://api.wunderground.com/api/";
  public final static String WUNDERGROUND_API_PART = "/forecast/geolookup/conditions/q/";

  public final static String DATA_FORMAT = "MM/dd/yyyy";

  // Uber API constants
  public final static String UBER_API_BASE_URL = System.getenv("UBER_API_BASE_URL") != null
      ? System.getenv("UBER_API_BASE_URL") : "https://api.uber.com";
  public final static String UBER_AUTH_URL = System.getenv("UBER_AUTH_URL") != null
      ? System.getenv("UBER_AUTH_URL") : "https://login.uber.com/oauth/v2/token";
  public final static String DEFAULT_UBER_PRODUCT = "uberX";

  // Resort location constants
  public final static String RESORT_ADDRESS = "123 Resort Blvd, Las Vegas, NV 89109";
  public final static double RESORT_LATITUDE = 36.1699;
  public final static double RESORT_LONGITUDE = -115.1398;
  public final static String RESORT_TIMEZONE = "America/Los_Angeles";

}
