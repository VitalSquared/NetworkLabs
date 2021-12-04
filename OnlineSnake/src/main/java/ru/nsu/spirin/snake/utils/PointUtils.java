package ru.nsu.spirin.snake.utils;

import lombok.experimental.UtilityClass;
import me.ippolitov.fit.snakes.SnakesProto;
import ru.nsu.spirin.snake.gamehandler.Point2D;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public final class PointUtils {
    public static List<Point2D> getPointList(List<SnakesProto.GameState.Coord> coordList) {
        return coordList.stream().map(coord -> new Point2D(coord.getX(), coord.getY())).collect(Collectors.toList());
    }

    private static int countNewPointCoordinate(int newCoordinate, int coordinateLimit) {
        if (newCoordinate >= coordinateLimit) {
            return newCoordinate % coordinateLimit;
        }
        else if (newCoordinate < 0) {
            return coordinateLimit - 1;
        }
        return newCoordinate;
    }

    public static Point2D getPointAbove(Point2D point, int fieldHeight) {
        return new Point2D(
                point.getX(),
                countNewPointCoordinate(point.getY() - 1, fieldHeight)
        );
    }

    public static Point2D getPointBelow(Point2D point, int fieldHeight) {
        return new Point2D(
                point.getX(),
                countNewPointCoordinate(point.getY() + 1, fieldHeight)
        );
    }

    public static Point2D getPointToRight(Point2D point, int fieldWidth) {
        return new Point2D(
                countNewPointCoordinate(point.getX() + 1, fieldWidth),
                point.getY()
        );
    }

    public static Point2D getPointToLeft(Point2D point, int fieldWidth) {
        return new Point2D(
                countNewPointCoordinate(point.getX() - 1, fieldWidth),
                point.getY()
        );
    }

    public static List<Point2D> getStraightConnectedPoints(Point2D point, int fieldWidth, int fieldHeight) {
        return List.of(
                PointUtils.getPointAbove(point, fieldHeight),
                PointUtils.getPointBelow(point, fieldHeight),
                PointUtils.getPointToLeft(point, fieldWidth),
                PointUtils.getPointToRight(point, fieldWidth)
        );
    }

    public static boolean arePointsStraightConnected(Point2D p1, Point2D p2, int fieldWidth, int fieldHeight) {
        return getStraightConnectedPoints(p1, fieldWidth, fieldHeight).contains(p2);
    }
}
