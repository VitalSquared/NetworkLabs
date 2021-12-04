package ru.nsu.spirin.snake.gamehandler;

import lombok.Getter;
import lombok.Setter;
import me.ippolitov.fit.snakes.SnakesProto.Direction;
import me.ippolitov.fit.snakes.SnakesProto.GameState.Snake.SnakeState;
import ru.nsu.spirin.snake.utils.DirectionUtils;
import ru.nsu.spirin.snake.utils.PointUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Snake implements Serializable {
    private final int fieldWidth;
    private final int fieldHeight;

    private @Getter Point2D head;
    private  @Getter Point2D tail;

    private @Getter @Setter int playerID = -1;
    private final @Getter List<Point2D> points;
    private @Getter @Setter SnakeState state = SnakeState.ALIVE;
    private @Getter Direction direction;

    public Snake(Point2D head, Point2D tail, int fieldWidth, int fieldHeight) {
        this.fieldWidth = fieldWidth;
        this.fieldHeight = fieldHeight;
        this.head = head;
        this.tail = tail;
        validateInitHeadAndTail(head, tail);
        this.points = new ArrayList<>();
        this.points.add(head);
        this.points.add(tail);

        this.direction = calculateCurrentDirection(head, tail);
    }

    public Snake(int playerID, List<Point2D> points, SnakeState state, Direction direction, int fieldWidth, int fieldHeight) {
        this.fieldWidth = fieldWidth;
        this.fieldHeight = fieldHeight;
        this.playerID = playerID;
        this.points = new ArrayList<>(points);
        this.state = state;
        this.direction = direction;
        this.head = this.points.get(0);
        this.tail = this.points.get(this.points.size() - 1);
    }

    private void validateInitHeadAndTail(Point2D head, Point2D tail) {
        if (!PointUtils.arePointsStraightConnected(head, tail, this.fieldWidth, this.fieldHeight)) {
            throw new IllegalArgumentException("Head and tail are not connected");
        }
    }

    private Direction calculateCurrentDirection(Point2D head, Point2D tail) {
        validateInitHeadAndTail(head, tail);
        if (PointUtils.getPointToRight(head, this.fieldWidth).equals(tail)) {
            return Direction.LEFT;
        }
        else if (PointUtils.getPointToLeft(head, this.fieldWidth).equals(tail)) {
            return Direction.RIGHT;
        }
        else if (PointUtils.getPointBelow(head, this.fieldHeight).equals(tail)) {
            return Direction.UP;
        }
        else if (PointUtils.getPointAbove(head, this.fieldHeight).equals(tail)) {
            return Direction.DOWN;
        }
        throw new IllegalStateException("Cant calculate current direction");
    }

    public void makeMove(Direction dir) {
        if (null == dir) {
            makeMove();
            return;
        }

        if (DirectionUtils.getReversed(dir).equals(this.direction)) {
            dir = this.direction;  //Блокирует движение змейки в противоположном направлении
        }
        this.direction = dir;
        this.head = getNewHead(dir);
        this.points.add(0, this.head);
    }

    private Point2D getNewHead(Direction dir) {
        return switch (dir) {
            case DOWN -> PointUtils.getPointBelow(this.head, this.fieldHeight);
            case UP -> PointUtils.getPointAbove(this.head, this.fieldHeight);
            case LEFT -> PointUtils.getPointToLeft(this.head, this.fieldWidth);
            case RIGHT -> PointUtils.getPointToRight(this.head, this.fieldWidth);
        };
    }

    public void makeMove() {
        makeMove(direction);
    }

    public void removeTail() {
        this.points.remove(tail);
        if (this.points.size() <= 1) {
            throw new IllegalStateException("Snake cant have less than 2 points");
        }
        this.tail = this.points.get(this.points.size() - 1);
    }

    public boolean isSnakeBody(Point2D p) {
        for (int i = 1; i < this.points.size() - 1; i++) {
            if (p.equals(this.points.get(i))) {
                return true;
            }
        }
        return false;
    }

    public boolean isSnake(Point2D p) {
        return p.equals(this.head) || p.equals(this.tail) || isSnakeBody(p);
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
        return Objects.hash(this.points);
    }
}
