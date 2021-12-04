package ru.nsu.spirin.snake.datatransfer.messages;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto;
import me.ippolitov.fit.snakes.SnakesProto.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class SteerMessage extends Message {
    private final @NotNull @Getter Direction direction;

    public SteerMessage(@NotNull Direction direction, long messageSequence) {
        super(MessageType.STEER, messageSequence, -1, -1);
        this.direction = Objects.requireNonNull(direction, "Direction cant be null");
    }

    @Override
    public SnakesProto.GameMessage getGameMessage() {
        var builder = SnakesProto.GameMessage.newBuilder();

        var steerBuilder = SnakesProto.GameMessage.SteerMsg.newBuilder();
        steerBuilder.setDirection(this.direction);

        builder.setSteer(steerBuilder.build());
        builder.setMsgSeq(getMessageSequence());
        return builder.build();
    }
}
