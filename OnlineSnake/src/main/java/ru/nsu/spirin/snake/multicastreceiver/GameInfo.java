package ru.nsu.spirin.snake.multicastreceiver;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.game.Player;
import ru.nsu.spirin.snake.datatransfer.NetNode;

import java.util.List;
import java.util.Objects;

public final class GameInfo {
    private final @NotNull @Getter GameConfig config;
    private final @NotNull @Getter NetNode masterNode;
    private final @Getter List<Player> players;
    private final @Getter boolean canJoin;

    private @NotNull @Getter String masterNodeName = "";

    public GameInfo(@NotNull GameConfig config, @NotNull NetNode masterNode, List<Player> players, boolean canJoin) {
        this.config = config;
        this.masterNode = masterNode;
        this.players = players;
        this.canJoin = canJoin;
        setMasterNodeName();
    }

    private void setMasterNodeName() {
        for (var player : this.players) {
            if (player.getRole() == SnakesProto.NodeRole.MASTER) {
                masterNodeName = player.getName();
                return;
            }
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        GameInfo other = (GameInfo) object;
        return (this.canJoin == other.canJoin) && (this.players == other.players) && this.config.equals(other.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(config, canJoin, players);
    }
}
