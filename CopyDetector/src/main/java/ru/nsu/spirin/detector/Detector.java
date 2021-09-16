package ru.nsu.spirin.detector;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.UUID;

@RequiredArgsConstructor
public final class Detector {
    private static final String UUID_NAME = "COPY_DETECTOR";

    private final @NonNull InetAddress address;

    private final int port;
    private final int messageIntervalMillis;
    private final int timeoutMillis;
    private final int workTimeMillis;

    private final HashMap<String, Long> connections = new HashMap<>();

    private long startTimeMillis;

    public void run() throws IOException {
        UUID uuid = UUID.nameUUIDFromBytes(UUID_NAME.getBytes());

        this.startTimeMillis = System.currentTimeMillis();
        MulticastSocket receiveSocket = new MulticastSocket(this.port);
        DatagramSocket sendSocket = new DatagramSocket();
        byte[] buffer = new byte[uuid.toString().length()];

        try (receiveSocket; sendSocket) {
            receiveSocket.setSoTimeout(this.messageIntervalMillis);
            receiveSocket.joinGroup(this.address);

            while (shouldWork()) {
                sendMessage(sendSocket, uuid);

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    receiveSocket.receive(packet);
                    if (!isDataValid(packet.getData(), uuid)) {
                        throw new InvalidObjectException("");
                    }
                }
                catch (SocketTimeoutException | InvalidObjectException socketTimeoutException) {
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

    private boolean isDataValid(byte[] data, UUID uuid) {
        return uuid.equals(UUID.fromString(new String(data)));
    }

    private void sendMessage(DatagramSocket socket, UUID uuid) throws IOException {
        byte[] buffer = uuid.toString().getBytes();
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
