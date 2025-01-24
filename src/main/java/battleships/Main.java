package main.java.battleships;

import main.java.battleships.model.GameConfig;
import main.java.battleships.model.GameMode;

public class Main {
    public static void main(String[] args) {
        GameConfig config = parseArgs(args);
        if (config == null) {
            System.out.println("Nieprawidłowe parametry. Użycie:");
            System.out.println("-mode [server|client] -port N -map map-file [-host hostName]");
            return;
        }

        /* optional block if nothing is in "serverFile" or "clientFile" - generating Boards and saving to files

        Board sampleBoard = new Board();
        sampleBoard.generateMap();
        if (config.getMode() == GameMode.SERVER) {
            sampleBoard.saveBoardToFile("serverFile");
        } else {
            sampleBoard.saveBoardToFile("clientFile");
        } */

        Player player = new Player(config);
        player.start();
    }

    private static GameConfig parseArgs(String[] args) {
        GameConfig config = new GameConfig();

        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 >= args.length) return null;

            switch (args[i]) {
                case "-mode":
                    if (args[i + 1].equals("server")) {
                        config.setMode(GameMode.SERVER);
                    } else if (args[i + 1].equals("client")) {
                        config.setMode(GameMode.CLIENT);
                    } else {
                        return null;
                    }
                    break;
                case "-port":
                    try {
                        config.setPort(Integer.parseInt(args[i + 1]));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                    break;
                case "-host":
                    config.setHostName(args[i + 1]);
                    break;
                case "-map":
                    config.setMapFile(args[i + 1]);
                    break;
                default:
                    return null;
            }
        }
        return config.validate() ? config : null;
    }
}