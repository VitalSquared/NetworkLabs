package ru.nsu.spirin.snake.messages.messages;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.gamehandler.Player;
import ru.nsu.spirin.snake.utils.PlayerUtils;

import java.util.List;
import java.util.Objects;

public final class AnnouncementMessage extends Message {
    private final @NotNull @Getter GameConfig config;
    private final @Getter List<Player> players;
    private final @Getter boolean canJoin;

    public AnnouncementMessage(@NotNull GameConfig config, List<Player> players, boolean canJoin, long messageSequence) {
        super(MessageType.ANNOUNCEMENT, messageSequence, -1, -1);
        this.players = players;
        this.config = Objects.requireNonNull(config, "Config cant be null");
        this.canJoin = canJoin;
    }

    @Override
    public SnakesProto.GameMessage getGameMessage() {
        var builder = SnakesProto.GameMessage.newBuilder();

        var announcementBuilder = SnakesProto.GameMessage.AnnouncementMsg.newBuilder();
        var gamePlayersBuilder = SnakesProto.GamePlayers.newBuilder();
        for (var player : this.players) {
            gamePlayersBuilder.addPlayers(PlayerUtils.createPlayerForMessage(player));
        }
        announcementBuilder.setPlayers(gamePlayersBuilder.build());
        announcementBuilder.setConfig(this.config);
        announcementBuilder.setCanJoin(this.canJoin);

        builder.setAnnouncement(announcementBuilder.build());
        builder.setMsgSeq(getMessageSequence());
        return builder.build();
    }
}
