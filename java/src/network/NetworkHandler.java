package network;

import model.GameMode;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class NetworkHandler implements AutoCloseable {
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private ServerSocket serverSocket;
    private Message lastSentMessage;
    private int retryCount = 0;
    private final GameMode mode;

    public NetworkHandler(GameMode mode, int port, String host) throws IOException {
        this.mode = mode;
        if (mode == GameMode.SERVER) {
            serverSocket = new ServerSocket(port);
            socket = serverSocket.accept();
            serverSocket.close();
        } else {
            socket = new Socket(host, port);
        }

        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    public void sendMessage(Message message) {
        writer.print(message.format());
        writer.flush();
        System.out.println("Sent: " + message.format().trim());
        lastSentMessage = message;
        retryCount = 0;
    }

    public Message receiveMessage() {
        try {
            // Set timeout based on game mode
            boolean isHumanPlayer = (mode == GameMode.SERVER || mode == GameMode.CLIENT);
            socket.setSoTimeout(lastSentMessage != null ? (isHumanPlayer ? 60000 : 1000) : 0);

            String line = reader.readLine();
            if (line != null) {
                retryCount = 0;
                lastSentMessage = null;
                return parseMessage(line);
            }
        } catch (SocketTimeoutException e) {
            retryCount++;
            if (retryCount >= 3) {
                System.out.println("No response received. Would you like to retry? (y/n)");
                Scanner scanner = new Scanner(System.in);
                if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                    retryCount = 0;
                    sendMessage(lastSentMessage);
                } else {
                    System.out.println("Game terminated by user.");
                    System.exit(1);
                }
            } else {
                System.out.println("Waiting for response... ( " + retryCount + "/3)");
                sendMessage(lastSentMessage);
            }
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
            System.exit(1);
        }
        return null;
    }

    private Message parseMessage(String line) {
        String[] parts = line.split(";");
        String command = parts[0];
        String coordinates = parts.length > 1 ? parts[1] : null;
        return new Message(command, coordinates);
    }

    @Override
    public void close() throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (reader != null) {
            reader.close();
        }
        if (writer != null) {
            writer.close();
        }
    }
}