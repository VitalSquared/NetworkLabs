package ru.nsu.spirin.snake.datatransfer;

import ru.nsu.spirin.snake.messages.MessageOwner;
import ru.nsu.spirin.snake.messages.messages.Message;

import java.net.InetAddress;

public interface RDTSocket {
    Message send(Message message, NetNode receiver);
    void sendNonBlocking(Message message, NetNode receiver);
    void sendWithoutConfirm(Message message, NetNode receiver);
    MessageOwner receive();

    InetAddress getAddress();
    void removePendingMessage(long messageSequence);

    int getLocalPort();

    void start();
    void stop();
}
