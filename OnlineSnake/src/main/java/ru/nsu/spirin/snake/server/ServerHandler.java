package ru.nsu.spirin.snake.server;

import ru.nsu.spirin.snake.gamehandler.GameState;

public interface ServerHandler {
    void update(GameState state);

    int getPort();

    void stop();
}
