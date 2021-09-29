package ru.nsu.spirin.transfer;

import org.apache.log4j.Logger;
import ru.nsu.spirin.transfer.client.Client;
import ru.nsu.spirin.transfer.server.Server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    private static final int DEFAULT_PORT = 2050;
    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 0xFFFF;

    private static final int DEFAULT_BACKLOG = 5;
    private static final int MIN_BACKLOG = 1;
    private static final int MAX_BACKLOG = 10;

    public static void main(String[] args) {
        if (0 == args.length) {
            logger.error("[1] args: --server port [backlog=5]");
            logger.error("[2] args: --client file_name server_ip port");
            return;
        }
        switch (args[0]) {
            case "--server" -> runServer(args);
            case "--client" -> runClient(args);
            default -> logger.error("First arg is not '--server' or '--client': " + args[0]);
        }
    }

    private static void runServer(String[] args) {
        if (2 > args.length) {
            logger.error("args: --server port [backlog=5]");
            return;
        }

        int port = convertArgumentToInteger(args, 1, DEFAULT_PORT, MIN_PORT, MAX_PORT);
        int backlog = convertArgumentToInteger(args, 2, DEFAULT_BACKLOG, MIN_BACKLOG, MAX_BACKLOG);
        Server server = new Server(port, backlog);
        server.run();
    }

    private static void runClient(String[] args) {
        if (4 > args.length) {
            logger.error("args: --client file_name server_ip port");
            return;
        }

        String fileName = args[1];
        String serverIP = args[2];
        int port = convertArgumentToInteger(args, 3, DEFAULT_PORT, MIN_PORT, MAX_PORT);

        Path path = getFilePath(fileName);
        if (null == path) {
            logger.error("File doesn't exist: " + fileName);
            return;
        }

        InetAddress address;
        try {
            address = InetAddress.getByName(serverIP);
        }
        catch (UnknownHostException exception) {
            logger.error("Unable to connect to {" + serverIP + "}: " + exception.getLocalizedMessage());
            return;
        }

        Client client = new Client(path, address, port);
        client.run();
    }

    private static int convertArgumentToInteger(String[] args, int index, int defaultValue, int minValue, int maxValue) {
        try {
            if (index >= args.length) {
                logger.info(String.format("Argument no. %d doesn't exist. Defaulting to {%d}", index + 1, defaultValue));
                return defaultValue;
            }
            int value = Integer.parseInt(args[index]);
            if (minValue <= value && value <= maxValue) {
                return value;
            }
            logger.error(String.format("Converted number {%d} is not in range [%d, %d]. Defaulting to {%d}", value, minValue, maxValue, defaultValue));
        }
        catch (NumberFormatException numberFormatException) {
            logger.error(String.format("Unable to convert {%s} to integer. Defaulting to {%d}", args[index], defaultValue));
        }
        return defaultValue;
    }

    private static Path getFilePath(String fileName) {
        Path path = Paths.get(fileName);
        return Files.exists(path) ? path : null;
    }
}
