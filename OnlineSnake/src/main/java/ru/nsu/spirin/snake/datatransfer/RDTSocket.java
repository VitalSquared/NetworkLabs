package ru.nsu.spirin.snake.datatransfer;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.datatransfer.messages.AckMessage;
import ru.nsu.spirin.snake.datatransfer.messages.Message;
import ru.nsu.spirin.snake.datatransfer.messages.MessageType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class RDTSocket {
    private final DatagramSocket socket;
    private final Map<Message, NetNode> receivedMessages = new ConcurrentHashMap<>();
    private final Map<Message, NetNode> sentMessages = new ConcurrentHashMap<>();
    private final Map<Message, NetNode> messagesToSend = new ConcurrentHashMap<>();
    private final Map<Long, Message> ackedMessages = new ConcurrentHashMap<>();

    private final Thread sender;
    private final Thread receiver;

    public RDTSocket(DatagramSocket socket, int senderTimeoutMs) {
        this.socket = socket;
        sender = new Thread(new Sender(senderTimeoutMs));
        receiver = new Thread(new Receiver());
        sender.start();
        receiver.start();
    }

    public void stop() {
        sender.interrupt();
        receiver.interrupt();
    }

    public int getLocalPort() {
        return this.socket.getLocalPort();
    }

    public CompletableFuture<Message> send(Message message, InetAddress destAddress, int destPort) {
        return CompletableFuture.supplyAsync(() -> {
            addMessageToSend(new NetNode(destAddress, destPort), message);
            Message result;
            synchronized (ackedMessages) {
                while (!ackedMessages.containsKey(message.getMessageSequence())) {
                    try {
                        ackedMessages.wait();
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                result = ackedMessages.get(message.getMessageSequence());
                ackedMessages.remove(message.getMessageSequence());
            }
            return result;
        });
    }

    public void sendWithoutConfirm(DatagramPacket packet) throws IOException {
        this.socket.send(packet);
    }

    public Map.Entry<Message, NetNode> receive() {
        Map.Entry<Message, NetNode> received;
        synchronized (receivedMessages) {
            while (receivedMessages.isEmpty()) {
                try {
                    receivedMessages.wait();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            received = receivedMessages.entrySet().stream().findFirst().get();
            receivedMessages.remove(received.getKey());
        }
        return received;
    }

    public void addMessageToSend(@NotNull NetNode receiver, @NotNull Message gameMessage) {
        synchronized (messagesToSend) {
            messagesToSend.put(gameMessage, receiver);
        }
    }

    public void addReceivedMessage(@NotNull NetNode sender, @NotNull Message gameMessage) {
        if (gameMessage.getType() == MessageType.ACK || gameMessage.getType() == MessageType.ERROR) {
            synchronized (sentMessages) {
                sentMessages.remove(sender);
            }
            synchronized (ackedMessages) {
                ackedMessages.put(gameMessage.getMessageSequence(), gameMessage);
                ackedMessages.notifyAll();
            }
            return;
        }
        if (gameMessage.getType().isNeedConfirmation()) {
            addMessageToSend(sender, new AckMessage(gameMessage.getMessageSequence(), gameMessage.getReceiverID(), gameMessage.getSenderID()));
        }
        synchronized (receivedMessages) {
            receivedMessages.put(Objects.requireNonNull(gameMessage), Objects.requireNonNull(sender));
            receivedMessages.notifyAll();
        }
    }

    @RequiredArgsConstructor
    private class Sender implements Runnable {
        private final int timeoutMs;

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    synchronized (messagesToSend) {
                        synchronized (sentMessages) {
                            messagesToSend.putAll(sentMessages);
                            sentMessages.clear();
                        }
                        for (var msg : messagesToSend.keySet()) {
                            NetNode node = messagesToSend.get(msg);
                            socket.send(MessageParser.serializeMessage(msg, node.getAddress(), node.getPort()));
                            synchronized (sentMessages) {
                                sentMessages.put(msg, node);
                            }
                        }
                    }
                    Thread.sleep(timeoutMs);
                }
                catch (IOException | InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
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
                    exception.printStackTrace();
                }
            }
        }
    }
}
