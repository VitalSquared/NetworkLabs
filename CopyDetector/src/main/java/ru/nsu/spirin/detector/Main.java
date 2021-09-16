package ru.nsu.spirin.detector;

import java.net.InetAddress;

public class Main {
    private static final int DEFAULT_PORT = 2050;
    private static final int DEFAULT_TIMEOUT_MILLIS = 500;
    private static final int DEFAULT_MESSAGE_INTERVAL_MILLIS = 250;
    private static final int DEFAULT_WORK_TIME_MILLIS = 8000;

    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 0xFFFF;

    private static final int MIN_TIME_MILLIS = 0;
    private static final int MAX_TIME_MILLIS = Integer.MAX_VALUE;

    public static void main(String[] args) {
        if (0 == args.length) {
            System.out.println("params: multicast_ip [port=2050] [work_time_millis=8000] [timeout_of_others_millis=500] [message_wait_interval_millis=250]");
            return;
        }

        String multicastIP = args[0];
        int port = convertArgumentToInteger(args, 1, DEFAULT_PORT, MIN_PORT, MAX_PORT);
        int workTimeMillis = convertArgumentToInteger(args, 2, DEFAULT_WORK_TIME_MILLIS, MIN_TIME_MILLIS, MAX_TIME_MILLIS);
        int timeoutMillis = convertArgumentToInteger(args, 3, DEFAULT_TIMEOUT_MILLIS, MIN_TIME_MILLIS, MAX_TIME_MILLIS);
        int messageIntervalMillis = convertArgumentToInteger(args, 4, DEFAULT_MESSAGE_INTERVAL_MILLIS, MIN_TIME_MILLIS, MAX_TIME_MILLIS);

        try {
            InetAddress address = InetAddress.getByName(multicastIP);
            if (!address.isMulticastAddress()) {
                System.err.printf("Specified address is not multicast: %s", multicastIP);
                return;
            }

            Detector detector = new Detector(address, port, messageIntervalMillis, timeoutMillis, workTimeMillis);
            detector.run();
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
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
}
