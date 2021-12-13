package ru.nsu.spirin.snake.client.network;

import me.ippolitov.fit.snakes.SnakesProto.Direction;
import ru.nsu.spirin.snake.datatransfer.NetNode;
import ru.nsu.spirin.snake.multicastreceiver.GameInfo;

import java.util.Set;

public interface NetworkHandler {
    void startNewGame();

    void joinToGame(NetNode gameOwner, String playerName);

    void handleMove(Direction direction);

    void exit();

    void updateActiveGames(Set<GameInfo> gameInfos);
}
