package model;

import java.util.*;

public class Board {

    private final int BOARD_SIZE = 10;
    private final char EMPTY = '~';
    private final char SHIP = '#';
    private final char[][] board;
    private final Random random = new Random();
    private Map<Integer, List<int[][]>> shapes;
    private final int[] shipSizes = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};

    public Board() {
        board = new char[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {Arrays.fill(board[i], EMPTY);}
        initializeShapes();
        generateMap();
    }

    private void initializeShapes(){
        this.shapes = new HashMap<>();
        shapes.put(4, Arrays.asList(
                new int[][] {{0,0}, {0,1}, {0,2}, {0,3}}, // line
                new int[][] {{0,0}, {0,1}, {0,2}, {1,0}}, // L
                new int[][] {{0,0}, {0,1}, {0,2}, {1,2}}, // mirrored L
                new int[][] {{0,0}, {0,1}, {0,2}, {1,1}}, // T
                new int[][] {{0,0}, {0,1}, {1,0}, {1,1}} // square
        ));
        shapes.put(3, Arrays.asList(
                new int[][] {{0,0}, {0,1}, {0,2}}, // line
                new int[][] {{0,0}, {1,0}, {1,1}}, // L
                new int[][] {{0,1}, {1,0}, {1,1}}, // mirrored L
                new int[][] {{0,0}, {0,1}, {1,1}}  // mirrored backwards L
        ));
        shapes.put(2, Collections.singletonList(
                new int[][] {{0,0}, {0,1}}
        ));
        shapes.put(1, Collections.singletonList(
                new int[][] {{0,0}}
        ));
    }

    public Board(char c){
        board = new char[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {Arrays.fill(board[i], c);}
        initializeShapes();
    }

    public void placeShip(int size) {
        List<int[][]> possibleShapes = shapes.get(size);
        int[][] shape = possibleShapes.get(random.nextInt(possibleShapes.size()));

        int ATTEMPTS = 100;
        for (int i = 0; i < ATTEMPTS; i++) {
            int row = random.nextInt(BOARD_SIZE);
            int col = random.nextInt(BOARD_SIZE);
            boolean canRotate = random.nextBoolean();

            if (canPlaceShipHere(row, col, shape, canRotate)) {
                for (int[] coords : shape) {
                    int finalRow = row + (canRotate ? coords[1] : coords[0]);
                    int finalCol = col + (canRotate ? coords[0] : coords[1]);
                    board[finalRow][finalCol] = SHIP;
                }
                return;
            }
        }
    }

    private boolean canPlaceShipHere(int startRow, int startCol, int[][] shape, boolean rotate) {
        for (int[] coords : shape) {
            int row = startRow + (rotate ? coords[1] : coords[0]);
            int col = startCol + (rotate ? coords[0] : coords[1]);

            if (row < 0 || col < 0 || row >= BOARD_SIZE || col >= BOARD_SIZE) {return false;}

            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    int newRow = row + i;
                    int newCol = col + j;
                    if (newRow >= 0 && newRow < BOARD_SIZE && newCol >= 0 && newCol < BOARD_SIZE && board[newRow][newCol] == SHIP) {return false;}
                }
            }
        }
        return true;
    }

    public void generateMap(){
        for (int i = 0; i < BOARD_SIZE; i++) {Arrays.fill(board[i], EMPTY);}
        for (int size : shipSizes) {placeShip(size);}
    }

    public char[][] getBoard() {
        return board;
    }

    public boolean hasShipsLeft() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == SHIP) {
                    return true;
                }
            }
        }
        return false;
    }

    public void markShot(int row, int col, char mark) {
        if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE) {
            board[row][col] = mark;
        }
    }

    public void displayBoard() {
        char[][] boardArray = getBoard();
        System.out.println("  A B C D E F G H I J");
        for (int i = 0; i < 10; i++) {
            System.out.print(i+1);
            if (i!=9) System.out.print(" ");
            for (int j = 0; j < 10; j++) {
                System.out.print(boardArray[i][j] + " ");
            }
            System.out.println();
        }
    }

    public void changeUnknownsToSea(){
        for (int i = 0; i < BOARD_SIZE; i++){
            for (int j = 0; j < BOARD_SIZE; j++){
                if (board[i][j] != SHIP){ board[i][j] = EMPTY;}
            }
        }
    }

    public void changeMissesToSea(){
        for (int i = 0; i < BOARD_SIZE; i++){
            for (int j = 0; j < BOARD_SIZE; j++){
                if (board[i][j] == '~'){ board[i][j] = EMPTY;}
            }
        }
    }

    public void markBorders(int row, int col) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int newRow = row + i;
                int newCol = col + j;
                if (newRow >= 0 && newRow < BOARD_SIZE &&
                        newCol >= 0 && newCol < BOARD_SIZE &&
                        board[newRow][newCol] == '~') {
                    board[newRow][newCol] = '~';
                }
            }
        }
    }

    public String checkShot(Coordinates coords) {
        if (board[coords.getRow()][coords.getCol()] == '#') {
            board[coords.getRow()][coords.getCol()] = '#';
            if (isLastShip()) {
                return "last ship sunk";
            }
            if (isShipSunk(coords.getRow(), coords.getCol())) {
                return "hit and sunk";
            }
            return "hit";
        }
        board[coords.getRow()][coords.getCol()] = '~';
        return "miss";
    }

    public boolean isLastShip() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == '#') return false;
            }
        }
        return true;
    }

    public boolean isShipSunk(int row, int col) {
        Set<Coordinates> checked = new HashSet<>();
        Set<Coordinates> shipCells = new HashSet<>();
        findConnectedShipCells(row, col, checked, shipCells);

        for (Coordinates cell : shipCells) {
            if (board[cell.getRow()][cell.getCol()] == '#') {
                return false;
            }
        }
        return true;
    }

    private void findConnectedShipCells(int row, int col,
                                        Set<Coordinates> checked,
                                        Set<Coordinates> shipCells) {
        Coordinates current = new Coordinates(row, col);
        if (checked.contains(current)) return;

        checked.add(current);
        if (board[row][col] != '#' && board[row][col] != 'X') return;

        shipCells.add(current);

        int[][] directions = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (newRow >= 0 && newRow < BOARD_SIZE &&
                    newCol >= 0 && newCol < BOARD_SIZE) {
                findConnectedShipCells(newRow, newCol, checked, shipCells);
            }
        }
    }
}