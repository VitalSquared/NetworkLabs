package ru.nsu.spirin.snake.utils;

import lombok.experimental.UtilityClass;
import me.ippolitov.fit.snakes.SnakesProto;
import ru.nsu.spirin.snake.gamehandler.Snake;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public final class SnakeUtils {
    public static SnakesProto.GameState.Snake createSnakeForMessage(Snake snake) {
        var builder = SnakesProto.GameState.Snake.newBuilder();
        builder.setPlayerId(snake.getPlayerID());
        builder.setState(snake.getState());
        builder.setHeadDirection(snake.getDirection());
        var coordBuilder = SnakesProto.GameState.Coord.newBuilder();
        for (var point : snake.getPoints()) {
            coordBuilder.setX(point.getX());
            coordBuilder.setY(point.getY());
            builder.addPoints(coordBuilder.build());
        }
        return builder.build();
    }

    public static List<Snake> getSnakeList(List<SnakesProto.GameState.Snake> snakes, SnakesProto.GameConfig config) {
        return snakes.stream().map(snake ->
                new Snake(
                        snake.getPlayerId(),
                        PointUtils.getPointList(snake.getPointsList()),
                        snake.getState(),
                        snake.getHeadDirection(),
                        config.getWidth(),
                        config.getHeight()
                )
        ).collect(Collectors.toList());
    }
}
