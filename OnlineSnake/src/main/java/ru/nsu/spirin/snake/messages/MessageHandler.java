package ru.nsu.spirin.snake.messages;

import ru.nsu.spirin.snake.datatransfer.NetNode;
import ru.nsu.spirin.snake.messages.messages.ErrorMessage;
import ru.nsu.spirin.snake.messages.messages.JoinMessage;
import ru.nsu.spirin.snake.messages.messages.PingMessage;
import ru.nsu.spirin.snake.messages.messages.RoleChangeMessage;
import ru.nsu.spirin.snake.messages.messages.StateMessage;
import ru.nsu.spirin.snake.messages.messages.SteerMessage;

public interface MessageHandler {
    void handle(NetNode sender, SteerMessage message);
    void handle(NetNode sender, JoinMessage message);
    void handle(NetNode sender, PingMessage message);
    void handle(NetNode sender, StateMessage message);
    void handle(NetNode sender, ErrorMessage message);
    void handle(NetNode sender, RoleChangeMessage message);
}
