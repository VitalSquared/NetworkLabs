package ru.nsu.spirin.snake.client.network;

import lombok.RequiredArgsConstructor;
import me.ippolitov.fit.snakes.SnakesProto.Direction;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import me.ippolitov.fit.snakes.SnakesProto.NodeRole;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.client.view.GameView;
import ru.nsu.spirin.snake.datatransfer.GameSocket;
import ru.nsu.spirin.snake.datatransfer.RDTSocket;
import ru.nsu.spirin.snake.gamehandler.Player;
import ru.nsu.spirin.snake.messages.MessageHandler;
import ru.nsu.spirin.snake.messages.messages.ErrorMessage;
import ru.nsu.spirin.snake.messages.messages.MessageType;
import ru.nsu.spirin.snake.messages.messages.RoleChangeMessage;
import ru.nsu.spirin.snake.messages.messages.StateMessage;
import ru.nsu.spirin.snake.messages.messages.SteerMessage;
import ru.nsu.spirin.snake.gamehandler.GameState;
import ru.nsu.spirin.snake.datatransfer.NetNode;
import ru.nsu.spirin.snake.messages.messages.JoinMessage;
import ru.nsu.spirin.snake.messages.messages.Message;
import ru.nsu.spirin.snake.messages.messages.PingMessage;
import ru.nsu.spirin.snake.server.ServerGame;
import ru.nsu.spirin.snake.server.ServerHandler;
import ru.nsu.spirin.snake.utils.StateUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public final class GameNetwork implements NetworkHandler {
    private static final Logger logger = Logger.getLogger(GameNetwork.class);

    private final RDTSocket rdtSocket;
    private final GameConfig config;
    private final String playerName;
    private final GameView view;
    private final InetSocketAddress multicastInfo;
    private final NetworkInterface networkInterface;

    private ServerHandler activeServerGame = null;
    private GameState gameState;
    private NodeRole nodeRole;
    private NetNode master;
    private NetNode deputy;
    private Instant masterLastSeen = Instant.now();

    private int masterID = -1;
    private int deputyID = -1;
    private int playerID = -1;

    private final AtomicLong msgSeq = new AtomicLong(0);
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final MessageHandler messageHandler = createMessageHandler();
    private Future<?> activeJoinTask = null;
    private Timer timer = new Timer();

    private long currentSteerMessageSeq = -1;
    private Direction curDirection = null;
    private Thread receiveThread = null;

    public GameNetwork(GameConfig config, String playerName, GameView view, InetSocketAddress multicastInfo, NetworkInterface networkInterface) throws IOException {
        this.rdtSocket = new GameSocket(networkInterface, config.getPingDelayMs());
        this.config = config;
        this.view = view;
        this.multicastInfo = multicastInfo;
        this.networkInterface = networkInterface;
        this.playerName = playerName;
    }

    @Override
    public void startNewGame() {
        stopCurrentServerGame();
        rdtSocket.stop();
        rdtSocket.start();
        try {
            this.activeServerGame = new ServerGame(
                    this.config,
                    this.multicastInfo,
                    InetAddress.getLocalHost(),
                    this.rdtSocket.getLocalPort(),
                    this.playerName,
                    this.networkInterface
            );
            this.master = new NetNode(InetAddress.getLocalHost(), this.activeServerGame.getPort());
            this.masterLastSeen = Instant.now();
            this.gameState = null;
            changeNodeRole(NodeRole.MASTER);

            startTimerTasks();
        }
        catch (IOException exception) {
            logger.error(exception.getLocalizedMessage());
            stopCurrentServerGame();
        }
    }

    @Override
    public void joinToGame(@NotNull NetNode gameOwner, @NotNull String playerName) {
        exit();
        rdtSocket.start();
        if (null != this.activeJoinTask) {
            this.activeJoinTask.cancel(true);
        }
        this.activeJoinTask = this.executorService.submit(() -> {
            Message response = this.rdtSocket.send(new JoinMessage(playerName, this.msgSeq.get()), gameOwner);
            if (null == response) {
                return;
            }
            if (MessageType.ERROR.equals(response.getType())) {
                this.messageHandler.handle(null, (ErrorMessage) response);
                return;
            }
            if (!MessageType.ACK.equals(response.getType())) {
                logger.error("For join message, Server didn't respond with Ack message");
                return;
            }

            this.masterID = response.getSenderID();
            this.playerID = response.getReceiverID();
            this.master = gameOwner;
            this.masterLastSeen = Instant.now();
            this.deputy = null;
            this.gameState = null;
            changeNodeRole(NodeRole.NORMAL);

            startTimerTasks();
        });
    }

    @Override
    public void handleMove(Direction direction) {
        if (!NodeRole.VIEWER.equals(this.nodeRole)) {
            this.rdtSocket.removePendingMessage(currentSteerMessageSeq);
            this.currentSteerMessageSeq = msgSeq.getAndIncrement();
            this.curDirection = direction;
            this.rdtSocket.sendNonBlocking(new SteerMessage(direction, currentSteerMessageSeq, playerID, masterID), master);
        }
    }

    @Override
    public void exit() {
        if (null != this.master) {
            this.rdtSocket.sendWithoutConfirm(
                    new RoleChangeMessage(NodeRole.VIEWER, NodeRole.MASTER, this.msgSeq.getAndIncrement(), this.playerID, this.masterID),
                    this.master
            );
        }
        this.nodeRole = NodeRole.VIEWER;
        this.rdtSocket.stop();
        this.timer.cancel();
        if (null != this.receiveThread) {
            this.receiveThread.interrupt();
            this.receiveThread = null;
        }
        stopCurrentServerGame();
    }

    private void stopCurrentServerGame() {
        if (null != this.activeServerGame) {
            this.activeServerGame.stop();
        }
        this.activeServerGame = null;
        this.masterID = -1;
        this.playerID = -1;
        this.deputyID = -1;
        this.master = null;
        this.deputy = null;
        this.gameState = null;
    }

    private void startTimerTasks() {
        this.timer.cancel();
        this.timer = new Timer();
        startSendPingMessages();
        startHandleReceivedMessages();
        startMasterCheck();
    }

    private void startHandleReceivedMessages() {
        this.receiveThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                var recv = rdtSocket.receive();
                handleMessage(recv.getOwner(), recv.getMessage());
            }
        });
        this.receiveThread.start();
    }

    private void startSendPingMessages() {
        this.timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        if (null != master) {
                            rdtSocket.sendWithoutConfirm(new PingMessage(msgSeq.getAndIncrement(), playerID, masterID), master);
                        }
                    }
                },
                0,
                this.config.getPingDelayMs()
        );
    }

    private void startMasterCheck() {
        this.timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        if (master != null && Duration.between(masterLastSeen, Instant.now()).abs().toMillis() > config.getStateDelayMs()) {
                            if (nodeRole == NodeRole.DEPUTY) {
                                swapToMaster();
                            }
                            else if (nodeRole == NodeRole.NORMAL) {
                                master = deputy;
                                deputy = null;
                                masterID = deputyID;
                                deputyID = -1;
                                masterLastSeen = Instant.now();
                            }
                            if (curDirection != null) {
                                handleMove(curDirection);
                            }
                        }
                    }
                },
                0,
                this.config.getNodeTimeoutMs()
        );
    }

    private void handleMessage(@NotNull NetNode sender, @NotNull Message message) {
        switch (message.getType()) {
            case ROLE_CHANGE -> messageHandler.handle(sender, (RoleChangeMessage) message);
            case ERROR -> messageHandler.handle(sender, (ErrorMessage) message);
            case STATE -> messageHandler.handle(sender, (StateMessage) message);
            default -> throw new IllegalStateException("Cant handle this message type = " + message.getType());
        }
    }

    private void swapToMaster()  {
        try {
            this.activeServerGame = new ServerGame(this.gameState, this.multicastInfo, this.networkInterface);
            this.master = new NetNode(InetAddress.getLocalHost(), this.activeServerGame.getPort());
            this.deputy = null;
            this.masterID = this.playerID;
            this.deputyID = -1;
            changeNodeRole(NodeRole.MASTER);
            this.gameState.getActivePlayers().forEach(player -> {
                        if (NodeRole.NORMAL.equals(player.getRole())) {
                            this.rdtSocket.sendNonBlocking(
                                    new RoleChangeMessage(
                                            NodeRole.DEPUTY,
                                            NodeRole.NORMAL,
                                            this.msgSeq.getAndIncrement(),
                                            this.playerID,
                                            player.getId()
                                    ),
                                    player.getNetNode()
                            );
                        }
                    }
            );
        }
        catch (IOException exception) {
            logger.error(exception.getLocalizedMessage());
        }
    }

    private void changeNodeRole(@NotNull NodeRole nodeRole) {
        if (null == this.config) {
            logger.error("Cant change role=" + this.nodeRole + " to " + nodeRole + " without config");
            exit();
            throw new IllegalStateException("Cant change role without config");
        }
        this.nodeRole = nodeRole;
        logger.info("Client: I am " + this.nodeRole);
    }

    private void lose() {
        System.out.println("Lose");
    }

    private MessageHandler createMessageHandler() {
        return new MessageHandler() {
            @Override
            public void handle(NetNode sender, SteerMessage message) {
                logger.error("Client shouldn't receive Steer messages");
            }

            @Override
            public void handle(NetNode sender, JoinMessage message) {
                logger.error("Client shouldn't receive Join messages");
            }

            @Override
            public void handle(NetNode sender, PingMessage message) {
                logger.error("Client shouldn't receive Ping messages");
            }

            @Override
            public void handle(NetNode sender, StateMessage stateMsg) {
                if (master != null && !sender.equals(master)) {
                    logger.info("Received state from somewhere else");
                    return;
                }

                GameState newState = stateMsg.getGameState();
                if (gameState != null && gameState.getStateID() >= newState.getStateID()) {
                    logger.warn("Received state with id=" + newState.getStateID() + " less then last gamehandler state id=" + gameState.getStateID());
                    return;
                }

                master = sender;
                masterLastSeen = Instant.now();
                Player deputyPlayer = StateUtils.getDeputyFromState(newState);
                if (deputyPlayer == null) {
                    deputy = null;
                    deputyID = -1;
                }
                else {
                    deputy = deputyPlayer.getNetNode();
                    deputyID = deputyPlayer.getId();
                }
                gameState = newState;
                view.updateCurrentGame(newState);
            }

            @Override
            public void handle(NetNode sender, ErrorMessage message) {
                logger.error("ERROR=" + message.getErrorMessage());
            }

            @Override
            public void handle(NetNode sender, RoleChangeMessage roleChangeMsg) {
                switch (nodeRole) {
                    case MASTER -> logger.info("MASTER"); //shouldn't happen
                    case DEPUTY -> {
                        if (roleChangeMsg.getSenderRole() == NodeRole.MASTER && roleChangeMsg.getReceiverRole() == NodeRole.MASTER) {
                            swapToMaster();
                            deputy = null;
                        }
                        else if (roleChangeMsg.getSenderRole() == NodeRole.MASTER &&
                                 roleChangeMsg.getReceiverRole() == NodeRole.VIEWER) {
                            lose();
                        }
                    }
                    case NORMAL -> {
                        if (roleChangeMsg.getSenderRole() == NodeRole.MASTER && roleChangeMsg.getReceiverRole() == NodeRole.DEPUTY) {
                            changeNodeRole(NodeRole.DEPUTY);
                            masterLastSeen = Instant.now();
                        }
                        else if (roleChangeMsg.getSenderRole() == NodeRole.DEPUTY &&
                                 roleChangeMsg.getReceiverRole() == NodeRole.NORMAL) {
                            master = deputy;
                            masterLastSeen = Instant.now();
                            deputy = null;
                        }
                        else if (roleChangeMsg.getSenderRole() == NodeRole.MASTER &&
                                 roleChangeMsg.getReceiverRole() == NodeRole.VIEWER) {
                            lose();
                        }
                    }
                    case VIEWER -> logger.info("VIEWER");
                }
            }
        };
    }
}
