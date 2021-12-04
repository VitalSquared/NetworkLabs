package ru.nsu.spirin.snake.gamehandler;

import static me.ippolitov.fit.snakes.SnakesProto.Direction;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.datatransfer.NetNode;

import java.util.Map;
import java.util.Set;

public interface GameHandler {
    Player registerNewPlayer(@NotNull String playerName, NetNode netNode);
    void removePlayer(Player player);
    void moveAllSnakes(Map<Player, Direction> playersMoves);
    Snake getSnakeByPlayer(Player player);
    void addObserver(GameObserver gameObserver);
    void removeObserver(GameObserver gameObserver);
    void notifyObservers();
}
