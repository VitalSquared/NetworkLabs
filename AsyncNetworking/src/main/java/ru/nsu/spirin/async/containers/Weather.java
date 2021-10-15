package ru.nsu.spirin.async.containers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public final class Weather {
    private @Getter @Setter @JsonProperty("weather") List<GeneralWeather> general;
    private @Getter @Setter @JsonProperty("main") WeatherParameters    parameters;
    private @Getter @Setter @JsonProperty("visibility") int visibility;
    private @Getter @Setter @JsonProperty("wind") Wind wind;
    private @Getter @Setter @JsonProperty("clouds") Clouds clouds;

    public static final class GeneralWeather {
        private @Getter @Setter @JsonProperty("main") String main;
        private @Getter @Setter @JsonProperty("description") String description;
    }

    public static final class WeatherParameters {
        private @Getter @Setter @JsonProperty("temp") double temperature;
        private @Getter @Setter @JsonProperty("feels_like") double feelsLikeTemperature;
        private @Getter @Setter @JsonProperty("temp_min") double minTemperature;
        private @Getter @Setter @JsonProperty("temp_max") double maxTemperature;
        private @Getter @Setter @JsonProperty("pressure") int pressure;
        private @Getter @Setter @JsonProperty("humidity") int humidity;
        private @Getter @Setter @JsonProperty("sea_level") int seaLevel;
        private @Getter @Setter @JsonProperty("grnd_level") int groundLevel;
    }

    public static final class Wind {
        private @Getter @Setter @JsonProperty("speed") double speed;
        private @Getter @Setter @JsonProperty("deg") int degree;
        private @Getter @Setter @JsonProperty("gust") double gust;

        public Direction getDirection() {
            int index = (this.degree % 360) / 45;
            return Direction.values()[index];
        }

        public enum Direction {
            NORTH("North"),
            NORTH_EAST("North-East"),
            EAST("East"),
            SOUTH_EAST("South-East"),
            SOUTH("South"),
            SOUTH_WEST("South-West"),
            WEST("West"),
            NORTH_WEST("North-West");

            private final String text;

            Direction(String text) {
                this.text = text;
            }

            @Override
            public String toString() {
                return this.text;
            }
        }
    }

    public static final class Clouds {
        private @Getter @Setter @JsonProperty("all") int clouds;
    }
}
