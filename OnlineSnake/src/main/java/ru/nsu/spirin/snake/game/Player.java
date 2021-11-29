package ru.nsu.spirin.snake.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.ippolitov.fit.snakes.SnakesProto.NodeRole;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.datatransfer.NetNode;

import java.io.Serializable;
import java.util.Objects;

@RequiredArgsConstructor
public final class Player implements Serializable {
    private final @NotNull @Getter String name;
    private final @Getter int id;
    private final @Getter NetNode netNode;
    private @Getter @Setter NodeRole role = NodeRole.NORMAL;
    private @Getter @Setter int score = 0;

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Player other = (Player) object;
        return this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }
}
