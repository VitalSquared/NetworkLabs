package ru.nsu.spirin.async.utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.log4j.Logger;

public final class APIRequestGenerator {
    public static final int MIN_ENTRIES_LIMIT = 1;
    public static final int MAX_ENTRIES_LIMIT = 10;

    public static final int MIN_RADIUS_METERS = 100;
    public static final int MAX_RADIUS_METERS = 10000;

    private static final Logger logger = Logger.getLogger(APIRequestGenerator.class);

    private static final String REQUEST_LANGUAGE = "ru";

    private static final String GEO_API_KEY = "e05c4b44-b78e-4dce-ad43-0183607d5610";
    private static final String OTM_API_KEY = "5ae2e3f221c38a28845f05b6be4a37dab9dfb1e566d9e74817be9d50";
    private static final String OWP_API_KEY = "4ff48f6d420194b719d75f4af226cf80";

    private static final OkHttpClient httpClient = new OkHttpClient();

    public static Request createAddressesRequest(String address, int countLimit) {
        String url = String.format(
                "https://graphhopper.com/api/1/geocode?q=%s&limit=%d&locale=%s&debug=true&key=%s",
                address.toLowerCase(),
                clampValue(countLimit, MIN_ENTRIES_LIMIT, MAX_ENTRIES_LIMIT),
                REQUEST_LANGUAGE,
                GEO_API_KEY);
        return new Request.Builder().url(url).get().build();
    }

    public static Request createFeaturesRequest(GeoPosition position, int radius, int countLimit) {
        String url = String.format(
                "http://api.opentripmap.com/0.1/%s/places/radius?lang=%s&radius=%s&lon=%s&lat=%s&format=json&limit=%d&apikey=%s",
                REQUEST_LANGUAGE,
                REQUEST_LANGUAGE,
                clampValue(radius, MIN_RADIUS_METERS, MAX_RADIUS_METERS) + ".0",
                position.getLongitudeAsString(),
                position.getLatitudeAsString(),
                clampValue(countLimit, MIN_ENTRIES_LIMIT, MAX_ENTRIES_LIMIT), OTM_API_KEY);
        return new Request.Builder().url(url).get().build();
    }

    public static Request createFeatureDescriptionRequest(String featureXID) {
        String url = String.format(
                "http://api.opentripmap.com/0.1/%s/places/xid/%s?apikey=%s",
                REQUEST_LANGUAGE,
                featureXID,
                OTM_API_KEY);
        return new Request.Builder().url(url).get().build();
    }

    public static Request createWeatherRequest(GeoPosition position) {
        String url = String.format(
                "http://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&units=metric&lang=%s&appid=%s",
                position.getLongitudeAsString(),
                position.getLatitudeAsString(),
                REQUEST_LANGUAGE,
                OWP_API_KEY);
        return new Request.Builder().url(url).get().build();
    }

    public static Response createResponse(Request request) {
        Response response;
        try {
            response = httpClient.newCall(request).execute();
        }
        catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            response = null;
        }
        return response;
    }

    private static int clampValue(int value, int min, int max) {
        return Math.min(Math.max(min, value), max);
    }
}
