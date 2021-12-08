package ru.nsu.spirin.snake.messages.messages;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto;

public final class ErrorMessage extends Message {
    private final @Getter String errorMessage;

    public ErrorMessage(String errorMessage, long messageSequence, int senderID, int receiverID) {
        super(MessageType.ERROR, messageSequence, senderID, receiverID);
        this.errorMessage = errorMessage;
    }

    @Override
    public SnakesProto.GameMessage getGameMessage() {
        var builder = SnakesProto.GameMessage.newBuilder();

        var errorBuilder = SnakesProto.GameMessage.ErrorMsg.newBuilder();
        errorBuilder.setErrorMessage(this.errorMessage);

        builder.setError(errorBuilder.build());
        builder.setMsgSeq(getMessageSequence());
        return builder.build();
    }
}
