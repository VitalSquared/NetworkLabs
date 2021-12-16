package ru.nsu.spirin.snake.client.controller.events;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto.Direction;

public final class MoveEvent extends UserEvent {
    private final @Getter Direction direction;

    public MoveEvent(Direction direction) {
        super(UserEventType.MOVE);
        this.direction = direction;
    }
}
