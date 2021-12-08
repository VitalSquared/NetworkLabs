package ru.nsu.spirin.snake.client.network;

import me.ippolitov.fit.snakes.SnakesProto.Direction;
import ru.nsu.spirin.snake.datatransfer.NetNode;

public interface NetworkHandler {
    void startNewGame();

    void joinToGame(NetNode gameOwner, String playerName);

    void handleMove(Direction direction);

    void exit();
}
