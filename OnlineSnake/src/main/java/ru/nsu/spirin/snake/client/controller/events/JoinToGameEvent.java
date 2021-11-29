package ru.nsu.spirin.snake.client.controller.events;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.datatransfer.NetNode;

public final class JoinToGameEvent extends UserEvent {
    private final @NotNull @Getter NetNode masterNode;
    private final @NotNull @Getter String masterName;
    private final @NotNull @Getter GameConfig config;

    public JoinToGameEvent(@NotNull NetNode masterNode, @NotNull String masterName, @NotNull GameConfig config) {
        super(UserEventType.JOIN_GAME);
        this.masterNode = masterNode;
        this.masterName = masterName;
        this.config = config;
    }
}
