package ru.nsu.spirin.snake.messages.messages;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto;
import org.jetbrains.annotations.NotNull;

public final class JoinMessage extends Message {
    private final @Getter String playerName;

    public JoinMessage(@NotNull String playerName, long messageSequence) {
        super(MessageType.JOIN, messageSequence, -1, -1);
        this.playerName = playerName;
    }

    @Override
    public SnakesProto.GameMessage getGameMessage() {
        var builder = SnakesProto.GameMessage.newBuilder();

        var joinBuilder = SnakesProto.GameMessage.JoinMsg.newBuilder();
        joinBuilder.setName(this.playerName);

        builder.setJoin(joinBuilder.build());
        builder.setMsgSeq(getMessageSequence());
        return builder.build();
    }
}
