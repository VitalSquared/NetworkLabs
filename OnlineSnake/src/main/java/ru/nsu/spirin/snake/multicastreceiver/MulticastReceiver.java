package ru.nsu.spirin.snake.multicastreceiver;

import org.apache.log4j.Logger;
import ru.nsu.spirin.snake.client.network.GameNetwork;
import ru.nsu.spirin.snake.client.view.GameView;
import ru.nsu.spirin.snake.messages.MessageParser;
import ru.nsu.spirin.snake.messages.messages.Message;
import ru.nsu.spirin.snake.messages.messages.MessageType;
import ru.nsu.spirin.snake.datatransfer.NetNode;
import ru.nsu.spirin.snake.messages.messages.AnnouncementMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class MulticastReceiver {
    private static final Logger logger = Logger.getLogger(MulticastReceiver.class);
    private static final int BUFFER_SIZE = 4096;
    private static final int SO_TIMEOUT_MS = 3000;

    private final GameView view;
    private final GameNetwork network;
    private final InetSocketAddress multicastInfo;
    private final NetworkInterface networkInterface;
    private final Thread checkerThread;

    private final Map<GameInfo, Instant> gameInfos = new HashMap<>();

    public MulticastReceiver(InetSocketAddress multicastInfo, GameView view, GameNetwork network, NetworkInterface networkInterface) {
        validateAddress(multicastInfo.getAddress());
        this.multicastInfo = multicastInfo;
        this.networkInterface = networkInterface;
        this.view = view;
        this.network = network;
        this.checkerThread = new Thread(getCheckerRunnable());
    }

    private void validateAddress(InetAddress multicastAddress) {
        if (!multicastAddress.isMulticastAddress()) {
            throw new IllegalArgumentException(multicastAddress + " is not multicast");
        }
    }

    public void start() {
        this.checkerThread.start();
    }

    public void stop() {
        this.checkerThread.interrupt();
    }

    private Runnable getCheckerRunnable() {
        return () -> {
            try (MulticastSocket socket = new MulticastSocket(this.multicastInfo.getPort())) {
                byte[] buffer = new byte[BUFFER_SIZE];

                socket.joinGroup(this.multicastInfo, this.networkInterface);
                socket.setSoTimeout(SO_TIMEOUT_MS);

                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket datagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);
                    boolean hasTimedOut = false;

                    try {
                        socket.receive(datagramPacket);
                    }
                    catch (SocketTimeoutException exception) {
                        hasTimedOut = true;
                    }

                    if (!hasTimedOut) {
                        NetNode sender = new NetNode(datagramPacket.getAddress(), datagramPacket.getPort());
                        Message message = MessageParser.deserializeMessage(datagramPacket);
                        if (MessageType.ANNOUNCEMENT.equals(message.getType())) {
                            this.gameInfos.put(createGameInfo(sender, (AnnouncementMessage) message), Instant.now());
                        }
                    }

                    this.gameInfos.entrySet().removeIf(entry -> Duration.between(entry.getValue(), Instant.now()).abs().toMillis() >= SO_TIMEOUT_MS);
                    this.network.updateActiveGames(this.gameInfos.keySet());
                    this.view.updateGameList(this.gameInfos.keySet());
                }
                socket.leaveGroup(this.multicastInfo, this.networkInterface);
            } catch (IOException exception) {
                logger.error("Problem with multicast socket on port=" + this.multicastInfo.getPort(), exception);
            }
        };
    }

    private GameInfo createGameInfo(NetNode sender, AnnouncementMessage announcementMsg) {
        return new GameInfo(
                announcementMsg.getConfig(),
                sender,
                announcementMsg.getPlayers(),
                announcementMsg.isCanJoin()
        );
    }
}
