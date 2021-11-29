package ru.nsu.spirin.snake.config;

import lombok.experimental.UtilityClass;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;

@UtilityClass
public final class ConfigValidator {
    private static final int MIN_FIELD_WIDTH = 10;
    private static final int MAX_FIELD_WIDTH = 100;

    private static final int MIN_FIELD_HEIGHT = 10;
    private static final int MAX_FIELD_HEIGHT = 100;

    private static final float MIN_FOOD_PER_PLAYER = 0.0f;
    private static final float MAX_FOOD_PER_PLAYER = 100.0f;

    private static final int MIN_FOOD_STATIC = 0;
    private static final int MAX_FOOD_STATIC = 100;

    private static final int MIN_PING_DELAY_MS = 1;
    private static final int MAX_PING_DELAY_MS = 10000;

    private static final int MIN_STATE_DELAY_MS = 1;
    private static final int MAX_STATE_DELAY_MS = 10000;

    private static final int MIN_NODE_TIMEOUT_MS = 1;
    private static final int MAX_NODE_TIMEOUT_MS = 10000;

    private static final float MIN_DEAD_FOOD_PROB = 0.0f;
    private static final float MAX_DEAD_FOOD_PROB = 1.0f;

    public static void validate(GameConfig config) {
        validateIntField(config.getWidth(), MIN_FIELD_WIDTH, MAX_FIELD_WIDTH, ConfigFieldNames.FIELD_WIDTH);
        validateIntField(config.getHeight(), MIN_FIELD_HEIGHT, MAX_FIELD_HEIGHT, ConfigFieldNames.FIELD_HEIGHT);
        validateFloatField(config.getFoodPerPlayer(), MIN_FOOD_PER_PLAYER, MAX_FOOD_PER_PLAYER, ConfigFieldNames.FOOD_PER_PLAYER);
        validateIntField(config.getFoodStatic(), MIN_FOOD_STATIC, MAX_FOOD_STATIC, ConfigFieldNames.FOOD_STATIC);
        validateIntField(config.getPingDelayMs(), MIN_PING_DELAY_MS, MAX_PING_DELAY_MS, ConfigFieldNames.PING_DELAY_MS);
        validateIntField(config.getStateDelayMs(), MIN_STATE_DELAY_MS, MAX_STATE_DELAY_MS, ConfigFieldNames.STATE_DELAY_MS);
        validateIntField(config.getNodeTimeoutMs(), MIN_NODE_TIMEOUT_MS, MAX_NODE_TIMEOUT_MS, ConfigFieldNames.NODE_TIMEOUT_MS);
        validateFloatField(config.getDeadFoodProb(), MIN_DEAD_FOOD_PROB, MAX_DEAD_FOOD_PROB, ConfigFieldNames.DEAD_FOOD_PROBABILITY);
    }

    private static void validateIntField(int fieldValue, int minValue, int maxValue, String fieldName) {
        if (fieldValue < minValue || fieldValue > maxValue) {
            throw new IllegalStateException(String.format("%s=%d is not in range [%d, %d]", fieldName, fieldValue, minValue, maxValue));
        }
    }

    private static void validateFloatField(float fieldValue, float minValue, float maxValue, String fieldName) {
        if (Float.compare(fieldValue, minValue) < 0 || Float.compare(fieldValue, maxValue) > 0) {
            throw new IllegalStateException(String.format("%s=%f is not in range [%f, %f]", fieldName, fieldValue, minValue, maxValue));
        }
    }
}
