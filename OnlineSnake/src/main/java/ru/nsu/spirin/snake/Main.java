package ru.nsu.spirin.snake;

import org.apache.log4j.Logger;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Scanner;
import java.util.stream.Collectors;

public final class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) throws SocketException {
        if (1 != args.length) {
            logger.info("Usage: player_name");
            return;
        }

        NetworkInterface networkInterface = getNetworkInterface();
        if (null == networkInterface) {
            return;
        }

        JavaFXStarter.setNetworkInterface(networkInterface);
        JavaFXStarter.setPlayerName(args[0]);
        JavaFXStarter.main(new String[0]);
    }

    private static NetworkInterface getNetworkInterface() throws SocketException {
        var networkInterfaces = NetworkInterface.networkInterfaces().filter(netInterface -> {
            try {
                return netInterface.isUp() && !netInterface.isLoopback();
            }
            catch (SocketException ignored) {

            }
            return false;
        }).collect(Collectors.toList());

        if (networkInterfaces.isEmpty()) {
            logger.info("You don't have any working network interfaces.");
            return null;
        }

        if (networkInterfaces.size() == 1) {
            return networkInterfaces.get(0);
        }

        System.out.println("Choose network interface");
        for (int i = 0; i < networkInterfaces.size(); i++) {
            System.out.println(i + ": " + networkInterfaces.get(i).toString());
        }

        System.out.print("Your input: ");
        int selected;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            selected = scanner.nextInt();
            if (selected < 0 || selected > networkInterfaces.size()) {
                System.out.println("Wrong number. Choose again: ");
                continue;
            }
            break;
        }

        return networkInterfaces.get(selected);
    }
}
