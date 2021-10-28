package ru.nsu.spirin.async.utils;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;

@Getter @Setter
public class GeoPosition {
    private @JsonAlias("lat") double latitude;
    private @JsonAlias({"lng", "lon"}) double longitude;

    public String getLatitudeAsString() {
        return String.format(Locale.US, "%f", this.latitude);
    }

    public String getLongitudeAsString() {
        return String.format(Locale.US, "%f", this.longitude);
    }
}
