package ru.nsu.spirin.snake.datatransfer;

import lombok.RequiredArgsConstructor;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.messages.MessageOwner;
import ru.nsu.spirin.snake.messages.MessageParser;
import ru.nsu.spirin.snake.messages.messages.AckMessage;
import ru.nsu.spirin.snake.messages.messages.Message;
import ru.nsu.spirin.snake.messages.messages.MessageType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@RequiredArgsConstructor
public final class GameSocket implements RDTSocket {
    private static final Logger logger = Logger.getLogger(GameSocket.class);

    private final DatagramSocket socket;
    private final NetworkInterface networkInterface;
    private final int sendDelayMs;

    private final Map<Long, Message> responses = new HashMap<>();
    private final Map<Instant, MessageOwner> receivedMessages = new HashMap<>();
    private final Map<Long, TimerTask> sendTasks = new HashMap<>();

    private Thread receiver = null;
    private Timer timer = new Timer();

    public GameSocket(NetworkInterface networkInterface, int sendDelayMs) throws IOException {
        this.socket = new MulticastSocket();
        this.sendDelayMs = sendDelayMs;
        this.networkInterface = networkInterface;
    }

    @Override
    public void start() {
        this.receiver = new Thread(new Receiver());
        this.receiver.start();
        this.timer = new Timer();
    }

    @Override
    public void stop() {
        if (null != this.receiver) {
            this.receiver.interrupt();
        }
        this.timer.cancel();
        this.responses.clear();
        this.receivedMessages.clear();
        this.sendTasks.clear();
    }

    @Override
    public InetAddress getAddress() {
        return this.networkInterface.getInetAddresses().nextElement();
    }

    @Override
    public int getLocalPort() {
        return this.socket.getLocalPort();
    }

    @Override
    public Message send(Message message, NetNode receiver) {
        DatagramPacket packet = MessageParser.serializeMessage(message, receiver.getAddress(), receiver.getPort());
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    socket.send(packet);
                }
                catch (IOException exception) {
                    logger.error(exception.getLocalizedMessage());
                }
            }
        };
        this.timer.schedule(task, 0, this.sendDelayMs);

        Message result;
        synchronized (this.sendTasks) {
            this.sendTasks.put(message.getMessageSequence(), task);
            while (this.sendTasks.containsKey(message.getMessageSequence())) {
                try {
                    this.sendTasks.wait();
                }
                catch (InterruptedException exception) {
                    logger.error(exception.getLocalizedMessage());
                    task.cancel();
                    this.sendTasks.remove(message.getMessageSequence());
                    return null;
                }
            }
        }

        synchronized (this.responses) {
            result = this.responses.get(message.getMessageSequence());
            this.responses.remove(message.getMessageSequence());
        }
        return result;
    }

    @Override
    public void sendNonBlocking(Message message, NetNode receiver) {
        DatagramPacket packet = MessageParser.serializeMessage(message, receiver.getAddress(), receiver.getPort());
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    socket.send(packet);
                }
                catch (IOException exception) {
                    logger.error(exception.getLocalizedMessage());
                }
            }
        };
        this.timer.schedule(task, 0, this.sendDelayMs);
        synchronized (this.sendTasks) {
            this.sendTasks.put(message.getMessageSequence(), task);
        }
    }

    @Override
    public void sendWithoutConfirm(Message message, NetNode receiver) {
        try {
            this.socket.send(MessageParser.serializeMessage(message, receiver.getAddress(), receiver.getPort()));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public MessageOwner receive() {
        MessageOwner received;
        synchronized (this.receivedMessages) {
            while (this.receivedMessages.isEmpty()) {
                try {
                    this.receivedMessages.wait();
                }
                catch (InterruptedException exception) {
                    logger.error(exception.getLocalizedMessage());
                }
            }

            Instant earliest = this.receivedMessages.keySet().stream().findFirst().get();
            received = this.receivedMessages.get(earliest);
            this.receivedMessages.remove(earliest);
        }
        return received;
    }

    @Override
    public void removePendingMessage(long messageSequence) {
        synchronized (this.sendTasks) {
            this.sendTasks.remove(messageSequence);
            this.sendTasks.notifyAll();
        }
    }

    private void addReceivedMessage(@NotNull NetNode sender, @NotNull Message gameMessage) {
        if (MessageType.ACK.equals(gameMessage.getType()) || MessageType.ERROR.equals(gameMessage.getType())) {
            synchronized (this.responses) {
                this.responses.put(gameMessage.getMessageSequence(), gameMessage);
            }
            synchronized (this.sendTasks) {
                TimerTask task = this.sendTasks.get(gameMessage.getMessageSequence());
                if (null != task) {
                    task.cancel();
                }
                this.sendTasks.remove(gameMessage.getMessageSequence());
                this.sendTasks.notifyAll();
            }

            if (MessageType.ACK.equals(gameMessage.getType())) {
                return;
            }
        }
        if (gameMessage.getType().isNeedConfirmation()) {
            sendWithoutConfirm(
                    new AckMessage(gameMessage.getMessageSequence(), gameMessage.getReceiverID(), gameMessage.getSenderID()),
                    sender
            );
        }
        synchronized (this.receivedMessages) {
            this.receivedMessages.put(Instant.now(), new MessageOwner(gameMessage, sender));
            this.receivedMessages.notifyAll();
        }
    }

    private class Receiver implements Runnable {
        private int PACKET_SIZE = 4096;

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
                    socket.receive(packet);
                    Message message = MessageParser.deserializeMessage(packet);
                    addReceivedMessage(new NetNode(packet.getAddress(), packet.getPort()), message);
                }
                catch (IOException exception) {
                    logger.error(exception.getLocalizedMessage());
                }
            }
        }
    }
}
