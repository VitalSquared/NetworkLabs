package ru.nsu.spirin.snake.datatransfer.messages;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto;
import me.ippolitov.fit.snakes.SnakesProto.NodeRole;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class RoleChangeMessage extends Message {
    private final @Getter NodeRole senderRole;
    private final @Getter NodeRole receiverRole;

    public RoleChangeMessage(@NotNull NodeRole senderRole, @NotNull NodeRole receiverRole, long messageSequence, int senderID, int receiverID) {
        super(MessageType.ROLE_CHANGE, messageSequence, senderID, receiverID);
        this.senderRole = Objects.requireNonNull(senderRole, "Role from cant be null");
        this.receiverRole = Objects.requireNonNull(receiverRole, "Role to cant be null");
    }

    @Override
    public SnakesProto.GameMessage getGameMessage() {
        var builder = SnakesProto.GameMessage.newBuilder();

        var roleBuilder = SnakesProto.GameMessage.RoleChangeMsg.newBuilder();
        roleBuilder.setSenderRole(this.senderRole);
        roleBuilder.setReceiverRole(this.receiverRole);

        builder.setRoleChange(roleBuilder.build());
        builder.setMsgSeq(getMessageSequence());
        builder.setSenderId(getSenderID());
        builder.setReceiverId(getReceiverID());
        return builder.build();
    }
}
