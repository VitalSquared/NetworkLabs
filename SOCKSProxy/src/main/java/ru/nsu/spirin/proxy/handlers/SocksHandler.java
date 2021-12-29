package ru.nsu.spirin.proxy.handlers;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import ru.nsu.spirin.proxy.models.Connection;

public abstract class SocksHandler extends Handler {
    public SocksHandler(Connection connection) {
        super(connection);
    }

    @Override
    public int read(SelectionKey selectionKey) throws IOException {
        int readCount = super.read(selectionKey);
        if (readCount < 0) {
            throw new IOException("Socket closed during SOCKS5 handshake");
        }
        return readCount;
    }
}
