package org.example.socketproject.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ChatServer server;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private boolean registered = false;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            // Initialiser les flux de communication
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            // Lire le nom d'utilisateur (envoyé par le client)
            username = reader.readLine();

            if (username == null || username.trim().isEmpty()) {
                username = "Anonyme_" + socket.getPort();
            }

            // Vérifier l'unicité du pseudo
            if (!server.registerUsername(username)) {
                writer.println("❌ Ce nom d'utilisateur est déjà utilisé. Veuillez en choisir un autre.");
                ChatLogger.getInstance().logError("Tentative de connexion avec un pseudo déjà utilisé : " + username);
                return; // on quitte run(), finally appellera disconnect()
            }
            registered = true;

            String clientIP = socket.getInetAddress().getHostAddress();
            ChatLogger.getInstance().logConnection(username, clientIP);
            System.out.println("" + username + " a rejoint le chat");
            server.broadcast(" " + username + " a rejoint le chat", this);
            
            // Envoyer la liste des utilisateurs connectés à tous les clients
            server.broadcastUserList();
            
            // Boucle de réception des messages
            String message;
            while ((message = reader.readLine()) != null) {
                if (message.trim().isEmpty()) {
                    continue;
                }

                // Commandes spéciales
                if (message.equalsIgnoreCase("/quit")) {
                    break;
                }
                
                // Message privé : format /msg username message
                if (message.startsWith("/msg ")) {
                    String[] parts = message.substring(5).split(" ", 2);
                    if (parts.length == 2) {
                        String targetUser = parts[0];
                        String privateMsg = parts[1];
                        if (server.sendPrivateMessage(username, targetUser, privateMsg)) {
                            // Confirmer à l'expéditeur que le message a été envoyé
                            writer.println("✅ Message privé envoyé à " + targetUser);
                        } else {
                            writer.println("❌ Utilisateur '" + targetUser + "' introuvable ou déconnecté");
                        }
                        continue;
                    }
                }

                System.out.println("[" + username + "] " + message);
                // Le logger sera appelé dans broadcast()
                server.broadcast(username + ": " + message, this);
            }

        } catch (IOException e) {
            ChatLogger.getInstance().logError("Erreur avec le client " + username + " : " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    // Envoyer un message à ce client
    public void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    // Déconnecter proprement le client
    public void disconnect() {
        try {
            if (registered && username != null) {
                ChatLogger.getInstance().logDisconnection(username);
                System.out.println(" " + username + " s'est déconnecté");
                server.broadcast(" " + username + " a quitté le chat", this);
                server.unregisterUsername(username);
                // Mettre à jour la liste des utilisateurs
                server.broadcastUserList();
            }

            server.removeClient(this);

            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();

        } catch (IOException e) {
            ChatLogger.getInstance().logError("Erreur lors de la déconnexion de " + username + " : " + e.getMessage());
        }
    }
    
    // Getter pour le username (utilisé par ChatServer pour les logs)
    public String getUsername() {
        return username;
    }
}