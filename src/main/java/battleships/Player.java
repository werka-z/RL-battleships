package main.java.battleships;

import main.java.battleships.model.Board;
import main.java.battleships.model.Coordinates;
import main.java.battleships.model.GameConfig;
import main.java.battleships.model.GameMode;
import main.java.battleships.network.Message;
import main.java.battleships.network.NetworkHandler;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Player {
    private final GameConfig config;
    private final Board myBoard;
    private final Board enemyBoard;
    private final NetworkHandler network;
    private final Random random;
    private final Set<Coordinates> shotsFired;
    private Coordinates lastShot;

    public Player(GameConfig config) {
        this.config = config;
        this.myBoard = new Board(config.getMapFile());
        this.enemyBoard = new Board('?');
        this.random = new Random();
        this.shotsFired = new HashSet<>();

        try {
            this.network = new NetworkHandler(config.getMode(), config.getPort(), config.getHostName());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize network", e);
        }
    }

    private Coordinates getRandomTarget() {
        if (shotsFired.size() >= 100) { // all positions tried
            return null;
        }

        Coordinates coords;
        do {
            int row = random.nextInt(10);
            int col = random.nextInt(10);
            coords = new Coordinates(row, col);
        } while (shotsFired.contains(coords));

        shotsFired.add(coords);
        return coords;
    }

    private void handleShot(String coords) {
        try {
            Coordinates shotCoords = new Coordinates(coords);
            String result = checkShot(shotCoords);

            if (result.equals("hit and sunk")) {
                network.sendMessage(new Message(result, null));
                handleGameEnd(false);
                return;
            }

            char marker = result.equals("miss") ? '~' : '@';
            myBoard.markShot(shotCoords.getRow(), shotCoords.getCol(), marker);

            Coordinates myShot = getRandomTarget();
            if (myShot == null) {throw new RuntimeException("No valid shots remaining");}
            network.sendMessage(new Message(result, myShot.toString()));
            new Message(result, myShot.toString());

            lastShot = myShot;
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid coordinates: " + e.getMessage());
        }
    }

    private void handleGameEnd(boolean won) {
        if (won){
            System.out.print("Won!\n");
            enemyBoard.changeUnknownsToSea();
            enemyBoard.displayBoard();
        } else {
            System.out.print("Lost...\n");
            enemyBoard.changeMissesToSea();
            enemyBoard.displayBoard();
        }
        System.out.println();
        myBoard.displayBoard();
    }

    public void start() {
        try {
            myBoard.displayBoard();
            if (config.getMode() == GameMode.CLIENT) {
                network.sendMessage(new Message("start", "A1"));
            }

            playGame();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void playGame() {
        while (true) {
            Message message = network.receiveMessage();
            if (message == null) continue;

            System.out.println("Received: " + message.format().trim());

            if (message.command().equals("last ship sunk")) {
                handleGameEnd(true);
                return;
            }

            if (lastShot != null) {
                switch (message.command()) {
                    case "miss":
                        enemyBoard.markShot(lastShot.getRow(), lastShot.getCol(), '~');
                        break;
                    case "hit":
                        enemyBoard.markShot(lastShot.getRow(), lastShot.getCol(), '#');
                        break;
                    case "hit and sunk":
                        enemyBoard.markShot(lastShot.getRow(), lastShot.getCol(), '#');
                        enemyBoard.markBorders(lastShot.getRow(), lastShot.getCol());
                        break;
                }
                lastShot = null;
            }

            String coords = message.coordinates();
            if (coords != null) {
                handleShot(coords);
            }
        }
    }

    private String checkShot(Coordinates coords) {
        char[][] board = myBoard.getBoard();
        if (board[coords.getRow()][coords.getCol()] == '#' || board[coords.getRow()][coords.getCol()] == '@') {
            board[coords.getRow()][coords.getCol()] = '@';
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

    private boolean isLastShip() {
        char[][] board = myBoard.getBoard();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (board[i][j] == '#') return false;

            }
        }
        return true;
    }
    private boolean isShipSunk(int row, int col) {
        char[][] board = myBoard.getBoard();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int newRow = row + i;
                int newCol = col + j;
                if (newRow >= 0 && newRow < 10 && newCol >= 0 && newCol < 10) {
                    if (board[newRow][newCol] == '#') return false;
                }
            }
        }
        return true;
    }
}