package ai;

public record ExplorationConfig(double initialRate, double decayRate, double minRate, int decayInterval) {

    public static ExplorationConfig getDefault() {
        return new ExplorationConfig(0.2, 0.995, 0.05, 10);
    }
}