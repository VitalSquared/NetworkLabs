package ru.nsu.spirin.snake.messages;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.experimental.UtilityClass;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage;
import org.apache.commons.lang3.SerializationException;
import org.apache.log4j.Logger;
import ru.nsu.spirin.snake.gamehandler.GameState;
import ru.nsu.spirin.snake.messages.messages.AckMessage;
import ru.nsu.spirin.snake.messages.messages.AnnouncementMessage;
import ru.nsu.spirin.snake.messages.messages.ErrorMessage;
import ru.nsu.spirin.snake.messages.messages.JoinMessage;
import ru.nsu.spirin.snake.messages.messages.Message;
import ru.nsu.spirin.snake.messages.messages.PingMessage;
import ru.nsu.spirin.snake.messages.messages.RoleChangeMessage;
import ru.nsu.spirin.snake.messages.messages.StateMessage;
import ru.nsu.spirin.snake.messages.messages.SteerMessage;
import ru.nsu.spirin.snake.utils.PlayerUtils;
import ru.nsu.spirin.snake.utils.StateUtils;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

@UtilityClass
public final class MessageParser {
    private static final Logger logger = Logger.getLogger(MessageParser.class);

    public static DatagramPacket serializeMessage(Message message, InetAddress destAddress, int destPort) {
        byte[] messageBytes = message.getGameMessage().toByteArray();
        return new DatagramPacket(
                messageBytes,
                messageBytes.length,
                destAddress,
                destPort
        );
    }

    public static Message deserializeMessage(DatagramPacket packet) throws ClassCastException, SerializationException, InvalidProtocolBufferException {
        GameMessage message = GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
        validate(message.hasMsgSeq(), "No message sequence");
        if (message.hasAck()) {
            validate(message.hasSenderId(), "No sender id");
            validate(message.hasReceiverId(), "No receiver id");
            return new AckMessage(
                    message.getMsgSeq(),
                    message.getSenderId(),
                    message.getReceiverId());
        }
        else if (message.hasAnnouncement()) {
            validate(message.getAnnouncement().hasConfig(), "No config");
            validate(message.getAnnouncement().hasPlayers(), "No players");
            return new AnnouncementMessage(
                    message.getAnnouncement().getConfig(),
                    PlayerUtils.getPlayerList(message.getAnnouncement().getPlayers().getPlayersList()),
                    !message.getAnnouncement().hasCanJoin() || message.getAnnouncement().getCanJoin(),
                    message.getMsgSeq()
            );
        }
        else if (message.hasError()) {
            validate(message.getError().hasErrorMessage(), "No error message");
            return new ErrorMessage(
                    message.getError().getErrorMessage(),
                    message.getMsgSeq(),
                    message.hasSenderId() ? message.getSenderId() : -1,
                    message.hasReceiverId() ? message.getReceiverId() : -1
            );
        }
        else if (message.hasJoin()) {
            validate(message.getJoin().hasName(), "No player name");
            return new JoinMessage(
                    message.getJoin().getName(),
                    message.getMsgSeq()
            );
        }
        else if (message.hasPing()) {
            return new PingMessage(
                    message.getMsgSeq(),
                    message.hasSenderId() ? message.getSenderId() : -1,
                    message.hasReceiverId() ? message.getReceiverId() : -1
            );
        }
        else if (message.hasRoleChange()) {
            validate(message.getRoleChange().hasSenderRole(), "No sender role");
            validate(message.getRoleChange().hasReceiverRole(), "No receiver role");
            validate(message.hasSenderId(), "No sender id");
            validate(message.hasReceiverId(), "No receiver id");
            return new RoleChangeMessage(
                    message.getRoleChange().getSenderRole(),
                    message.getRoleChange().getReceiverRole(),
                    message.getMsgSeq(),
                    message.hasSenderId() ? message.getSenderId() : -1,
                    message.hasReceiverId() ? message.getReceiverId() : -1
            );
        }
        else if (message.hasState()) {
            validate(message.getState().hasState(), "No state");

            GameState state = StateUtils.getStateFromMessage(message.getState().getState());
            validate(null != state, "Couldn't parse state from message");

            return new StateMessage(
                    state,
                    message.getMsgSeq(),
                    message.hasSenderId() ? message.getSenderId() : -1,
                    message.hasReceiverId() ? message.getReceiverId() : -1
            );
        }
        else if (message.hasSteer()) {
            validate(message.getSteer().hasDirection(), "No direction");
            return new SteerMessage(
                    message.getSteer().getDirection(),
                    message.getMsgSeq(),
                    message.hasSenderId() ? message.getSenderId() : -1,
                    message.hasReceiverId() ? message.getReceiverId() : -1
            );
        }
        else {
            logger.error("Can't deserialize message: No message");
            throw new SerializationException("No message");
        }
    }

    private void validate(boolean value, String errorMessage) {
        if (!value) {
            logger.error("Can't deserialize message: " + errorMessage);
            throw new SerializationException(errorMessage);
        }
    }
}
