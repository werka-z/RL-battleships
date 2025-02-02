package model;

public class GameConfig {
    private GameMode mode;
    private int port;
    private String hostName;

    public boolean validate() {
        if (mode == null || port <= 0) return false;
        return mode != GameMode.CLIENT || hostName != null;
    }

    public GameMode getMode() { return mode; }
    public void setMode(GameMode mode) { this.mode = mode; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
}
