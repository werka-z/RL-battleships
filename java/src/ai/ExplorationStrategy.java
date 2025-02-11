package ai;

import model.Coordinates;
import java.util.Set;

public abstract class ExplorationStrategy {
    protected final int boardSize;

    protected ExplorationStrategy(int boardSize) {
        this.boardSize = boardSize;
    }

    public abstract Coordinates getExplorationShot(Set<Coordinates> shotsFired, double[][] stateActionValues);

}