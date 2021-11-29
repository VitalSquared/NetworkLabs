package ru.nsu.spirin.snake.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@RequiredArgsConstructor
public final class Cell {
    private final @NotNull @Getter Point2D point;
    private @NotNull @Getter @Setter CellType type;

    public Cell(int x, int y, @NotNull CellType type) {
        this.point = new Point2D(x, y);
        this.type = Objects.requireNonNull(type, "Cell type cant be null");
    }

    public Cell(int x, int y) {
        this(x, y, CellType.EMPTY);
    }

    public Cell(@NotNull Cell cell) {
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
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Cell other = (Cell) object;
        return this.point.equals(other.point) && (this.type == other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(point, type);
    }
}
