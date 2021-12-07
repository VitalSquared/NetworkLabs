package ru.nsu.spirin.snake.messages.messages;

import me.ippolitov.fit.snakes.SnakesProto;

public final class PingMessage extends Message {
    public PingMessage(long messageSequence) {
        super(MessageType.PING, messageSequence, -1, -1);
    }

    @Override
    public SnakesProto.GameMessage getGameMessage() {
        var builder = SnakesProto.GameMessage.newBuilder();
        builder.setPing(SnakesProto.GameMessage.PingMsg.newBuilder().build());
        builder.setMsgSeq(getMessageSequence());
        return builder.build();
    }
}
