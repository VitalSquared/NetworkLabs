package ru.nsu.spirin.transfer.server;

import lombok.RequiredArgsConstructor;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
public class Server {
    private static final Logger logger = Logger.getLogger(Server.class);

    private final int port;
    private final int backlog;

    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(this.port, this.backlog)) {
            logger.info("Server started with port = " + this.port);
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                String clientID = createClientID(clientSocket);
                logger.info("[Server] Client connected: [" + clientID + "]");
                threadPool.submit(new ClientRequestHandler(clientSocket, clientID));
            }
        }
        catch (IOException exception) {
            logger.error("[Server] " + exception.getLocalizedMessage());
        }
        threadPool.shutdown();
    }

    public String createClientID(Socket clientSocket) {
        return clientSocket.getInetAddress() + ":" + clientSocket.getPort();
    }
}
