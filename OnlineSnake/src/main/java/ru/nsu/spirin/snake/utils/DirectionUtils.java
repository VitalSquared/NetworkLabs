package ru.nsu.spirin.snake.utils;

import lombok.experimental.UtilityClass;
import me.ippolitov.fit.snakes.SnakesProto.Direction;

@UtilityClass
public final class DirectionUtils {
    public static Direction getReversed(Direction direction) {
        if (null == direction) {
            return null;
        }
        return switch (direction) {
            case DOWN -> Direction.UP;
            case UP -> Direction.DOWN;
            case RIGHT -> Direction.LEFT;
            case LEFT -> Direction.RIGHT;
        };
    }
}
