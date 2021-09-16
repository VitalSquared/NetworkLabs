package ru.nsu.spirin.detector;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.HashMap;

@RequiredArgsConstructor()
public final class Detector {
    private static final int    BUF_SIZE        = 1024;
    private static final String DEFAULT_MESSAGE = "";

    private final @NonNull InetAddress address;

    private final int port;
    private final int messageIntervalMillis;
    private final int timeoutMillis;
    private final int workTimeMillis;

    private final HashMap<String, Long> connections = new HashMap<>();

    private long startTimeMillis;

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
                }
                catch (SocketTimeoutException socketTimeoutException) {
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

            receiveSocket.leaveGroup(this.address);
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private boolean shouldWork() {
        return workTimeMillis >= (System.currentTimeMillis() - startTimeMillis);
    }

    private void sendMessage(DatagramSocket socket) throws IOException {
        byte[] buffer = DEFAULT_MESSAGE.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.address, this.port);
        socket.send(packet);
    }

    private void removeUnavailableConnections(long currentTimeMillis) {
        boolean removedAny = false;
        for (var it = this.connections.entrySet().iterator(); it.hasNext(); ) {
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
        for (var entry : this.connections.entrySet()) {
            System.out.println(entry.getKey());
        }
    }
}
