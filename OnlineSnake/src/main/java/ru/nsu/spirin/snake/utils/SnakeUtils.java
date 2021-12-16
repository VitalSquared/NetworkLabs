package ru.nsu.spirin.snake.utils;

import lombok.experimental.UtilityClass;
import me.ippolitov.fit.snakes.SnakesProto;
import org.apache.log4j.Logger;
import ru.nsu.spirin.snake.gamehandler.Snake;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@UtilityClass
public final class SnakeUtils {
    private static final Logger logger = Logger.getLogger(SnakeUtils.class);

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
        return snakes.stream().map(snake -> {
                    if (!validateSnake(snake)) {
                        logger.info("Snake doesn't have required fields");
                        return null;
                    }
                    return new Snake(
                            snake.getPlayerId(),
                            PointUtils.getPointList(snake.getPointsList()),
                            snake.getState(),
                            snake.getHeadDirection(),
                            config.getWidth(),
                            config.getHeight()
                    );
                }
        ).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private boolean validateSnake(SnakesProto.GameState.Snake snake) {
        return snake.hasPlayerId() &&
               snake.hasState() &&
               snake.hasHeadDirection() &&
               snake.getPointsCount() >= 2;
    }
}
