package ru.nsu.spirin.snake.gamehandler;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@RequiredArgsConstructor
public final class Point2D implements Serializable {
    private final @Getter int x;
    private final @Getter int y;

    @Override
    public boolean equals(Object object){
        if (this == object) {
            return true;
        }
        if (!(object instanceof Point2D)){
            return false;
        }
        Point2D other = (Point2D) object;
        return (this.x == other.x) && (this.y == other.y);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + this.x;
        hash = 71 * hash + this.y;
        return hash;
    }

    @Override
    public String toString() {
        return String.format("Point2D{%d, %d}", this.x, this.y);
    }
}
