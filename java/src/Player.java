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
    private BattleshipAI ai;

    public Player(GameConfig config) {
        this.config = config;
        this.myBoard = new Board();
        this.enemyBoard = new Board('?');
        this.random = new Random();
        this.shotsFired = new HashSet<>();

        if (config.getMode() == GameMode.AI_USER){
            this.ai = new BattleshipAI();
        }

        if (config.getMode() == GameMode.SERVER || config.getMode() == GameMode.CLIENT) {
            try {
                this.network = new NetworkHandler(config.getMode(), config.getPort(), config.getHostName());
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize network", e);
            }
        } else {
            this.network = null;
        }
    }

    private Coordinates getTarget() {
        return switch (config.getMode()) {
            case SERVER, CLIENT -> getUserTarget();
            case AI_USER -> getAITarget();
            case BOT_USER -> getRandomTarget();
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
        if (ai == null) {
            throw new IllegalStateException("AI not initialized");
        }

        Coordinates target = ai.getNextShot();
        if (target == null || shotsFired.contains(target)) {
            return getRandomTarget();
        }

        shotsFired.add(target);
        lastShot = target;
        return target;
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
            String result = myBoard.checkShot(shotCoords);

            if (config.getMode() == GameMode.AI_USER && ai != null) {
                ai.updateFromResult(result);
            }

            if (result.equals("hit and sunk")) {
                network.sendMessage(new Message(result, null));
                handleGameEnd(false);
                return;
            }

            char marker = result.equals("miss") ? '~' : '#';
            myBoard.markShot(shotCoords.getRow(), shotCoords.getCol(), marker);

            System.out.println("\nYour board:");
            myBoard.displayBoard();
            System.out.println("\nEnemy board:");
            enemyBoard.displayBoard();

            Coordinates myShot = getTarget();
            if (myShot == null) {
                throw new RuntimeException("No valid shots remaining");
            }
            network.sendMessage(new Message(result, myShot.toString()));

            lastShot = myShot;
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid coordinates: " + e.getMessage());
        }
    }

    public void start() {
        try {
//            System.out.println("\nYour board:");
//            myBoard.displayBoard();
            System.out.println("\nEnemy's board:");
            enemyBoard.displayBoard();

            if (config.getMode() == GameMode.CLIENT) {
                Coordinates firstMove = getTarget();
                network.sendMessage(new Message("start", firstMove.toString()));
            } else if (config.getMode() == GameMode.BOT_USER) {
                playAgainstBot();
                return;
            } else if (config.getMode() == GameMode.AI_USER) {
                playAIGame();
                return;
            }


            playGame();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void displayGameState() {
        System.out.println("\nYour board:");
        myBoard.displayBoard();
        System.out.println("\nEnemy board:");
        enemyBoard.displayBoard();
    }

    private void processPlayerTurn(Coordinates target) {
        String result = myBoard.checkShot(target);
        System.out.println("Result: " + result);

        char marker = result.equals("miss") ? '~' : '#';
        enemyBoard.markShot(target.getRow(), target.getCol(), marker);

        if (result.equals("last ship sunk")) {
            System.out.println("Congratulations! You win!");
        }
    }

    private void processOpponentTurn(Coordinates target, String player) {
        if (target == null) {
            System.out.println("Game ended - no more valid targets");
            return;
        }

        String result = myBoard.checkShot(target);
        System.out.println(player + " fired at " + target + ": " + result);

        if (config.getMode() == GameMode.AI_USER && ai != null) {
            ai.updateFromResult(result);
        }

        char marker = result.equals("miss") ? '~' : '#';
        myBoard.markShot(target.getRow(), target.getCol(), marker);

        if (result.equals("last ship sunk")) {
            System.out.println("Game Over - " + player + " wins!");
        }
    }

    private void playAgainstBot() {
        while (true) {
            displayGameState();

            // human's turn
            System.out.print("\nYour turn - ");
            Coordinates target = getUserTarget();
            processPlayerTurn(target);
            if (myBoard.isLastShip()) break;

            // bot's turn
            processOpponentTurn(getRandomTarget(), "Bot");
            if (myBoard.isLastShip()) break;
        }
    }

    private void playAIGame() {
        while (true) {
            displayGameState();

            System.out.println("\nYour turn - ");
            Coordinates target = getUserTarget();
            processPlayerTurn(target);
            if (myBoard.isLastShip()) break;

            processOpponentTurn(getAITarget(), "AI");
            if (myBoard.isLastShip()) break;
        }
    }

    private void handleGameEnd(boolean won) {
        if (won) {
            System.out.println("Congratulations! You won!");
        } else {
            System.out.println("Game Over - You lost!");
        }
        System.out.println("\nFinal board states:");
        System.out.println("\nYour board:");
        myBoard.displayBoard();
        System.out.println("\nEnemy board:");
        enemyBoard.displayBoard();
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

}