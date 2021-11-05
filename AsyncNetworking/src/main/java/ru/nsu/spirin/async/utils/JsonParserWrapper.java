package ru.nsu.spirin.async.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public final class JsonParserWrapper {
    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.disable(FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @SneakyThrows
    public static <T> T parse(String content, Class<T> valueType) {
        return mapper.readValue(content, valueType);
    }

    @SneakyThrows
    public static <T> T parse(String content, TypeReference<T> valueTypeRef) {
        return mapper.readValue(content, valueTypeRef);
    }
}
