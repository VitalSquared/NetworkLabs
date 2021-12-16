package ru.nsu.spirin.snake.messages.messages;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto;
import ru.nsu.spirin.snake.gamehandler.GameState;
import ru.nsu.spirin.snake.gamehandler.Player;
import ru.nsu.spirin.snake.utils.StateUtils;

import java.util.List;

public final class StateMessage extends Message {
    private final @Getter GameState gameState;
    private final @Getter List<Player> players;

    public StateMessage(GameState gameState, List<Player> players, long messageSequence, int senderID, int receiverID) {
        super(MessageType.STATE, messageSequence, senderID, receiverID);
        this.gameState = gameState;
        this.players = players;
    }

    public StateMessage(GameState gameState, long messageSequence, int senderID, int receiverID) {
        this(gameState, gameState.getActivePlayers(), messageSequence, senderID, receiverID);
    }

    @Override
    public SnakesProto.GameMessage getGameMessage() {
        var builder = SnakesProto.GameMessage.newBuilder();

        var stateBuilder = SnakesProto.GameMessage.StateMsg.newBuilder();
        stateBuilder.setState(StateUtils.createStateForMessage(this.gameState));

        builder.setState(stateBuilder.build());
        builder.setMsgSeq(getMessageSequence());
        return builder.build();
    }
}
