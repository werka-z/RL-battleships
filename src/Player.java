import model.Board;
import model.Coordinates;
import model.GameConfig;
import model.GameMode;
import network.Message;
import network.NetworkHandler;

import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
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
        this.myBoard = new Board();
        this.enemyBoard = new Board('?');
        this.random = new Random();
        this.shotsFired = new HashSet<>();

        if (config.getMode() == GameMode.SERVER || config.getMode() == GameMode.CLIENT) {
            try {
                this.network = new NetworkHandler(config.getMode(), config.getPort(), config.getHostName());
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize network", e);
            }
        } else if (config.getMode() == GameMode.AI_USER || config.getMode() == GameMode.BOT_USER) {
            try {
                this.network = new NetworkHandler(GameMode.CLIENT, config.getPort(), config.getHostName());
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize connection", e);
            }
        } else { // BOT_USER mode
            this.network = null;
        }
    }

    private Coordinates getTarget() {
        return switch (config.getMode()) {
            case SERVER, CLIENT -> getUserTarget();
            case AI_USER -> getAITarget();
            case BOT_USER -> getRandomTarget();
            default -> throw new IllegalStateException("Invalid game mode");
        };
    }

    private Coordinates getUserTarget() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                System.out.print("Enter target coordinates (e.g. A1): ");
                String input = scanner.nextLine().trim().toUpperCase();

                if (!input.matches("[A-J][1-9]0?")) {
                    System.out.println("Invalid format - please use letter A-J followed by number 1-10");
                    continue;
                }

                Coordinates coords = new Coordinates(input);

                shotsFired.add(coords);
                return coords;

            } catch (IllegalArgumentException e) {
                System.out.println("Invalid coordinates: " + e.getMessage());
            }
        }
    }

    private Coordinates getAITarget() {
        try {
            String lastResult = "miss"; // default to miss for first shot
            if (lastShot != null) {
                char[][] board = enemyBoard.getBoard();
                char lastMarker = board[lastShot.getRow()][lastShot.getCol()];
                if (lastMarker == '#') {
                    lastResult = "hit";
                    if (isShipSunk(lastShot.getRow(), lastShot.getCol())) {
                        lastResult = "hit and sunk";
                    }
                }
            }

            String message = lastResult;
            if (lastShot != null) {
                message += ";" + lastShot;
            }

            network.sendMessage(new Message("move", message));
            Message response = network.receiveMessage();

            if (response == null || response.coordinates() == null) {
                throw new RuntimeException("No response from AI server");
            }

            Coordinates coords = new Coordinates(response.coordinates());
            if (shotsFired.contains(coords)) {
                throw new RuntimeException("AI returned already used coordinates: " + coords);
            }

            shotsFired.add(coords);
            return coords;

        } catch (Exception e) {
            System.err.println("Error getting AI target: " + e.getMessage());
            return getRandomTarget(); // if AI fails fall back to random
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

            char marker = result.equals("miss") ? '~' : '#';
            myBoard.markShot(shotCoords.getRow(), shotCoords.getCol(), marker);

            Coordinates myShot = getTarget();
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
            } else if (config.getMode() == GameMode.BOT_USER) {
                playBotGame();
                return;
            }

            playGame();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void playBotGame() {
        while (true) {
            // Bot's turn
            Coordinates target = getRandomTarget();
            if (target == null) {
                System.out.println("Game ended - no more valid targets");
                break;
            }

            // Process shot
            String result = checkShot(target);
            System.out.println("Bot fired at " + target + ": " + result);

            // Update board
            char marker = result.equals("miss") ? '~' : '#';
            enemyBoard.markShot(target.getRow(), target.getCol(), marker);

            // Display current state
            System.out.println("\nCurrent board state:");
            enemyBoard.displayBoard();

            // Check for game end
            if (result.equals("last ship sunk")) {
                System.out.println("Game Over - Bot wins!");
                break;
            }
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