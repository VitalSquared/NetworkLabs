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
import java.time.Instant;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class RDTSocket {
    private final DatagramSocket socket;
    private final Map<Message, NetNode> sentMessages = new HashMap<>();
    private final Map<Message, NetNode> messagesToSend = new HashMap<>();
    private final Map<Map.Entry<NetNode, Long>, Message> responses = new HashMap<>();
    private final Map<Instant, Map.Entry<Message, NetNode>> receivedMessages = new HashMap<>();

    private final Thread sender;
    private final Thread receiver;

    public RDTSocket(DatagramSocket socket, int senderTimeoutMs) {
        this.socket = socket;
        this.sender = new Thread(new Sender(senderTimeoutMs));
        this.receiver = new Thread(new Receiver());
        this.sender.start();
        this.receiver.start();
    }

    public void stop() {
        this.sender.interrupt();
        this.receiver.interrupt();
    }

    public int getLocalPort() {
        return this.socket.getLocalPort();
    }

    public Message send(Message message, InetAddress destAddress, int destPort) {
        NetNode dest = new NetNode(destAddress, destPort);

        synchronized (sentMessages) {
            Message msgToRemove = null;
            for (var msg : sentMessages.keySet()) {
                if (msg.getType() == message.getType() && sentMessages.get(msg).equals(dest)) {
                    msgToRemove = msg;
                    break;
                }
            }
            sentMessages.remove(msgToRemove);
        }
        synchronized (messagesToSend) {
            Message msgToRemove = null;
            for (var msg : messagesToSend.keySet()) {
                if (msg.getType() == message.getType() && messagesToSend.get(msg).equals(dest)) {
                    msgToRemove = msg;
                    break;
                }
            }
            messagesToSend.remove(msgToRemove);
        }

        var key = new AbstractMap.SimpleEntry<>(dest, message.getMessageSequence());
        addMessageToSend(dest, message);
        Message result;
        synchronized (responses) {
            while (!responses.containsKey(key)) {
                synchronized (messagesToSend) {
                    synchronized (sentMessages) {
                        if (!sentMessages.containsKey(message) && !messagesToSend.containsKey(message)) {
                            return null;
                        }
                    }
                }
                try {
                    responses.wait();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            result = responses.get(key);
            responses.remove(key);
            synchronized (sentMessages) {
                sentMessages.remove(message);
            }
            synchronized (messagesToSend) {
                messagesToSend.remove(message);
            }
        }
        return result;
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
            Instant earliest = receivedMessages.keySet().stream().findFirst().get();
            received = receivedMessages.get(earliest);
            receivedMessages.remove(earliest);
        }
        return received;
    }

    public void addMessageToSend(@NotNull NetNode receiver, @NotNull Message message) {
        synchronized (messagesToSend) {
            messagesToSend.put(message, receiver);
            messagesToSend.notifyAll();
        }
    }

    public void addReceivedMessage(@NotNull NetNode sender, @NotNull Message gameMessage) {
        if (gameMessage.getType() == MessageType.ACK || gameMessage.getType() == MessageType.ERROR) {
            synchronized (responses) {
                responses.put(new AbstractMap.SimpleEntry<>(sender, gameMessage.getMessageSequence()), gameMessage);
                responses.notifyAll();
            }
            return;
        }
        if (gameMessage.getType().isNeedConfirmation()) {
            try {
                sendWithoutConfirm(MessageParser.serializeMessage(
                        new AckMessage(gameMessage.getMessageSequence(), gameMessage.getReceiverID(), gameMessage.getSenderID()),
                        sender.getAddress(),
                        sender.getPort()
                ));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        synchronized (receivedMessages) {
            receivedMessages.put(Instant.now(), new AbstractMap.SimpleEntry<>(Objects.requireNonNull(gameMessage), Objects.requireNonNull(sender)));
            receivedMessages.notifyAll();
        }
    }

    @RequiredArgsConstructor
    private class Sender implements Runnable {
        private final int timeoutMs;

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                synchronized (messagesToSend) {
                    synchronized (sentMessages) {
                        messagesToSend.putAll(sentMessages);
                        sentMessages.clear();
                    }
                    for (var msg : messagesToSend.keySet()) {
                        NetNode node = messagesToSend.get(msg);
                        synchronized (responses) {
                            if (responses.containsKey(new AbstractMap.SimpleEntry<>(node, msg.getMessageSequence()))) {
                                continue;
                            }
                        }
                        try {
                            socket.send(MessageParser.serializeMessage(msg, node.getAddress(), node.getPort()));
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    synchronized (sentMessages) {
                        sentMessages.putAll(messagesToSend);
                    }
                    messagesToSend.clear();

                    try {
                        messagesToSend.wait();
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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
