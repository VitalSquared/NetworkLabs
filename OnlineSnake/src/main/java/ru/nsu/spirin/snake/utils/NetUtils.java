package ru.nsu.spirin.snake.utils;

import lombok.experimental.UtilityClass;

import java.net.InetAddress;
import java.net.UnknownHostException;

@UtilityClass
public final class NetUtils {
    private static final InetAddress localhostAddress;

    static {
        try {
            localhostAddress = InetAddress.getByName("localhost");
        }
        catch (UnknownHostException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static InetAddress getLocalhostAddress() {
        return localhostAddress;
    }
}
