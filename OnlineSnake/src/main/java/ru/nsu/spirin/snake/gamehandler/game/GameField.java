package ru.nsu.spirin.snake.gamehandler.game;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.config.ConfigValidator;
import ru.nsu.spirin.snake.gamehandler.Point2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public final class GameField {
    private final List<Cell> field;
    private final List<Cell> emptyCells;
    private final @Getter int width;
    private final @Getter int height;

    private final Random random = new Random();

    public GameField(int width, int height) {
        validateFieldSizes(width, height);
        this.field = new ArrayList<>(width * height);
        this.emptyCells = new ArrayList<>(width * height);
        for (int row = 0; row < height; row++){
            for (int col = 0; col < width; col++){
                Cell cell = new Cell(col, row);
                this.field.add(cell);
                this.emptyCells.add(cell);
            }
        }
        this.width = width;
        this.height = height;
    }

    private void validateFieldSizes(int width, int height){
        if (width < ConfigValidator.MIN_FIELD_WIDTH || width > ConfigValidator.MAX_FIELD_WIDTH){
            throw new IllegalArgumentException("Width not from valid interval: ["
                    + ConfigValidator.MIN_FIELD_WIDTH + ", " + ConfigValidator.MAX_FIELD_WIDTH + "]");
        }
        if (height < ConfigValidator.MIN_FIELD_HEIGHT || height > ConfigValidator.MAX_FIELD_HEIGHT){
            throw new IllegalArgumentException("Height not from valid interval: ["
                    + ConfigValidator.MIN_FIELD_HEIGHT + ", " + ConfigValidator.MAX_FIELD_HEIGHT + "]");
        }
    }

    public Cell get(int row, int col){
        return new Cell(accessToCell(row, col));
    }

    private Cell accessToCell(int row, int col){
        int y = (row < 0) ? this.height + row : row % this.height;
        int x = (col < 0) ? this.width + col : col % this.width;
        return this.field.get(y * this.width + x);
    }

    public void set(int row, int col, CellType type) {
        Cell cell = accessToCell(row, col);
        if (CellType.EMPTY.equals(type)) {
            if (!CellType.EMPTY.equals(cell.getType())) {
                this.emptyCells.add(cell);
            }
        }
        else {
            this.emptyCells.remove(cell);
        }
        cell.setType(type);
    }

    public void set(@NotNull Point2D point, @NotNull CellType type) {
        Objects.requireNonNull(point, "Point2D cant be null");
        set(point.getY(), point.getX(), type);
    }

    public int getEmptyCellsNumber() {
        return this.emptyCells.size();
    }

    Optional<Cell> findCenterOfSquareWithOutSnake(int squareSize) {
        return this.field.stream()
                    .filter(cell -> isSquareWithoutSnake(cell, squareSize))
                    .findFirst();
    }

    private boolean isSquareWithoutSnake(Cell squareCenter, int squareSize) {
        final int centerOffset = squareSize / 2;
        for (int yCenterOffset = -centerOffset; yCenterOffset <= centerOffset; yCenterOffset++) {
            for (int xCenterOffset = -centerOffset; xCenterOffset <= centerOffset; xCenterOffset++) {
                Cell cell = accessToCell(
                        squareCenter.getY() + yCenterOffset,
                        squareCenter.getX() + xCenterOffset
                );
                if (CellType.SNAKE.equals(cell.getType())) {
                    return false;
                }
            }
        }
        return true;
    }

    public Optional<Cell> findRandomEmptyCell() {
        return this.emptyCells.isEmpty() ?
                Optional.empty() :
                Optional.of(this.emptyCells.get(this.random.nextInt(this.emptyCells.size() - 1)));
    }
}
