package ru.nsu.spirin.snake.client.view.javafx;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.datatransfer.NetNode;
import ru.nsu.spirin.snake.multicastreceiver.GameInfo;

import java.util.Objects;

public final class ActiveGameButton {
    private final @NotNull @Getter NetNode masterNode;
    private final @Getter int playersNumber;

    private final @NotNull @Getter GameConfig config;
    private final boolean canJoin;

    private final @Getter String masterNodeName;
    private final @Getter String fieldSize;
    private final @Getter String foodNumber;

    private final @Getter Button button;

    public ActiveGameButton(@NotNull GameInfo gameInfo) {
        Objects.requireNonNull(gameInfo);
        this.playersNumber = gameInfo.getPlayers().size();
        this.config = gameInfo.getConfig();
        this.masterNode = gameInfo.getMasterNode();
        this.canJoin = gameInfo.isCanJoin();
        this.button = new Button("Вход");
        this.fieldSize = this.config.getHeight() + "x" + this.config.getWidth();
        this.foodNumber = this.config.getFoodStatic() + ": x" + this.config.getFoodPerPlayer();
        this.masterNodeName = gameInfo.getMasterNodeName();
        designButton();
    }

    private void designButton() {
        this.button.setBackground(new Background(new BackgroundFill(Color.BLUE, new CornerRadii(0), new Insets(0))));
        this.button.setTextFill(Color.WHITE);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ActiveGameButton)) {
            return false;
        }
        ActiveGameButton other = (ActiveGameButton) object;
        return this.masterNode.equals(other.masterNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.masterNode);
    }
}
