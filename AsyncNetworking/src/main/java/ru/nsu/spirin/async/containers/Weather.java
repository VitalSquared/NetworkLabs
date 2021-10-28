package ru.nsu.spirin.async.containers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public final class Weather {
    private @JsonProperty("weather") List<GeneralWeather> general;
    private @JsonProperty("main") WeatherParameters    parameters;
    private int visibility;
    private Wind wind;
    private Clouds clouds;

    @Getter @Setter
    public static final class GeneralWeather {
        private String main;
        private String description;
    }

    @Getter @Setter
    public static final class WeatherParameters {
        private @JsonProperty("temp") double temperature;
        private @JsonProperty("feels_like") double feelsLikeTemperature;
        private @JsonProperty("temp_min") double minTemperature;
        private @JsonProperty("temp_max") double maxTemperature;
        private int pressure;
        private int humidity;
        private @JsonProperty("sea_level") int seaLevel;
        private @JsonProperty("grnd_level") int groundLevel;
    }

    @Getter @Setter
    public static final class Wind {
        private double speed;
        private @JsonProperty("deg") int degree;
        private double gust;

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

    @Getter @Setter
    public static final class Clouds {
        private @JsonProperty("all") int clouds;
    }
}
