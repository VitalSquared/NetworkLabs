package ru.nsu.spirin.snake.game;

public interface GameObservable {
    void addObserver(GameObserver gameObserver);
    void removeObserver(GameObserver gameObserver);
    void notifyObservers();
}
