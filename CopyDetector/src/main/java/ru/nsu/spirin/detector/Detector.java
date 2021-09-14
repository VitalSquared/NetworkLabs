package ru.nsu.spirin.detector;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.HashMap;

public final class Detector {
    private static final int BUF_SIZE = 1024;
    private static final String DEFAULT_MESSAGE = "123";

    private final InetAddress address;
    private final int port;
    private final int messageIntervalMillis;
    private final int timeoutMillis;
    private final int workTimeMillis;

    private final HashMap<String, Long> connections = new HashMap<>();

    private long startTimeMillis;

    public Detector(InetAddress address, int port, int messageIntervalMillis, int timeoutMillis, int workTimeMillis) {
        this.address = address;
        this.port = port;
        this.messageIntervalMillis = messageIntervalMillis;
        this.timeoutMillis = timeoutMillis;
        this.workTimeMillis = workTimeMillis;
    }

    public void run() throws IOException {
        this.startTimeMillis = System.currentTimeMillis();

        MulticastSocket receiveSocket = new MulticastSocket(this.port);
        DatagramSocket sendSocket = new DatagramSocket();
        byte[] buffer = new byte[BUF_SIZE];

        try (receiveSocket; sendSocket) {
            receiveSocket.setSoTimeout(this.messageIntervalMillis);
            receiveSocket.joinGroup(this.address);

            while (shouldWork()) {
                sendMessage(sendSocket);

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    receiveSocket.receive(packet);
                    if (!isMessageValid(packet.getData())) {
                        throw new InvalidDataException("");
                    }
                }
                catch (SocketTimeoutException | InvalidDataException socketTimeoutException) {
                    removeUnavailableConnections(System.currentTimeMillis());
                    continue;
                }

                String IPPort = packet.getAddress() + ":" + packet.getPort();
                boolean connectionExists = connections.containsKey(IPPort);

                long curTimeMillis = System.currentTimeMillis();
                removeUnavailableConnections(curTimeMillis);
                connections.put(IPPort, curTimeMillis);

                if (!connectionExists) {
                    printAliveCopies();
                }
            }
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private boolean shouldWork() {
        return workTimeMillis >= (System.currentTimeMillis() - startTimeMillis);
    }

    private boolean isMessageValid(byte[] message) {
        return true;
    }

    private void sendMessage(DatagramSocket socket) throws IOException {
        byte[] buffer = DEFAULT_MESSAGE.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.address, this.port);
        socket.send(packet);
    }

    private void removeUnavailableConnections(long currentTimeMillis) {
        boolean removedAny = false;
        for (var it = this.connections.entrySet().iterator(); it.hasNext();) {
            var entry = it.next();
            if (currentTimeMillis - entry.getValue() > this.timeoutMillis) {
                it.remove();
                removedAny = true;
            }
        }
        if (removedAny) {
            printAliveCopies();
        }
    }

    private void printAliveCopies() {
        System.out.println("List of alive copies: ");
        for (var it = this.connections.entrySet().iterator(); it.hasNext();) {
            var entry = it.next();
            System.out.println(entry.getKey());
        }
    }
}
