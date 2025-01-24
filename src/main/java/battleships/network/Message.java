package main.java.battleships.network;

public record Message(String command, String coordinates) {

    public String format() {
        return coordinates != null ?
                command + ";" + coordinates + "\n" :
                command + "\n";
    }
}