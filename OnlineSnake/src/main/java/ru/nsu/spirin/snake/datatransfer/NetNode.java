package ru.nsu.spirin.snake.datatransfer;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public class NetNode implements Serializable {
    private final @NotNull @Getter InetAddress address;
    private final @Getter int port;

    public NetNode(@NotNull InetAddress address, int port) {
        this.address = Objects.requireNonNull(address, "Node address cant be null");
        if (port <= 0) {
            throw new IllegalArgumentException("Port must be positive");
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
        return Objects.hash(address, port);
    }

    @Override
    public String toString() {
        return String.format("NetNode{%s:%d}", this.address, this.port);
    }
}
