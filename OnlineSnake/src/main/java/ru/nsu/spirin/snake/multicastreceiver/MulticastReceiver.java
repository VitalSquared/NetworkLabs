package ru.nsu.spirin.snake.multicastreceiver;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.client.view.GameView;
import ru.nsu.spirin.snake.datatransfer.MessageParser;
import ru.nsu.spirin.snake.datatransfer.messages.Message;
import ru.nsu.spirin.snake.datatransfer.messages.MessageType;
import ru.nsu.spirin.snake.datatransfer.NetNode;
import ru.nsu.spirin.snake.datatransfer.messages.AnnouncementMessage;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class MulticastReceiver {
    private static final Logger logger = Logger.getLogger(MulticastReceiver.class);
    private static final int BUFFER_SIZE = 4096;
    private static final int SO_TIMEOUT_MS = 3000;

    private final @NotNull GameView view;
    private final @NotNull Map<GameInfo, Instant> gameInfos = new HashMap<>();
    private final InetSocketAddress multicastInfo;

    private final @NotNull Thread checkerThread;

    public MulticastReceiver(@NotNull InetSocketAddress multicastInfo, @NotNull GameView view) {
        validateAddress(Objects.requireNonNull(multicastInfo.getAddress()));
        this.multicastInfo = multicastInfo;
        this.view = view;
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
                socket.joinGroup(this.multicastInfo.getAddress());
                socket.setSoTimeout(SO_TIMEOUT_MS);
                byte[] buffer = new byte[BUFFER_SIZE];
                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket datagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);
                    boolean hasTimedOut = false;
                    try {
                        socket.receive(datagramPacket);
                    } catch (SocketTimeoutException e) {
                        hasTimedOut = true;
                    }
                    if (!hasTimedOut) {
                        NetNode sender = new NetNode(datagramPacket.getAddress(), datagramPacket.getPort());
                        Message message = MessageParser.deserializeMessage(datagramPacket);
                        if (message.getType() == MessageType.ANNOUNCEMENT) {
                            gameInfos.put(parseGameInfo(sender, (AnnouncementMessage) message), Instant.now());
                        }
                    }
                    gameInfos.entrySet().removeIf(entry -> Duration.between(entry.getValue(), Instant.now()).abs().toMillis() >= SO_TIMEOUT_MS);
                    view.updateGameList(gameInfos.keySet());
                }
                socket.leaveGroup(this.multicastInfo.getAddress());
            } catch (IOException e) {
                logger.error("Problem with multicast socket on port=" + this.multicastInfo.getPort(), e);
            }
        };
    }

    private GameInfo parseGameInfo(@NotNull NetNode sender, @NotNull AnnouncementMessage announcementMsg) {
        return new GameInfo(
                announcementMsg.getConfig(),
                sender,
                announcementMsg.getPlayers(),
                announcementMsg.isCanJoin()
        );
    }
}
