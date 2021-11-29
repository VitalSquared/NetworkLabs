package ru.nsu.spirin.snake.server;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto.Direction;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import me.ippolitov.fit.snakes.SnakesProto.NodeRole;
import org.apache.commons.lang3.SerializationException;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.datatransfer.MessageParser;
import ru.nsu.spirin.snake.datatransfer.RDTSocket;
import ru.nsu.spirin.snake.datatransfer.messages.MessageType;
import ru.nsu.spirin.snake.game.Game;
import ru.nsu.spirin.snake.game.GameObserver;
import ru.nsu.spirin.snake.game.GameState;
import ru.nsu.spirin.snake.game.Player;
import ru.nsu.spirin.snake.datatransfer.NetNode;
import ru.nsu.spirin.snake.datatransfer.messages.AckMessage;
import ru.nsu.spirin.snake.datatransfer.messages.AnnouncementMessage;
import ru.nsu.spirin.snake.datatransfer.messages.ErrorMessage;
import ru.nsu.spirin.snake.datatransfer.messages.JoinMessage;
import ru.nsu.spirin.snake.datatransfer.messages.Message;
import ru.nsu.spirin.snake.datatransfer.messages.PingMessage;
import ru.nsu.spirin.snake.datatransfer.messages.RoleChangeMessage;
import ru.nsu.spirin.snake.datatransfer.messages.StateMessage;
import ru.nsu.spirin.snake.datatransfer.messages.SteerMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ServerGame implements GameObserver {
    private static final Logger logger = Logger.getLogger(ServerGame.class);
    public static final int ANNOUNCEMENT_SEND_PERIOD = 1000;

    private final Game game;
    private final @NotNull GameConfig gameConfig;
    private final Map<Player, Direction> playersMoves = new ConcurrentHashMap<>();
    private final Map<Player, Instant> playersLastSeen = new ConcurrentHashMap<>();
    private final Timer timer = new Timer();

    private final DatagramSocket datagramSocket;
    private final @Getter RDTSocket socket;
    private final InetAddress multicastAddress;
    private final int multicastPort;

    private Thread receiveThread = null;

    private final AtomicLong msgSeq = new AtomicLong(0);

    private Player master;
    private Player deputy;

    public ServerGame(@NotNull GameConfig gameConfig, InetAddress multicastAddress, int multicastPort, int masterPort, String masterName) throws SocketException {
        this.gameConfig = Objects.requireNonNull(gameConfig);
        this.game = new Game(gameConfig);
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.datagramSocket = new DatagramSocket();
        this.socket = new RDTSocket(this.datagramSocket, gameConfig.getNodeTimeoutMs());
        try {
            var player = registerNewPlayer(new NetNode(InetAddress.getByName("localhost"), masterPort), masterName);
            player.ifPresent(player1 -> {
                player1.setRole(NodeRole.MASTER);
                player1.setScore(0);
                this.master = player1;
            });
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }
        startGameUpdateTimer();
        startSendAnnouncementMessages();
        startRemovingDisconnectedPlayers();
        startReceivingMessages();
        this.game.addObserver(this);
    }

    public ServerGame(@NotNull GameConfig config, @NotNull GameState gameState, InetAddress multicastAddress, int multicastPort) throws SocketException {
        this.gameConfig = Objects.requireNonNull(config);
        this.game = new Game(gameState);
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.datagramSocket = new DatagramSocket();
        this.socket = new RDTSocket(this.datagramSocket, gameConfig.getNodeTimeoutMs());
        final Player[] master = { null };
        final Player[] deputy = { null };
        gameState.getActivePlayers().forEach(player -> {
            Optional<Player> player1 = registerNewPlayer(player.getNetNode(), player.getName());
            if (player1.isPresent()) {
                Player player2 = player1.get();
                player2.setRole(player.getRole());
                player2.setScore(player.getScore());
                if (player2.getRole() == NodeRole.DEPUTY) {
                    deputy[0] = player2;
                }
                else if (player2.getRole() == NodeRole.MASTER) {
                    master[0] = player2;
                }
            }
        });
        if (master[0] == null) {
            this.master = deputy[0];
            deputy[0].setRole(NodeRole.MASTER);
        }
        startGameUpdateTimer();
        startSendAnnouncementMessages();
        startRemovingDisconnectedPlayers();
        this.game.addObserver(this);
    }

    private void startSendAnnouncementMessages() {
        TimerTask announcementSendTask = new TimerTask() {
            @Override
            public void run() {
                sendMessageWithoutConfirmation(
                        new NetNode(multicastAddress, multicastPort),
                        new AnnouncementMessage(gameConfig, new ArrayList<>(playersLastSeen.keySet()), true, msgSeq.getAndIncrement())
                );
            }
        };
        timer.schedule(announcementSendTask, 0, ANNOUNCEMENT_SEND_PERIOD);
    }

    private void startReceivingMessages() {
        receiveThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                var recv = socket.receive();
                handleMessage(recv.getValue(), recv.getKey());
            }
        });
        receiveThread.start();
    }

    private void registerNewMove(@NotNull Player player, @NotNull Direction direction) {
        playersMoves.put(
                Objects.requireNonNull(player),
                Objects.requireNonNull(direction)
        );
    }

    private void startGameUpdateTimer() {
        TimerTask gameUpdateTask = new TimerTask() {
            @Override
            public void run() {
                game.makeAllPlayersMove(Map.copyOf(playersMoves));
                playersMoves.clear();
            }
        };
        timer.schedule(gameUpdateTask, 0, gameConfig.getStateDelayMs());
    }

    public void update(@NotNull GameState gameState) {
        playersLastSeen.keySet()
                .forEach(player -> sendMessageWithoutConfirmation(
                        player.getNetNode(),
                        new StateMessage(gameState, new ArrayList<>(playersLastSeen.keySet()), msgSeq.getAndIncrement())
                ));
    }

    public void handleMessage(@NotNull NetNode sender, @NotNull Message message) {
        Objects.requireNonNull(message, "Message cant be null");
        Objects.requireNonNull(sender, "Sender cant be null");
        switch (message.getType()) {
            case ROLE_CHANGE -> handle(sender, (RoleChangeMessage) message);
            case STEER -> handle(sender, (SteerMessage) message);
            case JOIN -> handle(sender, (JoinMessage) message);
            case PING -> handle(sender, (PingMessage) message);
            default -> throw new IllegalStateException("Cant handle this message type = " + message.getType());
        }
    }

    public void startRemovingDisconnectedPlayers() {
        TimerTask removeTask = getRemoveDisconnectedUsersTask();
        timer.schedule(removeTask, 0, gameConfig.getPingDelayMs());
    }

    @NotNull
    private TimerTask getRemoveDisconnectedUsersTask() {
        return new TimerTask() {
            @Override
            public void run() {
                playersLastSeen.forEach((player, lastSeen) -> {
                    if (isDisconnected(lastSeen)) {
                        game.removePlayer(player);
                        playersMoves.remove(player);
                    }
                });
                playersLastSeen.entrySet().removeIf(entry -> isDisconnected(entry.getValue()));
                if (playersLastSeen.entrySet().size() == 0) {
                    stop();
                }
                if (deputy != null && !playersLastSeen.containsKey(deputy)) {
                    deputy = null;
                    chooseNewDeputy();
                }
            }
        };
    }

    private void chooseNewDeputy() {
        Optional<Player> neighborOpt = playersLastSeen.keySet().stream().findAny();
        neighborOpt.ifPresentOrElse(
                this::setDeputy,
                () -> logger.warn("Cant chose deputy")
        );
    }

    private void setDeputy(@NotNull Player deputy) {
        sendMessage(deputy.getNetNode(), new RoleChangeMessage(NodeRole.MASTER, NodeRole.DEPUTY, msgSeq.getAndIncrement(), master.getId(), deputy.getId()))
                .thenAccept(message -> {
                    if (message.getType() == MessageType.ACK) {
                        this.deputy = deputy;
                    }
                    else {
                        logger.error("Potential deputy didn't send ACK message");
                    }
                });
    }

    private boolean isDisconnected(@NotNull Instant moment) {
        return Duration.between(moment, Instant.now()).abs().toMillis() >= gameConfig.getNodeTimeoutMs();
    }

    private void handle(@NotNull NetNode sender, @NotNull JoinMessage joinMsg) {
        if (!validateNewPlayer(sender, joinMsg.getPlayerName())) {
            return;
        }
        registerNewPlayer(sender, joinMsg.getPlayerName())
                .ifPresent(player -> {
                            logger.debug("NetNode=" + sender + " was successfully registered as player=" + player);
                            player.setRole(NodeRole.NORMAL);
                            player.setScore(0);
                            sendMessageWithoutConfirmation(player.getNetNode(), new AckMessage(joinMsg.getMessageSequence(), master.getId(), player.getId()));
                        }
                );
    }

    private boolean validateNewPlayer(@NotNull NetNode sender, @NotNull String playerName) {
        if (playersLastSeen.keySet().stream().anyMatch(player -> player.getName().equals(playerName))) {
            logger.error("Node=" + sender + " already registered as player");
            sendMessageWithoutConfirmation(sender, new ErrorMessage("Player already exist", msgSeq.getAndIncrement()));
            return false;
        }
        return true;
    }

    @NotNull
    private Optional<Player> registerNewPlayer(@NotNull NetNode netNode, @NotNull String playerName) {
        try {
            Player player = game.registrationNewPlayer(playerName, netNode);
            playersLastSeen.put(player, Instant.now());
            playersMoves.put(player, game.getPlayersWithSnakes().get(player).getDirection());
            return Optional.of(player);
        } catch (IllegalStateException e) {
            logger.debug("Cant place player on field because no space");
            sendMessageWithoutConfirmation(
                    netNode,
                    new ErrorMessage("Cant place player on field because no space", msgSeq.getAndIncrement())
            );
            return Optional.empty();
        }
    }

    private void handle(@NotNull NetNode sender, @NotNull RoleChangeMessage roleChangeMsg) {
        if (roleChangeMsg.getSenderRole() == NodeRole.VIEWER && roleChangeMsg.getReceiverRole() == NodeRole.MASTER) {
            removePlayer(sender);
        } else {
            logger.warn("Unsupported roles at role change message=" + roleChangeMsg + " from=" + sender);
            throw new IllegalArgumentException("Unsupported roles at role change message=" + roleChangeMsg + " from=" + sender);
        }
    }

    private void removePlayer(NetNode sender) {
        checkRegistration(sender);
        Player player = getPlayerByAddress(sender);
        if (player == null) {
            return;
        }
        playersLastSeen.remove(player);
        playersMoves.remove(player);
        game.removePlayer(player);
    }

    private void checkRegistration(@NotNull NetNode sender) {
        if (!playersLastSeen.containsKey(getPlayerByAddress(sender))) {
            logger.error("Node=" + sender + " is not registered");
            throw new IllegalArgumentException("Node={" + sender + "} is not registered");
        }
    }

    private void handle(@NotNull NetNode sender, @NotNull SteerMessage steerMsg) {
        checkRegistration(sender);
        updateLastSeen(sender);
        Player senderAsPlayer = getPlayerByAddress(sender);
        if (senderAsPlayer == null) {
            return;
        }
        registerNewMove(senderAsPlayer, steerMsg.getDirection());
        logger.debug("NetNode=" + sender + " as player=" + senderAsPlayer + " make move with direction=" + steerMsg.getDirection());
    }

    private void handle(@NotNull NetNode sender, @NotNull PingMessage pingMessageHandler) {
        updateLastSeen(sender);
    }

    private void updateLastSeen(@NotNull NetNode sender) {
        Player player = getPlayerByAddress(sender);
        if (player == null) {
            return;
        }
        playersLastSeen.put(player, Instant.now());
    }

    public void stop() {
        if (deputy != null) {
            sendMessage(deputy.getNetNode(), new RoleChangeMessage(NodeRole.MASTER, NodeRole.MASTER, msgSeq.getAndIncrement(), master.getId(), deputy.getId()));
        }
        timer.cancel();
        receiveThread.interrupt();
    }

    private void sendMessageWithoutConfirmation(NetNode node, Message message)  {
        try {
            socket.sendWithoutConfirm(MessageParser.serializeMessage(message, node.getAddress(), node.getPort()));
        }
        catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private CompletableFuture<Message> sendMessage(NetNode node, Message message) {
        return socket.send(message, node.getAddress(), node.getPort());
    }

    private Player getPlayerByAddress(NetNode node) {
        for (var player : playersLastSeen.keySet()) {
            if (player.getNetNode().equals(node)) {
                return player;
            }
        }
        return null;
    }
}
