package ru.nsu.spirin.snake.gamehandler;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.Serializable;
import java.util.List;

@RequiredArgsConstructor
public final class GameState implements Serializable {
    private final @Unmodifiable @NotNull @Getter List<Point2D> fruits;

    private final @Unmodifiable @NotNull @Getter List<Player> activePlayers;

    private final @Unmodifiable @NotNull @Getter List<Snake> snakes;

    private final @NotNull @Getter GameConfig gameConfig;

    private final @Getter int stateID;
}
