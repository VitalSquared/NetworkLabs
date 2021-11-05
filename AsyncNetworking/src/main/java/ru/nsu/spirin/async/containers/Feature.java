package ru.nsu.spirin.async.containers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import ru.nsu.spirin.async.utils.GeoPosition;

@Getter @Setter
public final class Feature {
    private String xid;
    private String name;
    private String kinds;
    private @JsonProperty("point") GeoPosition position;
}
