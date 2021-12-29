package ru.nsu.spirin.proxy.handlers;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import ru.nsu.spirin.proxy.models.Connection;

public final class ForwardHandler extends Handler {
    public ForwardHandler(Connection connection) {
        super(connection);
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {
        Connection connection = ((Handler) selectionKey.attachment()).getConnection();
        int readCount = read(selectionKey);
        if (0 != readCount) {
            connection.notifyBufferListener();
        }
    }
}
