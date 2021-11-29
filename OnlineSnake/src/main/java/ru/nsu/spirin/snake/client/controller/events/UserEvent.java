package ru.nsu.spirin.snake.client.controller.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public abstract class UserEvent {
    private final @NotNull @Getter UserEventType type;
}
