package ru.nsu.spirin.snake.client.view;

import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.gamehandler.GameState;
import ru.nsu.spirin.snake.multicastreceiver.GameInfo;

import java.util.Collection;

public interface GameView {
    void setConfig(@NotNull GameConfig gameConfig);
    void updateCurrentGame(GameState state);
    void updateGameList(@NotNull Collection<GameInfo> gameInfos);
}
