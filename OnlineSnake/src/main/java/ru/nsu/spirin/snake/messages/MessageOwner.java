package ru.nsu.spirin.snake.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.nsu.spirin.snake.datatransfer.NetNode;
import ru.nsu.spirin.snake.messages.messages.Message;

import java.util.Objects;

@RequiredArgsConstructor
public final class MessageOwner {
    private final @Getter Message message;
    private final @Getter NetNode owner;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof MessageOwner)) {
            return false;
        }
        MessageOwner other = (MessageOwner) object;
        return (this.message.equals(other.message)) && this.owner.equals(other.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.message, this.owner);
    }
}
