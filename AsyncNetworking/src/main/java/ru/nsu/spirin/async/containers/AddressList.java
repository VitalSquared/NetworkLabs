package ru.nsu.spirin.async.containers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import ru.nsu.spirin.async.utils.GeoPosition;

import java.util.List;

public final class AddressList {
    private @Getter @Setter @JsonProperty("hits") List<Address> addresses;

    @Getter @Setter
    public static final class Address {
        private String name;
        private String country;
        private String city;
        private String state;
        private String street;
        private @JsonProperty("housenumber") String houseNumber;
        private @JsonProperty("countrycode") String countryCode;
        private @JsonProperty("point") GeoPosition position;
    }
}
