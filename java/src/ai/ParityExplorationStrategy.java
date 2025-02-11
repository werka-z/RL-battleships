package ai;

import model.Coordinates;
import java.util.*;

public class ParityExplorationStrategy extends ExplorationStrategy {
    private final Random rng;
    private int totalMoves;

    public ParityExplorationStrategy(int boardSize) {
        super(boardSize);
        this.rng = new Random();
        this.totalMoves = 0;
    }

    @Override
    public Coordinates getExplorationShot(Set<Coordinates> shotsFired, double[][] stateActionValues) {
        totalMoves++;

        // first try to get a position following the checkerboard pattern
        return getParityBasedShot(shotsFired); // if no parity shots available, return null to let other strategies handle it
    }

    private Coordinates getParityBasedShot(Set<Coordinates> shotsFired) {
        // calculate current optimal parity based on moves
        boolean preferredParity = (totalMoves % 2 == 0);

        // first try preferred parity
        Coordinates shot = getParityShotWithPreference(shotsFired, preferredParity);
        if (shot != null) return shot;
        return getParityShotWithPreference(shotsFired, !preferredParity);
    }

    private Coordinates getParityShotWithPreference(Set<Coordinates> shotsFired, boolean parity) {
        List<Coordinates> validShots = new ArrayList<>();

        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                Coordinates pos = new Coordinates(i, j);
                if (!shotsFired.contains(pos) && ((i + j) % 2 == (parity ? 0 : 1))) {
                    validShots.add(pos);
                }
            }
        }

        if (!validShots.isEmpty()) return validShots.get(rng.nextInt(validShots.size()));
        return null;
    }
}