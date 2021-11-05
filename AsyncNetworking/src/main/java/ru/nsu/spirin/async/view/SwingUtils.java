package ru.nsu.spirin.async.view;

import java.awt.GridLayout;

public final class SwingUtils {
    private static final int VERTICAL_BUTTONS_IN_ROW = 1;
    private static final int HORIZONTAL_BUTTONS_IN_COLUMN = 1;
    private static final int HORIZONTAL_GAP = 0;
    private static final int VERTICAL_GAP = 10;

    public static GridLayout createVerticalGridLayout(int numberOfButtons) {
        return new GridLayout(numberOfButtons, VERTICAL_BUTTONS_IN_ROW, HORIZONTAL_GAP, VERTICAL_GAP);
    }

    public static GridLayout createVerticalGridLayoutNoGaps(int numberOfButtons) {
        return new GridLayout(numberOfButtons, VERTICAL_BUTTONS_IN_ROW, 0, 0);
    }

    public static GridLayout createHorizontalGridLayoutNoGaps(int numberOfButtons) {
        return new GridLayout(HORIZONTAL_BUTTONS_IN_COLUMN, numberOfButtons, 0, 0);
    }

    public static GridLayout createTableGridLayout(int rows, int cols) {
        return new GridLayout(rows, cols, HORIZONTAL_GAP, VERTICAL_GAP);
    }
}
