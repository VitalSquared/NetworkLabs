package ru.nsu.spirin.snake.game;

import lombok.Getter;
import lombok.Setter;
import me.ippolitov.fit.snakes.SnakesProto.Direction;
import me.ippolitov.fit.snakes.SnakesProto.GameState.Snake.SnakeState;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.utils.DirectionUtils;
import ru.nsu.spirin.snake.utils.PointUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Snake implements Serializable {
    private final int xCoordinateLimit;
    private final int yCoordinateLimit;

    private @NotNull @Getter Point2D head;
    private @NotNull @Getter Point2D tail;

    private @Getter @Setter int playerID = -1;
    private final @NotNull @Getter List<Point2D> points;
    private @NotNull @Getter @Setter SnakeState state = SnakeState.ALIVE;
    private @NotNull @Getter Direction direction;

    public Snake(@NotNull Point2D head, @NotNull Point2D tail, int xCoordinateLimit, int yCoordinateLimit) {
        this.xCoordinateLimit = xCoordinateLimit;
        this.yCoordinateLimit = yCoordinateLimit;
        this.head = Objects.requireNonNull(head, "Head point cant be null");
        this.tail = Objects.requireNonNull(tail, "Tail point cant be null");
        validateInitHeadAndTail(head, tail);
        points = new ArrayList<>();
        points.add(head);
        points.add(tail);

        this.direction = calculateCurrentDirection(head, tail);
    }

    public Snake(int playerID, List<Point2D> points, @NotNull SnakeState state, @NotNull Direction direction, int xCoordinateLimit, int yCoordinateLimit) {
        this.xCoordinateLimit = xCoordinateLimit;
        this.yCoordinateLimit = yCoordinateLimit;
        this.playerID = playerID;
        this.points = new ArrayList<>(points);
        this.state = state;
        this.direction = direction;
        this.head = this.points.get(0);
        this.tail = this.points.get(this.points.size() - 1);
    }

    private void validateInitHeadAndTail(Point2D head, Point2D tail) {
        if (!PointUtils.arePointsStraightConnected(head, tail, xCoordinateLimit, yCoordinateLimit)) {
            throw new IllegalArgumentException("Head and tail are not connected");
        }
    }

    private Direction calculateCurrentDirection(Point2D head, Point2D tail) {
        validateInitHeadAndTail(head, tail);
        if (PointUtils.getPointToRight(head, xCoordinateLimit).equals(tail)) {
            return Direction.LEFT;
        }
        else if (PointUtils.getPointToLeft(head, xCoordinateLimit).equals(tail)) {
            return Direction.RIGHT;
        }
        else if (PointUtils.getPointBelow(head, yCoordinateLimit).equals(tail)) {
            return Direction.UP;
        }
        else if (PointUtils.getPointAbove(head, yCoordinateLimit).equals(tail)) {
            return Direction.DOWN;
        }
        throw new IllegalStateException("Cant calculate current direction");
    }

    public void makeMove(@NotNull Direction dir) {
        Objects.requireNonNull(dir, "Direction cant be null");
        if (DirectionUtils.getReversed(dir) == direction) {
            dir = direction;  //Блокирует движение змейки в противоположном направлении
        }
        direction = dir;
        head = getNewHead(dir);
        points.add(0, head);
    }

    private Point2D getNewHead(@NotNull Direction dir) {
        return switch (dir) {
            case DOWN -> PointUtils.getPointBelow(head, yCoordinateLimit);
            case UP -> PointUtils.getPointAbove(head, yCoordinateLimit);
            case LEFT -> PointUtils.getPointToLeft(head, xCoordinateLimit);
            case RIGHT -> PointUtils.getPointToRight(head, xCoordinateLimit);
        };
    }

    public void makeMove() {
        makeMove(direction);
    }

    public void removeTail() {
        points.remove(tail);
        if (points.size() <= 1) {
            throw new IllegalStateException("Snake cant have less than 2 points");
        }
        tail = points.get(points.size() - 1);
    }

    public boolean isSnakeBody(@NotNull Point2D p) {
        for (int i = 1; i < points.size() - 1; ++i) {
            if (p.equals(points.get(i))) {
                return true;
            }
        }
        return false;
    }

    public boolean isSnake(@NotNull Point2D p) {
        return p.equals(head) || p.equals(tail) || isSnakeBody(p);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Snake other = (Snake) object;
        return this.points.equals(other.points);
    }

    @Override
    public int hashCode() {
        return Objects.hash(points);
    }
}
