package ru.nsu.spirin.snake.utils;

import lombok.experimental.UtilityClass;
import me.ippolitov.fit.snakes.SnakesProto;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.game.Point2D;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@UtilityClass
public final class PointUtils {
    private static final String POINT_NULL_ERROR_MESSAGE = "Point2D cant be null";

    public static List<Point2D> getPointList(List<SnakesProto.GameState.Coord> coordList) {
        return coordList.stream().map(coord -> new Point2D(coord.getX(), coord.getY())).collect(Collectors.toList());
    }

    private static int countNewPointCoordinate(int newCoordinate, int coordinateLimit) {
        if (newCoordinate >= coordinateLimit) {
            return newCoordinate % coordinateLimit;
        } else if (newCoordinate < 0) {
            return coordinateLimit - 1;
        }
        return newCoordinate;
    }

    @NotNull
    public static Point2D getPointAbove(@NotNull Point2D point, int yLimit) {
        Objects.requireNonNull(point, POINT_NULL_ERROR_MESSAGE);
        return new Point2D(
                point.getX(),
                countNewPointCoordinate(point.getY() - 1, yLimit)
        );
    }

    @NotNull
    public static Point2D getPointBelow(@NotNull Point2D point, int yLimit) {
        Objects.requireNonNull(point, POINT_NULL_ERROR_MESSAGE);
        return new Point2D(
                point.getX(),
                countNewPointCoordinate(point.getY() + 1, yLimit)
        );
    }

    @NotNull
    public static Point2D getPointToRight(@NotNull Point2D point, int xLimit) {
        Objects.requireNonNull(point, POINT_NULL_ERROR_MESSAGE);
        return new Point2D(
                countNewPointCoordinate(point.getX() + 1, xLimit),
                point.getY()
        );
    }

    @NotNull
    public static Point2D getPointToLeft(@NotNull Point2D point, int xLimit) {
        Objects.requireNonNull(point, POINT_NULL_ERROR_MESSAGE);
        return new Point2D(
                countNewPointCoordinate(point.getX() - 1, xLimit),
                point.getY()
        );
    }

    @NotNull
    public static List<Point2D> getStraightConnectedPoints(@NotNull Point2D point, int xLimit, int yLimit) {
        Objects.requireNonNull(point, POINT_NULL_ERROR_MESSAGE);
        return List.of(
                PointUtils.getPointAbove(point, yLimit),
                PointUtils.getPointBelow(point, yLimit),
                PointUtils.getPointToLeft(point, xLimit),
                PointUtils.getPointToRight(point, xLimit)
        );
    }

    public static boolean arePointsStraightConnected(@NotNull Point2D p1, @NotNull Point2D p2, int xLimit, int yLimit) {
        Objects.requireNonNull(p1, POINT_NULL_ERROR_MESSAGE);
        Objects.requireNonNull(p1, POINT_NULL_ERROR_MESSAGE);
        return getStraightConnectedPoints(p1, xLimit, yLimit).contains(p2);
    }
}
