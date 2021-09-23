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
                Socket newConnection = serverSocket.accept();
                logger.info("Client connected: " + newConnection.getInetAddress() + ":" + newConnection.getPort());
                threadPool.submit(new ClientRequestHandler(newConnection));
            }
        }
        catch (IOException exception) {
            logger.error(exception.getLocalizedMessage());
        }
        threadPool.shutdown();
    }
}
