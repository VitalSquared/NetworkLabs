package ru.nsu.spirin.async.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@RequiredArgsConstructor
public class GeoPosition {
    private @Getter final double latitude;
    private @Getter final double longitude;

    public String getLatitudeAsString() {
        return String.format(Locale.US, "%f", this.latitude);
    }

    public String getLongitudeAsString() {
        return String.format(Locale.US, "%f", this.longitude);
    }
}
