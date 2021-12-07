package ru.nsu.spirin.snake.datatransfer;

import ru.nsu.spirin.snake.messages.MessageOwner;
import ru.nsu.spirin.snake.messages.messages.Message;

public interface RDTSocket {
    Message send(Message message, NetNode receiver);
    void sendNonBlocking(Message message, NetNode receiver);
    void sendWithoutConfirm(Message message, NetNode receiver);
    MessageOwner receive();

    void removePendingMessages(long messageSequence);

    int getLocalPort();

    void start();
    void stop();
}
