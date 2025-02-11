package ai;

import model.Coordinates;
import java.util.*;

public class CompositeExplorationStrategy extends ExplorationStrategy {
    private final List<ExplorationStrategy> strategies;
    private final Random rng;
    private final ExplorationConfig config;
    private double currentExplorationRate;
    private int totalMoves;

    public CompositeExplorationStrategy(int boardSize) {
        this(boardSize, ExplorationConfig.getDefault());
    }

    public CompositeExplorationStrategy(int boardSize, ExplorationConfig config) {
        super(boardSize);
        this.strategies = new ArrayList<>();
        this.rng = new Random();
        this.config = config;
        this.currentExplorationRate = config.initialRate();
        this.totalMoves = 0;

        // strategies in order of preference
        strategies.add(new ParityExplorationStrategy(boardSize));
        strategies.add(new RandomExplorationStrategy(boardSize));
    }

    @Override
    public Coordinates getExplorationShot(Set<Coordinates> shotsFired, double[][] stateActionValues) {
        totalMoves++;
        updateExplorationRate();

        if (rng.nextDouble() >= currentExplorationRate) { // letting the main strategy handle it
            return null;
        }

        for (ExplorationStrategy strategy : strategies) { // trying each strategy until getting a valid shot
            Coordinates shot = strategy.getExplorationShot(shotsFired, stateActionValues);
            if (shot != null) {
                return shot;
            }
        }
        return null;
    }

    private void updateExplorationRate() {
        if (totalMoves % config.decayInterval() == 0) {
            currentExplorationRate = Math.max(
                    config.minRate(),
                    currentExplorationRate * config.decayRate()
            );
        }
    }
}