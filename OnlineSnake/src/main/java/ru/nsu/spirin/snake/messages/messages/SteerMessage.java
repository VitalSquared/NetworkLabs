package ru.nsu.spirin.snake.messages.messages;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto;
import me.ippolitov.fit.snakes.SnakesProto.Direction;

public final class SteerMessage extends Message {
    private final @Getter Direction direction;

    public SteerMessage(Direction direction, long messageSequence, int senderID, int receiverID) {
        super(MessageType.STEER, messageSequence, senderID, receiverID);
        this.direction = direction;
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
