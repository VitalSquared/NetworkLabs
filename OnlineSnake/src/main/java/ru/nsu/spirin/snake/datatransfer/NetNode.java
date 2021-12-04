package ru.nsu.spirin.snake.datatransfer;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public class NetNode implements Serializable {
    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 0xFFFF;

    private final @Getter InetAddress address;
    private final @Getter int port;

    public NetNode(InetAddress address, int port) {
        this.address = address;
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException("Port must be in range [" + MIN_PORT + ", " + MAX_PORT + "]");
        }
        this.port = port;
    }

    public NetNode(@NotNull String address, int port) throws UnknownHostException {
        this(InetAddress.getByName(address), port);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof NetNode)) {
            return false;
        }
        NetNode other = (NetNode) object;
        return (this.port == other.port) && this.address.equals(other.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.address, this.port);
    }

    @Override
    public String toString() {
        return String.format("NetNode{%s:%d}", this.address, this.port);
    }
}
