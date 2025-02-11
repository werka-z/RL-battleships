package ai;

import model.Coordinates;
import java.util.*;

public class QLearningAgent {
    private final double[][] stateActionValues;
    private final Set<Coordinates> shotsFired;
    private final CompositeExplorationStrategy explorationStrategy;
    private Coordinates lastShot;

    private final List<Coordinates> currentShipHits;
    private final PriorityQueue<Target> potentialTargets;
    private final double learningRate;
    private final double discountFactor;

    private static final int BOARD_SIZE = 10;
    private static final int[] SHIP_SIZES = {5, 4, 3, 3, 2};

    public QLearningAgent() {
        this.stateActionValues = new double[BOARD_SIZE][BOARD_SIZE];
        this.shotsFired = new HashSet<>();
        this.currentShipHits = new ArrayList<>();
        this.potentialTargets = new PriorityQueue<>();
        this.explorationStrategy = new CompositeExplorationStrategy(BOARD_SIZE);
        this.learningRate = 0.1;
        this.discountFactor = 0.9;

        initializeQValues();
    }

    private void initializeQValues() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                stateActionValues[i][j] = (i + j) % 2 == 0 ? 0.1 : 0.0;
            }
        }
    }

    public Coordinates getNextShot() {
        // check high-probability targets from previous hits
        if (!potentialTargets.isEmpty()) {
            Target bestTarget = null;
            while (!potentialTargets.isEmpty() && bestTarget == null) {
                Target candidate = potentialTargets.poll();
                if (!shotsFired.contains(candidate.coordinates)) {
                    bestTarget = candidate;
                }
            }
            if (bestTarget != null) {
                return executeShot(bestTarget.coordinates);
            }
        }

        // use exploration strategy
        Coordinates explorationShot = explorationStrategy.getExplorationShot(shotsFired, stateActionValues);
        if (explorationShot != null) return executeShot(explorationShot);
        else return executeShot(getBestQValuePosition()); // fall back to best Q-value
    }

    private Coordinates executeShot(Coordinates shot) {
        if (shot != null) {
            lastShot = shot;
            shotsFired.add(shot);
        }
        return shot;
    }

    private Coordinates getBestQValuePosition() {
        double maxQ = Double.NEGATIVE_INFINITY;
        Coordinates bestPos = null;

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                Coordinates pos = new Coordinates(i, j);
                if (!shotsFired.contains(pos) && stateActionValues[i][j] > maxQ) {
                    maxQ = stateActionValues[i][j];
                    bestPos = pos;
                }
            }
        }
        return bestPos;
    }

    public void updateFromResult(String result) {
        if (lastShot == null) return;

        double reward = switch(result) {
            case "hit" -> {
                handleHit(lastShot);
                yield 1.0;
            }
            case "hit and sunk" -> {
                handleSunkShip(lastShot);
                yield 2.0;
            }
            case "last ship sunk" -> {
                handleSunkShip(lastShot);
                yield 3.0;
            }
            default -> -0.1; // miss
        };
        updateQValue(lastShot, reward);
    }

    private void updateQValue(Coordinates action, double reward) {
        int row = action.getRow();
        int col = action.getCol();

        double maxFutureQ = getMaxFutureQValue(row, col);
        stateActionValues[row][col] += learningRate *
                (reward + discountFactor * maxFutureQ - stateActionValues[row][col]);
    }

    private double getMaxFutureQValue(int row, int col) {
        double maxQ = 0;
        for (int shipSize : SHIP_SIZES) {
            // Check horizontal possibility
            maxQ = Math.max(maxQ, getDirectionalQValue(row, col, shipSize, true));
            // Check vertical possibility
            maxQ = Math.max(maxQ, getDirectionalQValue(row, col, shipSize, false));
        }
        return maxQ;
    }

    private double getDirectionalQValue(int row, int col, int shipSize, boolean horizontal) {
        double sum = 0;
        int count = 0;

        for (int offset = -shipSize + 1; offset < shipSize; offset++) {
            int checkRow = horizontal ? row : row + offset;
            int checkCol = horizontal ? col + offset : col;

            if (checkRow >= 0 && checkRow < BOARD_SIZE &&
                    checkCol >= 0 && checkCol < BOARD_SIZE) {
                sum += stateActionValues[checkRow][checkCol];
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }

    private void handleHit(Coordinates hit) {
        currentShipHits.add(hit);
        updatePotentialTargets(hit);
    }

    private void handleSunkShip(Coordinates lastHit) {
        currentShipHits.add(lastHit);
        int shipSize = currentShipHits.size();

        boolean isHorizontal = currentShipHits.stream()
                .map(Coordinates::getRow)
                .distinct()
                .count() == 1;

        updateOrientationQValues(isHorizontal, shipSize);
        currentShipHits.clear();
        potentialTargets.clear();
    }

    private void updateOrientationQValues(boolean isHorizontal, int shipSize) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                Coordinates pos = new Coordinates(i, j);
                if (!shotsFired.contains(pos)) {
                    if (canFitShip(i, j, shipSize, isHorizontal)) {
                        stateActionValues[i][j] *= 1.2;
                    }
                }
            }
        }
    }

    private boolean canFitShip(int row, int col, int size, boolean horizontal) {
        if (horizontal) {
            if (col + size > BOARD_SIZE) return false;
            for (int j = col; j < col + size; j++) {
                if (shotsFired.contains(new Coordinates(row, j))) return false;
            }
        } else {
            if (row + size > BOARD_SIZE) return false;
            for (int i = row; i < row + size; i++) {
                if (shotsFired.contains(new Coordinates(i, col))) return false;
            }
        }
        return true;
    }

    private void updatePotentialTargets(Coordinates hit) {
        int row = hit.getRow();
        int col = hit.getCol();
        Direction direction = calculateDirection();

        if (direction == Direction.UNKNOWN || direction == Direction.HORIZONTAL) {
            addPotentialTarget(row, col - 1);
            addPotentialTarget(row, col + 1);
        }
        if (direction == Direction.UNKNOWN || direction == Direction.VERTICAL) {
            addPotentialTarget(row - 1, col);
            addPotentialTarget(row + 1, col);
        }
    }

    private Direction calculateDirection() {
        if (currentShipHits.size() < 2) return Direction.UNKNOWN;

        boolean sameRow = currentShipHits.stream()
                .map(Coordinates::getRow)
                .distinct()
                .count() == 1;

        boolean sameCol = currentShipHits.stream()
                .map(Coordinates::getCol)
                .distinct()
                .count() == 1;

        if (sameRow) return Direction.HORIZONTAL;
        if (sameCol) return Direction.VERTICAL;
        return Direction.UNKNOWN;
    }

    private void addPotentialTarget(int row, int col) {
        if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE) {
            Coordinates pos = new Coordinates(row, col);
            if (!shotsFired.contains(pos)) {
                potentialTargets.offer(new Target(pos, stateActionValues[row][col]));
            }
        }
    }

    private enum Direction {
        HORIZONTAL, VERTICAL, UNKNOWN
    }

    private record Target(Coordinates coordinates, double priority) implements Comparable<Target> {
        @Override
            public int compareTo(Target other) {
                return Double.compare(other.priority, this.priority);
            }
        }
}