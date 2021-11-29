package ru.nsu.spirin.snake.client.network;

import me.ippolitov.fit.snakes.SnakesProto;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.datatransfer.NetNode;

public interface NetworkHandler {
    void startNewGame();
    void joinToGame(@NotNull NetNode gameOwner, @NotNull String playerName);
    void handleMove(@NotNull SnakesProto.Direction direction);
    void exit();
}
