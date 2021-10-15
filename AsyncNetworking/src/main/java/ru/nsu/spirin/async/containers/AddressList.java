package ru.nsu.spirin.async.containers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import ru.nsu.spirin.async.utils.GeoPosition;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AddressList {
    private @JsonProperty("hits") @Getter @Setter List<Address> addresses;

    public static final class Address {
        private @Getter @Setter @JsonProperty("name") String name;
        private @Getter @Setter @JsonProperty("country") String country;
        private @Getter @Setter @JsonProperty("city") String city;
        private @Getter @Setter @JsonProperty("street") String street;
        private @Getter @Setter @JsonProperty("housenumber") String houseNumber;
        private @Getter @Setter @JsonProperty("countrycode") String countryCode;
        private @Getter @Setter @JsonProperty("point") AddressGeoPosition position;

        @Override
        public String toString() {
            return this.name + "; " + Stream.of(country, city, street, houseNumber).filter(s -> s != null && !s.isEmpty()).collect(
                    Collectors.joining(", "));
        }
    }

    private static final class AddressGeoPosition extends GeoPosition {
        @JsonCreator
        public AddressGeoPosition(@JsonProperty("lat") double latitude, @JsonProperty("lng") double longitude) {
            super(latitude, longitude);
        }
    }
}
