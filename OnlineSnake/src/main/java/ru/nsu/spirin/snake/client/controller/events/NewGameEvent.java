package ru.nsu.spirin.snake.client.controller.events;

public final class NewGameEvent extends UserEvent {
    public NewGameEvent() {
        super(UserEventType.NEW_GAME);
    }
}
