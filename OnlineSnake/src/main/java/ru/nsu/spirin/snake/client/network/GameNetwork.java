package ru.nsu.spirin.snake.client.network;

import lombok.RequiredArgsConstructor;
import me.ippolitov.fit.snakes.SnakesProto.Direction;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import me.ippolitov.fit.snakes.SnakesProto.NodeRole;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.client.view.GameView;
import ru.nsu.spirin.snake.datatransfer.MessageParser;
import ru.nsu.spirin.snake.datatransfer.RDTSocket;
import ru.nsu.spirin.snake.datatransfer.messages.ErrorMessage;
import ru.nsu.spirin.snake.datatransfer.messages.MessageType;
import ru.nsu.spirin.snake.datatransfer.messages.RoleChangeMessage;
import ru.nsu.spirin.snake.datatransfer.messages.StateMessage;
import ru.nsu.spirin.snake.datatransfer.messages.SteerMessage;
import ru.nsu.spirin.snake.game.GameState;
import ru.nsu.spirin.snake.datatransfer.NetNode;
import ru.nsu.spirin.snake.datatransfer.messages.JoinMessage;
import ru.nsu.spirin.snake.datatransfer.messages.Message;
import ru.nsu.spirin.snake.datatransfer.messages.PingMessage;
import ru.nsu.spirin.snake.server.ServerGame;
import ru.nsu.spirin.snake.utils.StateUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public final class GameNetwork implements NetworkHandler {
    private static final Logger logger = Logger.getLogger(GameNetwork.class);

    private final RDTSocket rdtSocket;
    private final GameConfig config;
    private final String playerName;
    private final GameView view;
    private final InetSocketAddress multicastInfo;

    private ServerGame activeServerGame = null;
    private GameState gameState;
    private NodeRole nodeRole;
    private NetNode master;
    private NetNode deputy;
    private Instant masterLastSeen = Instant.now();

    private int masterID = -1;
    private int playerID = -1;

    private final AtomicLong msgSeq = new AtomicLong(0);

    private Timer timer = new Timer();
    private Timer masterCheckTimer = new Timer();

    @Override
    public void startNewGame() {
        if (null != activeServerGame) {
            activeServerGame.stop();
            activeServerGame = null;
        }
        try {
            this.activeServerGame = new ServerGame(config, multicastInfo.getAddress(), multicastInfo.getPort(), rdtSocket.getLocalPort(), playerName);
            try {
                this.master = new NetNode(InetAddress.getByName("localhost"), activeServerGame.getSocket().getLocalPort());
            }
            catch (UnknownHostException e) {
                e.printStackTrace();
            }
            masterLastSeen = Instant.now();
            gameState = null;
            timer.cancel();
            timer = new Timer();
            masterCheckTimer.cancel();
            masterCheckTimer = new Timer();
            startMasterCheck();
            startSendPingMessages();
            startHandleReceivedMessages();
            startSendPingMessages();
        }
        catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void joinToGame(@NotNull NetNode gameOwner, @NotNull String playerName) {
        if (null != activeServerGame) {
            activeServerGame.stop();
            activeServerGame = null;
        }
        var complFuture = sendMessage(gameOwner, new JoinMessage(playerName, msgSeq.get()));
        complFuture.thenAccept(ackMessage -> {
            if (ackMessage.getType() == MessageType.ERROR) {
                showError(((ErrorMessage)ackMessage).getErrorMessage());
                return;
            }
            if (ackMessage.getType() != MessageType.ACK) {
                logger.error("NOT AN ACK MESSAGE");
                return;
            }
            masterID = ackMessage.getSenderID();
            playerID = ackMessage.getReceiverID();
            master = gameOwner;
            masterLastSeen = Instant.now();
            gameState = null;
            timer.cancel();
            timer = new Timer();
            masterCheckTimer.cancel();
            masterCheckTimer = new Timer();
            startMasterCheck();
            startSendPingMessages();
            startHandleReceivedMessages();
            changeNodeRole(NodeRole.NORMAL);
        });
    }

    @Override
    public void handleMove(@NotNull Direction direction) {
        sendMessageWithoutConfirmation(master, new SteerMessage(direction, msgSeq.getAndIncrement()));
    }

    @Override
    public void exit() {
        masterCheckTimer.cancel();
        timer.cancel();
        if (null != this.activeServerGame) {
            this.activeServerGame.stop();
        }
    }

    private void startHandleReceivedMessages() {
        timer.schedule(getReceivedMessageHandler(), 0, 100);
    }

    private void startSendPingMessages() {
        timer.schedule(getPingTimerTask(), 0, config.getPingDelayMs());
    }

    @NotNull
    private TimerTask getReceivedMessageHandler() {
        return new TimerTask() {
            @Override
            public void run() {
            var recv = rdtSocket.receive();
            handleMessage(recv.getValue(), recv.getKey());
            }
        };
    }

    private void handleMessage(@NotNull NetNode sender, @NotNull Message message) {
        switch (message.getType()) {
            case ROLE_CHANGE -> handle(sender, (RoleChangeMessage) message);
            case ERROR -> showError(((ErrorMessage) message).getErrorMessage());
            case STATE -> handle(sender, (StateMessage) message);
            default -> throw new IllegalStateException("Cant handle this message type = " + message.getType());
        }
    }

    private void handle(@NotNull NetNode sender, @NotNull StateMessage stateMsg) {
        GameState gameState = stateMsg.getGameState();
        this.master = sender;
        this.masterLastSeen = Instant.now();
        this.deputy = StateUtils.getDeputyFromState(gameState);
        if (this.gameState != null && this.gameState.getStateID() >= gameState.getStateID()) {
            logger.warn("Received state with id=" + gameState.getStateID() + " less then last game state id=" + this.gameState.getStateID());
            return;
        }
        this.gameState = gameState;
        this.view.updateCurrentGame(gameState);
    }

    private void handle(@NotNull NetNode sender, @NotNull RoleChangeMessage roleChangeMsg) {
        switch (nodeRole) {
            case MASTER -> logger.info("MASTER"); //shouldn't happen
            case DEPUTY -> {
                if (roleChangeMsg.getSenderRole() == NodeRole.MASTER && roleChangeMsg.getReceiverRole() == NodeRole.MASTER) {
                    swapToMaster();
                    this.deputy = null;
                }
                else if (roleChangeMsg.getSenderRole() == NodeRole.MASTER &&
                         roleChangeMsg.getReceiverRole() == NodeRole.VIEWER) {
                    lose();
                }
                else {
                    logger.warn("Unsupported roles at role change message=" + roleChangeMsg + " from=" + sender);
                    throw new IllegalArgumentException(
                            "Unsupported roles at role change message=" + roleChangeMsg + " from=" + sender);
                }
            }
            case NORMAL -> {
                if (roleChangeMsg.getSenderRole() == NodeRole.MASTER && roleChangeMsg.getReceiverRole() == NodeRole.DEPUTY) {
                    changeNodeRole(NodeRole.DEPUTY);
                    this.masterLastSeen = Instant.now();
                }
                else if (roleChangeMsg.getSenderRole() == NodeRole.DEPUTY &&
                         roleChangeMsg.getReceiverRole() == NodeRole.NORMAL) {
                    this.master = sender;
                    this.masterLastSeen = Instant.now();
                    this.deputy = null;
                }
                else if (roleChangeMsg.getSenderRole() == NodeRole.MASTER &&
                         roleChangeMsg.getReceiverRole() == NodeRole.VIEWER) {
                    lose();
                }
                else {
                    logger.warn("Unsupported roles at role change message=" + roleChangeMsg + " from=" + sender);
                    throw new IllegalArgumentException(
                            "Unsupported roles at role change message=" + roleChangeMsg + " from=" + sender);
                }
            }
            case VIEWER -> logger.info("VIEWER");
        }
    }

    private void swapToMaster() {
        try {
            this.activeServerGame = new ServerGame(gameState.getGameConfig(), gameState, multicastInfo.getAddress(), multicastInfo.getPort());
            this.master = new NetNode(InetAddress.getByName("localhost"), activeServerGame.getSocket().getLocalPort());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        gameState.getActivePlayers().forEach(player -> sendMessage(
                player.getNetNode(),
                new RoleChangeMessage(NodeRole.DEPUTY, NodeRole.NORMAL, msgSeq.getAndIncrement(), playerID, player.getId()))
        );
    }

    private void showError(String error) {
        logger.error("ERROR=" + error);
    }

    @NotNull
    private TimerTask getPingTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                if (master != null) {
                    sendMessageWithoutConfirmation(master, new PingMessage(msgSeq.getAndIncrement()));
                }
            }
        };
    }

    private void changeNodeRole(@NotNull NodeRole nodeRole) {
        if (config == null) {
            logger.error("Cant change role=" + this.nodeRole + " to " + nodeRole + " without config");
            exit();
            throw new IllegalStateException("Cant change role without config");
        }
        this.nodeRole = nodeRole;
    }

    private CompletableFuture<Message> sendMessage(@NotNull NetNode receiver, @NotNull Message message) {
        return rdtSocket.send(message, receiver.getAddress(), receiver.getPort());
    }

    private void sendMessageWithoutConfirmation(@NotNull NetNode receiver, @NotNull Message message) {
        try {
            rdtSocket.sendWithoutConfirm(MessageParser.serializeMessage(message, receiver.getAddress(), receiver.getPort()));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void lose() {
        System.out.println("Lose");
    }

    private void startMasterCheck() {
        masterCheckTimer.schedule(getMasterCheckTask(), 0, config.getNodeTimeoutMs());
    }

    private TimerTask getMasterCheckTask() {
        return new TimerTask() {
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
                }
            }
        };
    }
}
