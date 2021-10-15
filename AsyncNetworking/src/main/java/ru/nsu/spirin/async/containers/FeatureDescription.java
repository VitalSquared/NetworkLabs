package ru.nsu.spirin.async.containers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public final class FeatureDescription {
    private @Getter @Setter @JsonProperty("xid") String xid;
    private @Getter @Setter @JsonProperty("name") String name;
    private @Getter @Setter @JsonProperty("info") Info info;
    private @Getter @Setter @JsonProperty("wikipedia_extracts") WikipediaExtracts wikipediaExtracts;

    public static final class Info {
        private @Getter @Setter @JsonProperty("descr") String description;

        @Override
        public String toString() {
            return this.description;
        }
    }

    public static final class WikipediaExtracts {
        private @Getter @Setter @JsonProperty("title") String title;
        private @Getter @Setter @JsonProperty("text") String text;
    }
}
