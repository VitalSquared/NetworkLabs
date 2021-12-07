package ru.nsu.spirin.snake.client.network;

import lombok.RequiredArgsConstructor;
import me.ippolitov.fit.snakes.SnakesProto.Direction;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import me.ippolitov.fit.snakes.SnakesProto.NodeRole;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.client.view.GameView;
import ru.nsu.spirin.snake.datatransfer.RDTSocket;
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
import ru.nsu.spirin.snake.utils.NetUtils;
import ru.nsu.spirin.snake.utils.StateUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
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

    private ServerHandler activeServerGame = null;
    private GameState gameState;
    private NodeRole nodeRole;
    private NetNode master;
    private NetNode deputy;
    private Instant masterLastSeen = Instant.now();

    private int masterID = -1;
    private int playerID = -1;

    private final AtomicLong msgSeq = new AtomicLong(0);
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final MessageHandler messageHandler = createMessageHandler();
    private Future<?> activeJoinTask = null;
    private Timer timer = new Timer();
    private Timer roleChangeTimer = new Timer();

    private long currentSteerMessageSeq = -1;
    private Direction curDirection = null;

    @Override
    public void startNewGame() {
        stopCurrentServerGame();
        rdtSocket.stop();
        rdtSocket.start();
        try {
            this.activeServerGame = new ServerGame(
                    this.config,
                    this.multicastInfo,
                    this.rdtSocket.getLocalPort(),
                    this.playerName
            );
            this.master = new NetNode(InetAddress.getByName("localhost"), this.activeServerGame.getPort());
            this.masterLastSeen = Instant.now();
            this.gameState = null;
            changeNodeRole(NodeRole.MASTER);

            startTimerTasks();
        }
        catch (SocketException | UnknownHostException exception) {
            logger.error(exception.getLocalizedMessage());
            stopCurrentServerGame();
        }
    }

    @Override
    public void joinToGame(@NotNull NetNode gameOwner, @NotNull String playerName) {
        stopCurrentServerGame();
        rdtSocket.stop();
        rdtSocket.start();
        if (null != this.activeJoinTask) {
            this.activeJoinTask.cancel(true);
        }
        this.activeJoinTask = this.executorService.submit(() -> {
            Message ackMessage = rdtSocket.send(new JoinMessage(playerName, msgSeq.get()), gameOwner);
            if (ackMessage == null) {
                return;
            }
            if (ackMessage.getType() == MessageType.ERROR) {
                messageHandler.handle(null, (ErrorMessage) ackMessage);
                return;
            }
            if (ackMessage.getType() != MessageType.ACK) {
                logger.error("NOT AN ACK MESSAGE");
                return;
            }

            this.masterID = ackMessage.getSenderID();
            this.playerID = ackMessage.getReceiverID();
            this.master = gameOwner;
            this.masterLastSeen = Instant.now();
            this.gameState = null;
            changeNodeRole(NodeRole.NORMAL);

            startTimerTasks();
        });
    }

    @Override
    public void handleMove(@NotNull Direction direction) {
        this.rdtSocket.removePendingMessages(currentSteerMessageSeq);
        this.currentSteerMessageSeq = msgSeq.getAndIncrement();
        this.curDirection = direction;
        this.rdtSocket.sendNonBlocking(new SteerMessage(direction, currentSteerMessageSeq), master);
    }

    @Override
    public void exit() {
        if (master != null) {
            rdtSocket.sendWithoutConfirm(
                    new RoleChangeMessage(NodeRole.VIEWER, NodeRole.MASTER, msgSeq.getAndIncrement(), playerID, masterID),
                    master
            );
        }
        stopCurrentServerGame();
        this.timer.cancel();
        this.master = null;
        this.gameState = null;
        this.rdtSocket.stop();
    }

    private void stopCurrentServerGame() {
        if (null != this.activeServerGame) {
            this.activeServerGame.stop();
        }
        this.activeServerGame = null;
    }

    private void startTimerTasks() {
        this.timer.cancel();
        this.timer = new Timer();
        startSendPingMessages();
        startHandleReceivedMessages();
        startMasterCheck();
    }

    private void startHandleReceivedMessages() {
        this.timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        var recv = rdtSocket.receive();
                        handleMessage(recv.getOwner(), recv.getMessage());
                    }
                },
            0,
            100
        );
    }

    private void startSendPingMessages() {
        this.timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        if (master != null) {
                            rdtSocket.sendWithoutConfirm(new PingMessage(msgSeq.getAndIncrement()), master);
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
            this.activeServerGame = new ServerGame(gameState, multicastInfo);
            this.master = new NetNode(NetUtils.getLocalhostAddress(), activeServerGame.getPort());
            this.deputy = null;
            changeNodeRole(NodeRole.MASTER);
            roleChangeTimer.cancel();
            roleChangeTimer = new Timer();
            gameState.getActivePlayers().forEach(player -> {
                        if (player.getRole() == NodeRole.DEPUTY) {
                            return;
                        }
                        rdtSocket.sendNonBlocking(
                                new RoleChangeMessage(NodeRole.DEPUTY, NodeRole.NORMAL, msgSeq.getAndIncrement(), playerID,
                                        player.getId()), player.getNetNode());
                    }
            );
        }
        catch (SocketException exception) {
            logger.error(exception.getLocalizedMessage());
        }
    }

    private void changeNodeRole(@NotNull NodeRole nodeRole) {
        if (config == null) {
            logger.error("Cant change role=" + this.nodeRole + " to " + nodeRole + " without config");
            exit();
            throw new IllegalStateException("Cant change role without config");
        }
        this.nodeRole = nodeRole;
        logger.info("I am " + this.nodeRole);
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
                if (!sender.equals(master)) {
                    logger.info("Received state from somewhere else");
                    return;
                }

                GameState newState = stateMsg.getGameState();
                if (gameState != null && gameState.getStateID() >= newState.getStateID()) {
                    logger.warn("Received state with id=" + newState.getStateID() + " less then last gamehandler state id=" + gameState.getStateID());
                    return;
                }

                masterLastSeen = Instant.now();
                deputy = StateUtils.getDeputyFromState(newState);
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
