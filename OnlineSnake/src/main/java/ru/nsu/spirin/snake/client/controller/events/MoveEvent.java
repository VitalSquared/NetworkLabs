package ru.nsu.spirin.snake.client.controller.events;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto.Direction;
import org.jetbrains.annotations.NotNull;

public final class MoveEvent extends UserEvent {
    private final @NotNull @Getter Direction direction;

    public MoveEvent(@NotNull Direction direction) {
        super(UserEventType.MOVE);
        this.direction = direction;
    }
}
