package ru.nsu.spirin.snake.datatransfer.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.ippolitov.fit.snakes.SnakesProto;

import java.io.Serializable;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class Message implements Serializable {
    private final @Getter UUID uuid = UUID.randomUUID();
    private final @Getter MessageType type;
    private final @Getter long messageSequence;
    private final @Getter int senderID;
    private final @Getter int receiverID;

    public abstract SnakesProto.GameMessage getGameMessage();
}
