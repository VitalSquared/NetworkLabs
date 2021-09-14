package ru.nsu.spirin.detector;

import java.net.InetAddress;

public class Main {
    private static final int DEFAULT_PORT = 2050;
    private static final int DEFAULT_TIMEOUT_MILLIS = 1000;
    private static final int DEFAULT_MESSAGE_INTERVAL_MILLIS = 250;
    private static final int DEFAULT_WORK_TIME_MILLIS = 8000;

    public static void main(String[] args) {
        if (0 == args.length) {
            System.out.println("params: multicast_ip [port=2050] [work_time_millis=8000] [timeout_of_others_millis=1000] [message_wait_interval_millis=250]");
            return;
        }

        String multicastIP = args[0];
        int port = convertArgumentToInteger(args, 1, DEFAULT_PORT);
        int workTimeMillis = convertArgumentToInteger(args, 2, DEFAULT_WORK_TIME_MILLIS);
        int timeoutMillis = convertArgumentToInteger(args, 3, DEFAULT_TIMEOUT_MILLIS);
        int messageIntervalMillis = convertArgumentToInteger(args, 4, DEFAULT_MESSAGE_INTERVAL_MILLIS);

        try {
            InetAddress address = InetAddress.getByName(multicastIP);
            if (!address.isMulticastAddress()) {
                System.err.println("Specified address is not multicast");
                return;
            }
            Detector detector = new Detector(address, port, messageIntervalMillis, timeoutMillis, workTimeMillis);
            detector.run();
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static int convertArgumentToInteger(String[] args, int index, int defaultValue) {
        try {
            if (index < args.length) {
                return Integer.parseInt(args[index]);
            }
        }
        catch (NumberFormatException numberFormatException) {
            System.err.println("Unable to convert {" + args[index] + "} to integer. Defaulting to {" + defaultValue + "}");
        }
        return defaultValue;
    }
}
