package ru.nsu.spirin.snake.server;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto.Direction;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import me.ippolitov.fit.snakes.SnakesProto.NodeRole;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.datatransfer.GameSocket;
import ru.nsu.spirin.snake.datatransfer.RDTSocket;
import ru.nsu.spirin.snake.gamehandler.GameHandler;
import ru.nsu.spirin.snake.gamehandler.game.Game;
import ru.nsu.spirin.snake.gamehandler.GameState;
import ru.nsu.spirin.snake.gamehandler.Player;
import ru.nsu.spirin.snake.datatransfer.NetNode;
import ru.nsu.spirin.snake.messages.MessageHandler;
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public final class ServerGame implements ServerHandler {
    private static final Logger logger = Logger.getLogger(ServerGame.class);
    public static final int ANNOUNCEMENT_SEND_PERIOD_MS = 1000;

    private final GameHandler game;
    private final GameConfig gameConfig;
    private final Map<Player, Direction> playersMoves = new ConcurrentHashMap<>();
    private final Map<Player, Instant> playersLastSeen = new ConcurrentHashMap<>();
    private final AtomicLong msgSeq = new AtomicLong(0);
    private final MessageHandler messageHandler = createMessageHandler();
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    private final @Getter RDTSocket socket;
    private final InetSocketAddress multicastAddress;

    private Future<?> activeDeputyTask = null;
    private Timer timer = new Timer();
    private Thread receiveThread = null;
    private Player masterPlayer = null;
    private Player deputyPlayer = null;
    private Player potentialDeputyPlayer = null;
    private boolean isDeputyBeingChosen = false;

    public ServerGame(GameConfig gameConfig, InetSocketAddress multicastAddress, InetAddress masterAddress, int masterPort, String masterName, NetworkInterface networkInterface) throws IOException {
        this.gameConfig = gameConfig;
        this.game = new Game(gameConfig, this);
        this.multicastAddress = multicastAddress;
        this.socket = new GameSocket(networkInterface, this.gameConfig.getPingDelayMs());
        this.socket.start();

        var playerOptional = registerNewPlayer(
                new NetNode(masterAddress, masterPort),
                masterName
        );
        playerOptional.ifPresent(player -> {
            player.setRole(NodeRole.MASTER);
            player.setScore(0);
            this.masterPlayer = player;
        });

        startTimerTasks();
        startReceivingMessages();
    }

    public ServerGame(GameState gameState, InetSocketAddress multicastAddress, NetworkInterface networkInterface) throws IOException {
        this.gameConfig = gameState.getGameConfig();
        this.game = new Game(gameState, this);
        this.multicastAddress = multicastAddress;
        this.socket = new GameSocket(networkInterface, this.gameConfig.getPingDelayMs());
        this.socket.start();

        gameState.getActivePlayers().forEach(player -> {
            player.setRole(NodeRole.MASTER.equals(player.getRole()) ? NodeRole.NORMAL : player.getRole());
            player.setRole(NodeRole.DEPUTY.equals(player.getRole()) ? NodeRole.MASTER : player.getRole());
            if (NodeRole.MASTER.equals(player.getRole())) {
                this.masterPlayer = player;
            }
            this.playersLastSeen.put(player, Instant.now());
            if (!NodeRole.VIEWER.equals(player.getRole())) {
                this.playersMoves.put(player, this.game.getSnakeByPlayer(player).getDirection());
            }
        });

        startTimerTasks();
        startReceivingMessages();
    }

    @Override
    public void update(GameState gameState) {
        var playerList = new ArrayList<>(this.playersLastSeen.keySet());
        this.playersLastSeen.keySet()
                .forEach(player -> this.socket.sendWithoutConfirm(
                            new StateMessage(gameState, playerList, this.msgSeq.getAndIncrement(), masterPlayer.getId(), player.getId()),
                            player.getNetNode()
                        )
                );
        if (gameState.getActivePlayers().isEmpty()) {
            this.stop();
        }
    }

    @Override
    public int getPort() {
        return this.socket.getLocalPort();
    }

    @Override
    public void stop() {
        if (this.deputyPlayer != null) {
            this.socket.sendWithoutConfirm(
                    new RoleChangeMessage(NodeRole.MASTER, NodeRole.MASTER, this.msgSeq.getAndIncrement(), this.masterPlayer.getId(), this.deputyPlayer.getId()),
                    this.deputyPlayer.getNetNode()
            );
        }
        this.timer.cancel();
        this.receiveThread.interrupt();
        this.socket.stop();
    }

    private void startTimerTasks() {
        this.timer.cancel();
        this.timer = new Timer();
        startGameUpdateTimer();
        startSendAnnouncementMessages();
        startRemovingDisconnectedPlayers();
    }

    private void startGameUpdateTimer() {
        this.timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        game.moveAllSnakes(Map.copyOf(playersMoves));
                        playersMoves.clear();
                    }
                },
                0, this.gameConfig.getStateDelayMs());
    }

    private void startSendAnnouncementMessages() {
        this.timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        socket.sendWithoutConfirm(
                                new AnnouncementMessage(gameConfig, new ArrayList<>(playersLastSeen.keySet()), true, msgSeq.getAndIncrement()),
                                new NetNode(multicastAddress.getAddress(), multicastAddress.getPort())
                        );
                    }
                },
                0, ANNOUNCEMENT_SEND_PERIOD_MS);
    }

    private void startRemovingDisconnectedPlayers() {
        this.timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        playersLastSeen.forEach((player, lastSeen) -> {
                            if (isDisconnected(lastSeen)) {
                                game.removePlayer(player);
                                playersMoves.remove(player);
                            }
                        });
                        playersLastSeen.entrySet().removeIf(entry -> isDisconnected(entry.getValue()));

                        if (potentialDeputyPlayer != null && !playersLastSeen.containsKey(potentialDeputyPlayer)) {
                            potentialDeputyPlayer = null;
                            isDeputyBeingChosen = false;
                            chooseNewDeputy();
                        }
                        if (deputyPlayer != null && !playersLastSeen.containsKey(deputyPlayer)) {
                            deputyPlayer = null;
                            chooseNewDeputy();
                        }
                    }
                },
                0,
                gameConfig.getPingDelayMs());
    }

    private void startReceivingMessages() {
        this.receiveThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                var recv = this.socket.receive();
                handleMessage(recv.getOwner(), recv.getMessage());
            }
        });
        this.receiveThread.start();
    }

    private void registerNewMove(Player player, Direction direction) {
        this.playersMoves.put(player, direction);
    }

    private void handleMessage(NetNode sender, Message message) {
        switch (message.getType()) {
            case ROLE_CHANGE -> this.messageHandler.handle(sender, (RoleChangeMessage) message);
            case STEER -> this.messageHandler.handle(sender, (SteerMessage) message);
            case JOIN -> this.messageHandler.handle(sender, (JoinMessage) message);
            case PING -> this.messageHandler.handle(sender, (PingMessage) message);
            case ERROR -> this.messageHandler.handle(sender, (ErrorMessage) message);
            default -> throw new IllegalStateException("Server: Cant handle this message type = " + message.getType());
        }
    }

    private void chooseNewDeputy() {
        if (this.activeDeputyTask != null) {
            this.activeDeputyTask.cancel(true);
        }
        this.isDeputyBeingChosen = true;
        this.activeDeputyTask = this.executorService.submit(() -> {
            Optional<Player> playerOptional = this.playersLastSeen.keySet().stream().filter(player -> player.getRole() != NodeRole.MASTER).findAny();
            playerOptional.ifPresentOrElse(
                    this::setDeputyPlayer,
                    () -> {
                        logger.warn("Cant chose deputy");
                        this.isDeputyBeingChosen = false;
                    }
            );
        });
    }

    private void setDeputyPlayer(@NotNull Player deputy) {
        this.potentialDeputyPlayer = deputy;
        Message response = this.socket.send(
                new RoleChangeMessage(NodeRole.MASTER, NodeRole.DEPUTY, this.msgSeq.getAndIncrement(), this.masterPlayer.getId(), deputy.getId()),
                deputy.getNetNode()
        );
        if (response instanceof AckMessage) {
            this.deputyPlayer = deputy;
            this.deputyPlayer.setRole(NodeRole.DEPUTY);
        }
        this.potentialDeputyPlayer = null;
        this.isDeputyBeingChosen = false;
    }

    private boolean isDisconnected(@NotNull Instant moment) {
        return Duration.between(moment, Instant.now()).abs().toMillis() >= gameConfig.getNodeTimeoutMs();
    }

    private boolean validateNewPlayer(NetNode sender, JoinMessage joinMsg) {
        if (this.playersLastSeen.keySet().stream().anyMatch(player -> player.getName().equals(joinMsg.getPlayerName()))) {
            logger.error("Node=" + sender + " already registered as player");
            this.socket.sendWithoutConfirm(
                    new ErrorMessage("Player already exist", joinMsg.getMessageSequence(), masterPlayer.getId(), -1),
                    sender
            );
            return false;
        }
        return true;
    }

    private Optional<Player> registerNewPlayer(NetNode netNode, String playerName) {
        try {
            Player player = this.game.registerNewPlayer(playerName, netNode);
            this.playersLastSeen.put(player, Instant.now());
            this.playersMoves.put(player, this.game.getSnakeByPlayer(player).getDirection());
            return Optional.of(player);
        }
        catch (IllegalStateException exception) {
            logger.debug("Cant place player on field because no space");
            this.socket.sendWithoutConfirm(
                    new ErrorMessage("Cant place player on field because no space", this.msgSeq.getAndIncrement(),
                            masterPlayer.getId(), -1),
                    netNode
            );
            return Optional.empty();
        }
    }

    private void removePlayer(NetNode sender) {
        checkRegistration(sender);
        Player player = PlayerUtils.findPlayerByAddress(sender, this.playersLastSeen.keySet());
        if (null == player) {
            return;
        }

        updateLastSeen(sender);
        this.playersMoves.remove(player);
        this.game.removePlayer(player);
    }

    private void checkRegistration(@NotNull NetNode sender) {
        if (null == PlayerUtils.findPlayerByAddress(sender, this.playersLastSeen.keySet())) {
            logger.error("Node=" + sender + " is not registered");
            throw new IllegalArgumentException("Node={" + sender + "} is not registered");
        }
    }

    private void updateLastSeen(@NotNull NetNode sender) {
        Player player = PlayerUtils.findPlayerByAddress(sender, this.playersLastSeen.keySet());
        if (null != player) {
            this.playersLastSeen.put(player, Instant.now());
        }
    }

    private MessageHandler createMessageHandler() {
        return new MessageHandler() {
            @Override
            public void handle(NetNode sender, SteerMessage message) {
                checkRegistration(sender);
                updateLastSeen(sender);
                Player player = PlayerUtils.findPlayerByAddress(sender, playersLastSeen.keySet());
                if (null == player) {
                    return;
                }

                registerNewMove(player, message.getDirection());
                logger.debug("NetNode=" + sender + " as player=" + player + " make move with direction=" + message.getDirection());
            }

            @Override
            public void handle(NetNode sender, JoinMessage message) {
                if (!validateNewPlayer(sender, message)) {
                    return;
                }
                registerNewPlayer(sender, message.getPlayerName())
                        .ifPresent(player -> {
                                    logger.debug("NetNode=" + sender + " was successfully registered as player=" + player);
                                    player.setRole(NodeRole.NORMAL);
                                    player.setScore(0);
                                    if (null == deputyPlayer && !isDeputyBeingChosen) {
                                        chooseNewDeputy();
                                    }
                                }
                        );
            }

            @Override
            public void handle(NetNode sender, PingMessage message) {
                updateLastSeen(sender);
            }

            @Override
            public void handle(NetNode sender, StateMessage message) {
                throw new IllegalStateException("Server shouldn't receive State messages");
            }

            @Override
            public void handle(NetNode sender, ErrorMessage message) {
                logger.error(message.getErrorMessage());
            }

            @Override
            public void handle(NetNode sender, RoleChangeMessage message) {
                if (NodeRole.VIEWER.equals(message.getSenderRole()) && NodeRole.MASTER.equals(message.getReceiverRole())) {
                    removePlayer(sender);
                }
                else {
                    logger.warn("Server: Unsupported roles at role change message=" + message + " from=" + sender);
                    throw new IllegalArgumentException("Unsupported roles at role change message=" + message + " from=" + sender);
                }
            }
        };
    }
}
