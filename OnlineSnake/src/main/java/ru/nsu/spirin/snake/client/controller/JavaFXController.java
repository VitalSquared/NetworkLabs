package ru.nsu.spirin.snake.client.controller;

import lombok.RequiredArgsConstructor;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import ru.nsu.spirin.snake.client.network.NetworkHandler;
import ru.nsu.spirin.snake.client.view.GameView;
import ru.nsu.spirin.snake.client.controller.events.JoinToGameEvent;
import ru.nsu.spirin.snake.client.controller.events.MoveEvent;
import ru.nsu.spirin.snake.client.controller.events.UserEvent;

@RequiredArgsConstructor
public final class JavaFXController implements GameController {
    private final GameConfig playerConfig;
    private final String playerName;
    private final NetworkHandler gameNetwork;
    private final GameView view;

    @Override
    public void fireEvent(UserEvent userEvent) {
        switch (userEvent.getType()) {
            case NEW_GAME -> {
                this.view.setConfig(this.playerConfig);
                this.gameNetwork.startNewGame();
            }
            case JOIN_GAME -> {
                JoinToGameEvent joinEvent = (JoinToGameEvent) userEvent;
                this.view.setConfig(joinEvent.getConfig());
                this.gameNetwork.joinToGame(joinEvent.getMasterNode(), this.playerName);
            }
            case MOVE -> {
                MoveEvent moveEvent = (MoveEvent) userEvent;
                this.gameNetwork.handleMove(moveEvent.getDirection());
            }
            case EXIT -> this.gameNetwork.exit();
        }
    }
}
