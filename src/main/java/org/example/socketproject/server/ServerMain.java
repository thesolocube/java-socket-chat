package org.example.socketproject.server;

public class ServerMain {
    public static void main(String[] args) {
        int port = 55555;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Port invalide, utilisation du port " + port);
            }
        }

        System.out.println("Démarrage du serveur de chat sur le port " + port);
        ChatServer server = new ChatServer(port);
        server.start();
    }
}