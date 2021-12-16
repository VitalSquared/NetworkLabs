package ru.nsu.spirin.snake.client.controller.events;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import ru.nsu.spirin.snake.datatransfer.NetNode;

public final class JoinToGameEvent extends UserEvent {
    private final @Getter NetNode masterNode;
    private final @Getter String masterName;
    private final @Getter GameConfig config;

    public JoinToGameEvent(NetNode masterNode, String masterName, GameConfig config) {
        super(UserEventType.JOIN_GAME);
        this.masterNode = masterNode;
        this.masterName = masterName;
        this.config = config;
    }
}
