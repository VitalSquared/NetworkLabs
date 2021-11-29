package ru.nsu.spirin.snake.datatransfer.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MessageType {
    STATE(true),
    ACK(false),
    PING(false),
    ROLE_CHANGE(true),
    STEER(true),
    ANNOUNCEMENT(false),
    JOIN(true),
    ERROR(false);

    private final @Getter boolean needConfirmation;
}
