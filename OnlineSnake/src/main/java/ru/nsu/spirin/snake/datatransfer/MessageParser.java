package ru.nsu.spirin.snake.datatransfer;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.experimental.UtilityClass;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage;
import org.apache.commons.lang3.SerializationException;
import org.apache.log4j.Logger;
import ru.nsu.spirin.snake.datatransfer.messages.AckMessage;
import ru.nsu.spirin.snake.datatransfer.messages.AnnouncementMessage;
import ru.nsu.spirin.snake.datatransfer.messages.ErrorMessage;
import ru.nsu.spirin.snake.datatransfer.messages.JoinMessage;
import ru.nsu.spirin.snake.datatransfer.messages.Message;
import ru.nsu.spirin.snake.datatransfer.messages.PingMessage;
import ru.nsu.spirin.snake.datatransfer.messages.RoleChangeMessage;
import ru.nsu.spirin.snake.datatransfer.messages.StateMessage;
import ru.nsu.spirin.snake.datatransfer.messages.SteerMessage;
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
        if (message.hasAck()) {
            return new AckMessage(
                    message.getMsgSeq(),
                    message.getSenderId(),
                    message.getReceiverId());
        }
        else if (message.hasAnnouncement()) {
            return new AnnouncementMessage(
                    message.getAnnouncement().getConfig(),
                    PlayerUtils.getPlayerList(message.getAnnouncement().getPlayers().getPlayersList()),
                    message.getAnnouncement().getCanJoin(),
                    message.getMsgSeq()
            );
        }
        else if (message.hasError()) {
            return new ErrorMessage(
                    message.getError().getErrorMessage(),
                    message.getMsgSeq()
            );
        }
        else if (message.hasJoin()) {
            return new JoinMessage(
                    message.getJoin().getName(),
                    message.getMsgSeq()
            );
        }
        else if (message.hasPing()) {
            return new PingMessage(message.getMsgSeq());
        }
        else if (message.hasRoleChange()) {
            return new RoleChangeMessage(
                    message.getRoleChange().getSenderRole(),
                    message.getRoleChange().getReceiverRole(),
                    message.getMsgSeq(),
                    message.getSenderId(),
                    message.getReceiverId()
            );
        }
        else if (message.hasState()) {
            return new StateMessage(
                    StateUtils.getStateFromMessage(message.getState().getState()),
                    message.getMsgSeq()
            );
        }
        else if (message.hasSteer()) {
            return new SteerMessage(
                    message.getSteer().getDirection(),
                    message.getMsgSeq()
            );
        }
        else {
            logger.error("Can't deserialize message : Unable to cast to Message");
            throw new SerializationException("Can't cast to Message");
        }
    }
}
