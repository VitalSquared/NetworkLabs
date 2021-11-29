package ru.nsu.spirin.snake.config;

import lombok.experimental.UtilityClass;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@UtilityClass
public final class ConfigReader {
    private static final Logger logger = Logger.getLogger(ConfigReader.class);
    private static final Properties properties;

    static {
        properties = new Properties();

        InputStream stream = ClassLoader.getSystemResourceAsStream("config.properties");
        if (null != stream) {
            try (stream) {
                properties.load(stream);
            }
            catch (IOException exception) {
                logger.error(exception.getLocalizedMessage());
            }
        }
    }

    public static GameConfig getConfig() {
        GameConfig.Builder builder = GameConfig.newBuilder();
        builder.clear();
        builder.setDeadFoodProb(getFloatProperty(ConfigFieldNames.DEAD_FOOD_PROBABILITY, builder.getDeadFoodProb()));
        builder.setFoodPerPlayer(getFloatProperty(ConfigFieldNames.FOOD_PER_PLAYER, builder.getFoodPerPlayer()));
        builder.setHeight(getIntegerProperty(ConfigFieldNames.FIELD_HEIGHT, builder.getHeight()));
        builder.setWidth(getIntegerProperty(ConfigFieldNames.FIELD_WIDTH, builder.getWidth()));
        builder.setFoodStatic(getIntegerProperty(ConfigFieldNames.FOOD_STATIC, builder.getFoodStatic()));
        builder.setNodeTimeoutMs(getIntegerProperty(ConfigFieldNames.NODE_TIMEOUT_MS, builder.getNodeTimeoutMs()));
        builder.setPingDelayMs(getIntegerProperty(ConfigFieldNames.PING_DELAY_MS, builder.getPingDelayMs()));
        builder.setStateDelayMs(getIntegerProperty(ConfigFieldNames.STATE_DELAY_MS, builder.getStateDelayMs()));
        return builder.build();
    }

    private static float getFloatProperty(String key, float defaultValue) {
        try {
            return Float.parseFloat(properties.getProperty(key));
        }
        catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static int getIntegerProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        }
        catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
