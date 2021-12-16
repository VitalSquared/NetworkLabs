package ru.nsu.spirin.snake.client.view.javafx;

import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.datatransfer.NetNode;
import ru.nsu.spirin.snake.gamehandler.Player;

import java.util.*;

public final class PlayerColorMapper {
    private final @NotNull Map<Player, Color> playerColors;
    private int prevColorIndex;

    private static final List<Color> snakeColors = List.of(
            Color.RED,
            Color.ORANGE,
            Color.PURPLE
    );

    private static final Color ZOMBIE_SNAKE_COLOR = Color.BLACK;
    private static final Color MY_SNAKE_COLOR = Color.BLUE;

    public PlayerColorMapper() {
        this.playerColors = new HashMap<>();
        this.prevColorIndex = new Random().nextInt(snakeColors.size());
    }

    @NotNull
    public Optional<Color> getColor(@NotNull Player player, NetNode self) {
        if (self != null) {
            if (player.getNetNode().equals(self)) {
                return Optional.of(MY_SNAKE_COLOR);
            }
        }
        return Optional.ofNullable(this.playerColors.get(player));
    }

    public void addPlayer(@NotNull Player player) {
        int currentColorIndex = (this.prevColorIndex + 1) % snakeColors.size();
        this.playerColors.put(Objects.requireNonNull(player), snakeColors.get(currentColorIndex));
        this.prevColorIndex = currentColorIndex;
    }

    public void removePlayer(@NotNull Player player) {
        Objects.requireNonNull(player, "Player for remove cant be null");
        this.playerColors.remove(player);
    }

    public boolean isPlayerRegistered(@NotNull Player player) {
        Objects.requireNonNull(player, "Player cant be null");
        return this.playerColors.containsKey(player);
    }

    @NotNull
    public Set<Player> getRegisteredPlayers() {
        return Collections.unmodifiableSet(this.playerColors.keySet());
    }

    @NotNull
    public Color getZombieSnakeColor() {
        return ZOMBIE_SNAKE_COLOR;
    }
}
