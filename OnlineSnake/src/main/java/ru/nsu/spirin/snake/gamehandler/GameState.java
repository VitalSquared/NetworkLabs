package ru.nsu.spirin.snake.gamehandler;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;

import java.io.Serializable;
import java.util.List;

@RequiredArgsConstructor
public final class GameState implements Serializable {
    private final @Getter List<Point2D> fruits;
    private final @Getter List<Player> activePlayers;
    private final @Getter List<Snake> snakes;
    private final @Getter GameConfig gameConfig;
    private final @Getter int stateID;
}
