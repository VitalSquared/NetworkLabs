package ru.nsu.spirin.snake.utils;

import lombok.experimental.UtilityClass;
import me.ippolitov.fit.snakes.SnakesProto;
import org.apache.log4j.Logger;
import ru.nsu.spirin.snake.datatransfer.NetNode;
import ru.nsu.spirin.snake.gamehandler.GameState;
import ru.nsu.spirin.snake.gamehandler.Player;
import ru.nsu.spirin.snake.gamehandler.Point2D;
import ru.nsu.spirin.snake.gamehandler.Snake;

@UtilityClass
public final class StateUtils {
    private static final Logger logger = Logger.getLogger(StateUtils.class);

    public static GameState getStateFromMessage(SnakesProto.GameState state) {
        if (!validateGameState(state)) {
            logger.info("Game state doesn't have required fields");
            return null;
        }
        return new GameState(
                PointUtils.getPointList(state.getFoodsList()),
                PlayerUtils.getPlayerList(state.getPlayers().getPlayersList()),
                SnakeUtils.getSnakeList(state.getSnakesList(), state.getConfig()),
                state.getConfig(),
                state.getStateOrder()
        );
    }

    private static boolean validateGameState(SnakesProto.GameState state) {
        return state.hasStateOrder() &&
               state.hasPlayers() &&
               state.hasConfig();
    }

    public static SnakesProto.GameState createStateForMessage(GameState state) {
        var builder = SnakesProto.GameState.newBuilder();
        builder.setStateOrder(state.getStateID());
        for (Snake snake : state.getSnakes()) {
            builder.addSnakes(SnakeUtils.createSnakeForMessage(snake));
        }
        var coordBuilder = SnakesProto.GameState.Coord.newBuilder();
        for (Point2D fruit : state.getFruits()) {
            coordBuilder.setX(fruit.getX());
            coordBuilder.setY(fruit.getY());
            builder.addFoods(coordBuilder.build());
        }
        var playersBuilder = SnakesProto.GamePlayers.newBuilder();
        for (Player player : state.getActivePlayers()) {
            playersBuilder.addPlayers(PlayerUtils.createPlayerForMessage(player));
        }
        builder.setPlayers(playersBuilder.build());
        builder.setConfig(state.getGameConfig());
        return builder.build();
    }

    public static String getMasterNameFromState(GameState state) {
        for (var player : state.getActivePlayers()) {
            if (SnakesProto.NodeRole.MASTER.equals(player.getRole())) {
                return player.getName();
            }
        }
        return "";
    }

    public static Player getDeputyFromState(GameState state) {
        for (var player : state.getActivePlayers()) {
            if (SnakesProto.NodeRole.DEPUTY.equals(player.getRole())) {
                return player;
            }
        }
        return null;
    }
}
