
import model.Coordinates;

import java.util.*;

public class BattleshipAI {
    private final double[][] qValues;  // learned values for each position
    private final Set<Coordinates> shotsFired;
    private Coordinates lastShot;
    private final Random random;

    // track hits for targeting nearby squares
    private final Set<Coordinates> hits;
    private final Set<Coordinates> potentialTargets;

    public BattleshipAI() {
        this.qValues = new double[10][10];
        this.shotsFired = new HashSet<>();
        this.hits = new HashSet<>();
        this.potentialTargets = new HashSet<>();
        this.random = new Random();

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                // initialize Q-vals with small random vals
                qValues[i][j] = random.nextDouble() * 0.1;
            }
        }
    }

    public Coordinates getNextShot() {
        // If we have potential targets from previous hits, prioritize those
        if (!potentialTargets.isEmpty()) {
            Coordinates target = getBestPotentialTarget();
            if (target != null) {
                lastShot = target;
                shotsFired.add(target);
                return target;
            }
        }

        if (random.nextDouble() < 0.1) {  // 10% exploration (epsilon-greedy)
            Coordinates randomShot = getRandomUntriedPosition();
            if (randomShot != null) {
                lastShot = randomShot;
                shotsFired.add(randomShot);
                return randomShot;
            }
        }

        Coordinates bestShot = getBestQValuePosition();
        if (bestShot != null) {
            lastShot = bestShot;
            shotsFired.add(bestShot);
            return bestShot;
        }

        return null;  // if no valid shots remaining
    }

    private Coordinates getBestPotentialTarget() {
        if (potentialTargets.isEmpty()) return null;

        // Find target with highest Q-value among potential targets
        double maxQ = Double.NEGATIVE_INFINITY;
        Coordinates bestTarget = null;

        for (Coordinates target : potentialTargets) {
            if (!shotsFired.contains(target)) {
                double q = qValues[target.getRow()][target.getCol()];
                if (q > maxQ) {
                    maxQ = q;
                    bestTarget = target;
                }
            }
        }

        if (bestTarget != null) {
            potentialTargets.remove(bestTarget);
        }
        return bestTarget;
    }

    private Coordinates getRandomUntriedPosition() {
        List<Coordinates> available = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Coordinates pos = new Coordinates(i, j);
                if (!shotsFired.contains(pos)) {
                    available.add(pos);
                }
            }
        }
        return available.isEmpty() ? null : available.get(random.nextInt(available.size()));
    }

    private Coordinates getBestQValuePosition() {
        double maxQ = Double.NEGATIVE_INFINITY;
        Coordinates bestPos = null;

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Coordinates pos = new Coordinates(i, j);
                if (!shotsFired.contains(pos) && qValues[i][j] > maxQ) {
                    maxQ = qValues[i][j];
                    bestPos = pos;
                }
            }
        }
        return bestPos;
    }

    public void updateFromResult(String result) {
        if (lastShot == null) return;

        double reward;
        switch (result) {
            case "hit":
                reward = 1.0;
                handleHit(lastShot);
                break;
            case "hit and sunk":
                reward = 2.0;
                handleSunkShip(lastShot);
                break;
            case "last ship sunk":
                reward = 3.0;
                handleSunkShip(lastShot);
                break;
            default:  // miss
                reward = -0.1;
                break;
        }

        // Update Q-value for last action
        int row = lastShot.getRow();
        int col = lastShot.getCol();

        // Find max Q-value of neighboring positions
        double maxNeighborQ = getMaxNeighborQValue(row, col);

        // Q-learning update formula
        double learningRate = 0.1;
        double discountFactor = 0.9;
        qValues[row][col] += learningRate * (reward + discountFactor * maxNeighborQ - qValues[row][col]);
    }

    private double getMaxNeighborQValue(int row, int col) {
        double maxQ = Double.NEGATIVE_INFINITY;
        int[][] directions = {{-1,0}, {1,0}, {0,-1}, {0,1}};

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (newRow >= 0 && newRow < 10 && newCol >= 0 && newCol < 10) {
                maxQ = Math.max(maxQ, qValues[newRow][newCol]);
            }
        }

        return maxQ == Double.NEGATIVE_INFINITY ? 0 : maxQ;
    }

    private void handleHit(Coordinates hit) {
        hits.add(hit);

        // Add adjacent positions to potential targets
        int row = hit.getRow();
        int col = hit.getCol();
        int[][] directions = {{-1,0}, {1,0}, {0,-1}, {0,1}};

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (newRow >= 0 && newRow < 10 && newCol >= 0 && newCol < 10) {
                potentialTargets.add(new Coordinates(newRow, newCol));
            }
        }
    }

    private void handleSunkShip(Coordinates lastHit) {
        hits.add(lastHit);
        potentialTargets.clear();  // Clear potential targets as ship is sunk

        // Update Q-values around sunken ship
        for (Coordinates hit : hits) {
            int row = hit.getRow();
            int col = hit.getCol();

            // Increase Q-values in line with other hits
            updateLineQValues(row, col);
        }
        hits.clear();
    }

    private void updateLineQValues(int row, int col) {
        // Update Q-values in horizontal and vertical lines
        for (int i = 0; i < 10; i++) {
            if (i != col) qValues[row][i] *= 1.1;  // Horizontal
            if (i != row) qValues[i][col] *= 1.1;  // Vertical
        }
    }
}