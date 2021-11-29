package ru.nsu.spirin.snake.datatransfer.messages;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.game.GameState;
import ru.nsu.spirin.snake.game.Player;
import ru.nsu.spirin.snake.utils.StateUtils;

import java.util.List;
import java.util.Objects;

public final class StateMessage extends Message {
    private final @NotNull @Getter GameState gameState;
    private final @NotNull @Getter List<Player> playersNode;

    public StateMessage(@NotNull GameState gameState, @NotNull List<Player> players, long messageSequence) {
        super(MessageType.STATE, messageSequence, -1, -1);
        this.gameState = Objects.requireNonNull(gameState, "Game state cant be null");
        this.playersNode = Objects.requireNonNull(players, "Node-Players map cant be null");
    }

    public StateMessage(@NotNull GameState gameState, long messageSequence) {
        this(gameState, gameState.getActivePlayers(), messageSequence);
    }

    @Override
    public SnakesProto.GameMessage getGameMessage() {
        var builder = SnakesProto.GameMessage.newBuilder();

        var stateBuilder = SnakesProto.GameMessage.StateMsg.newBuilder();
        stateBuilder.setState(StateUtils.createStateForMessage(gameState));

        builder.setState(stateBuilder.build());
        builder.setMsgSeq(getMessageSequence());
        return builder.build();
    }
}
