package ru.nsu.spirin.snake.datatransfer.messages;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto;
import org.jetbrains.annotations.NotNull;

public final class ErrorMessage extends Message {
    private final @Getter String errorMessage;

    public ErrorMessage(@NotNull String errorMessage, long messageSequence) {
        super(MessageType.ERROR, messageSequence, -1, -1);
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
