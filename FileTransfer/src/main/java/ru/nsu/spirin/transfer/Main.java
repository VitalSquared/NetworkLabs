package ru.nsu.spirin.transfer;

import ru.nsu.spirin.transfer.client.Client;
import ru.nsu.spirin.transfer.server.Server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Main {
    private static final int DEFAULT_PORT = 2050;
    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 0xFFFF;

    private static final int DEFAULT_BACKLOG = 5;
    private static final int MIN_BACKLOG = 1;
    private static final int MAX_BACKLOG = 10;

    public static void main(String[] args) {
        if (0 == args.length) {
            System.err.println("[1] args: --server port [backlog=5]");
            System.err.println("[2] args: --client file_path server_ip port");
            return;
        }
        switch (args[0]) {
            case "--server" -> runServer(args);
            case "--client" -> runClient(args);
            default -> System.err.printf("First arg is not '--server' or '--client': %s\n", args[0]);
        }
    }

    private static void runServer(String[] args) {
        if (2 > args.length) {
            System.err.println("args: --server port [backlog=5]");
            return;
        }

        int port = convertArgumentToInteger(args, 1, DEFAULT_PORT, MIN_PORT, MAX_PORT);
        int backlog = convertArgumentToInteger(args, 2, DEFAULT_BACKLOG, MIN_BACKLOG, MAX_BACKLOG);
        Server server = new Server(port, backlog);
        server.run();
    }

    private static void runClient(String[] args) {
        if (4 > args.length) {
            System.err.println("args: --client file_path server_ip port");
            return;
        }

        String filePath = args[1];
        String serverIP = args[2];
        int port = convertArgumentToInteger(args, 3, DEFAULT_PORT, MIN_PORT, MAX_PORT);

        Path file;
        InetAddress address;
        try {
            file = getFilePath(filePath);
            if (file == null) {
                System.err.printf("File doesn't exist: %s\n", filePath);
                return;
            }
            address = InetAddress.getByName(serverIP);
        }
        catch (UnknownHostException exception) {
            System.err.printf("Unable to connect to %s: %s\n", serverIP, exception.getLocalizedMessage());
            return;
        }

        Client client = new Client(file, address, port);
        client.run();
    }

    private static int convertArgumentToInteger(String[] args, int index, int defaultValue, int minValue, int maxValue) {
        try {
            if (index < args.length) {
                int value = Integer.parseInt(args[index]);
                if (minValue <= value && value <= maxValue) {
                    return value;
                }
                System.err.printf("Converted number {%d} is not in range [%d, %d]. Defaulting to {%d}\n", value, minValue, maxValue, defaultValue);
            }
        }
        catch (NumberFormatException numberFormatException) {
            System.err.printf("Unable to convert {%s} to integer. Defaulting to {%d}\n", args[index], defaultValue);
        }
        return defaultValue;
    }

    private static Path getFilePath(String pathStr) {
        Path path = Paths.get(pathStr);
        return Files.exists(path) ? path : null;
    }
}
