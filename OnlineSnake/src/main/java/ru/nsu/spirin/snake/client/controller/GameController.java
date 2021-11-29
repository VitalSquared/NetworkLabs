package ru.nsu.spirin.snake.client.controller;

import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.client.controller.events.UserEvent;

public interface GameController {
    void fireEvent(@NotNull UserEvent userEvent);
}
