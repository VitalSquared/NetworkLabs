package ru.nsu.spirin.async.containers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public final class FeatureDescription {
    private String xid;
    private String name;
    private Info info;
    private @JsonProperty("wikipedia_extracts") WikipediaExtracts wikipediaExtracts;

    private boolean isNull = false;

    @Getter @Setter
    public static final class Info {
        private @JsonProperty("descr") String description;
    }

    @Getter @Setter
    public static final class WikipediaExtracts {
        private String title;
        private String text;
    }
}
