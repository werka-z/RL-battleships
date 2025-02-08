package model;

public class GameConfig {
    private static final int AI_PORT = 12345;
    private GameMode mode;
    private int port;
    private String hostName;



    public boolean validate() {
        if (mode == null) return false;

        return switch (mode) {
            case SERVER -> port > 0;
            case CLIENT -> port > 0 && hostName != null;
            case AI_USER -> {
                port = AI_PORT;
                yield true;
            }
            case BOT_USER -> true;
            default -> false;
        };
    }

    public GameMode getMode() { return mode; }
    public void setMode(GameMode mode) { this.mode = mode; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
}