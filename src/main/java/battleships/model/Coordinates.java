package main.java.battleships.model;

import java.util.Objects;

public class Coordinates {
    private final int row;
    private final int col;

    public Coordinates(String input) {
        if (input == null || input.length() < 2) {
            throw new IllegalArgumentException("Invalid coordinates format");
        }

        char colChar = input.toUpperCase().charAt(0);
        String rowStr = input.substring(1);

        if (colChar < 'A' || colChar > 'J') {
            throw new IllegalArgumentException("Column must be between A and J");
        }

        try {
            int rowNum = Integer.parseInt(rowStr);
            if (rowNum < 1 || rowNum > 10) {
                throw new IllegalArgumentException("Row must be between 1 and 10");
            }
            this.row = rowNum - 1;
            this.col = colChar - 'A';
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid row number");
        }
    }

    public Coordinates(int row, int col) {
        if (row < 0 || row >= 10 || col < 0 || col >= 10) {
            throw new IllegalArgumentException("Coordinates out of bounds");
        }
        this.row = row;
        this.col = col;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinates that)) return false;
        return row == that.row && col == that.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return String.format("%c%d", (char)('A' + col), row + 1);
    }
}