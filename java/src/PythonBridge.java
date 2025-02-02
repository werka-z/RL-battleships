import py4j.GatewayServer;

public class PythonBridge {
    private static PythonBridge instance;
    private final GatewayServer server;

    private PythonBridge() {
        server = new GatewayServer(this);
        server.start();
    }

    public static PythonBridge getInstance() {
        if (instance == null) {
            instance = new PythonBridge();
        }
        return instance;
    }

    public void updateShotResult(int row, int col, String result) {
        System.out.println("Shot result at " + row + "," + col + ": " + result);
    }

    public void shutdown() {
        if (server != null) {
            server.shutdown();
        }
    }
}