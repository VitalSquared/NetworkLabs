package ru.nsu.spirin.transfer.server;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

@RequiredArgsConstructor
public class Server {
    private final int port;
    private final int backlog;

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(this.port, this.backlog)) {
            System.out.println("Server started with port = " + this.port);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress() + ":" + socket.getPort());
                Thread clientHandler = new Thread(new ClientRequestHandler(socket));
                clientHandler.start();
            }
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
