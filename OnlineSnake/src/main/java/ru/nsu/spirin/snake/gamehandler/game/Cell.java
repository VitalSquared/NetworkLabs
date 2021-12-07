package ru.nsu.spirin.snake.gamehandler.game;

import lombok.Getter;
import lombok.Setter;
import ru.nsu.spirin.snake.gamehandler.Point2D;

import java.util.Objects;

public final class Cell {
    private final @Getter Point2D point;
    private @Getter @Setter CellType type;

    public Cell(int x, int y, CellType type) {
        this.point = new Point2D(x, y);
        this.type = type;
    }

    public Cell(Point2D point, CellType type) {
        this.point = point;
        this.type = type;
    }

    public Cell(int x, int y) {
        this(x, y, CellType.EMPTY);
    }

    public Cell(Cell cell) {
        this(cell.point, cell.getType());
    }

    public int getX() {
        return this.point.getX();
    }

    public int getY() {
        return this.point.getY();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (null == object || getClass() != object.getClass()) {
            return false;
        }
        Cell other = (Cell) object;
        return this.point.equals(other.point) && (this.type == other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.point, this.type);
    }
}
