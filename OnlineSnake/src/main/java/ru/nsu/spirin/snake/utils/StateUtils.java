package ru.nsu.spirin.snake.utils;

import lombok.experimental.UtilityClass;
import me.ippolitov.fit.snakes.SnakesProto;
import ru.nsu.spirin.snake.datatransfer.NetNode;
import ru.nsu.spirin.snake.gamehandler.GameState;
import ru.nsu.spirin.snake.gamehandler.Player;
import ru.nsu.spirin.snake.gamehandler.Point2D;
import ru.nsu.spirin.snake.gamehandler.Snake;

@UtilityClass
public final class StateUtils {
    public static GameState getStateFromMessage(SnakesProto.GameState state) {
        return new GameState(
                PointUtils.getPointList(state.getFoodsList()),
                PlayerUtils.getPlayerList(state.getPlayers().getPlayersList()),
                SnakeUtils.getSnakeList(state.getSnakesList(), state.getConfig()),
                state.getConfig(),
                state.getStateOrder()
        );
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

    public static NetNode getDeputyFromState(GameState state) {
        for (var player : state.getActivePlayers()) {
            if (SnakesProto.NodeRole.DEPUTY.equals(player.getRole())) {
                return player.getNetNode();
            }
        }
        return null;
    }
}
