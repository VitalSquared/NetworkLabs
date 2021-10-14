package ru.nsu.spirin.async.containers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import ru.nsu.spirin.async.utils.GeoPosition;

public final class Feature {
    private @Getter @Setter @JsonProperty("xid") String xid;
    private @Getter @Setter @JsonProperty("name") String name;
    private @Getter @Setter @JsonProperty("kinds") String kinds;
    private @Getter @Setter @JsonProperty("point") FeatureGeoPosition position;

    private static final class FeatureGeoPosition extends GeoPosition {
        @JsonCreator
        public FeatureGeoPosition(@JsonProperty("lat") double latitude, @JsonProperty("lon") double longitude) {
            super(latitude, longitude);
        }
    }
}
