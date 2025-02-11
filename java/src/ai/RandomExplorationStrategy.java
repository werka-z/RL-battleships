package ai;

import model.Coordinates;
import java.util.*;

public class RandomExplorationStrategy extends ExplorationStrategy {
    private final Random rng;

    public RandomExplorationStrategy(int boardSize) {
        super(boardSize);
        this.rng = new Random();
    }

    @Override
    public Coordinates getExplorationShot(Set<Coordinates> shotsFired, double[][] stateActionValues) {
        // trying random positions with a max number of attempts
        int attempts = 0;
        int maxAttempts = boardSize * boardSize;

        while (attempts < maxAttempts) {
            int row = rng.nextInt(boardSize);
            int col = rng.nextInt(boardSize);
            Coordinates pos = new Coordinates(row, col);

            if (!shotsFired.contains(pos)) {
                return pos;
            }
            attempts++;
        }
        // if random attempts fail
        return getSystematicShot(shotsFired);
    }

    private Coordinates getSystematicShot(Set<Coordinates> shotsFired) {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                Coordinates pos = new Coordinates(i, j);
                if (!shotsFired.contains(pos)) {
                    return pos;
                }
            }
        }
        return null;
    }
}