package org.example.socketproject.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ChatServer {
    private int port;
    private ServerSocket serverSocket;
    private Set<ClientHandler> clients;
    private Set<String> usernames;
    private ExecutorService threadPool;
    private volatile boolean running;

    public ChatServer(int port) {
        this.port = port;
        this.clients = ConcurrentHashMap.newKeySet(); // Thread-safe
        this.usernames = ConcurrentHashMap.newKeySet(); // Pseudos uniques
        this.threadPool = Executors.newCachedThreadPool();
        this.running = false;
    }

    public void start() {
        ChatLogger logger = ChatLogger.getInstance();
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            logger.logServerStart(port);
            System.out.println(" Adresses IP disponibles pour la connexion :");
            System.out.println("   - localhost / 127.0.0.1 (même machine)");
            printLocalIPAddresses();
            System.out.println(" En attente de connexions...\n");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    clients.add(clientHandler);
                    threadPool.execute(clientHandler);

                    String clientIP = clientSocket.getInetAddress().getHostAddress();
                    logger.logServerEvent("Nouvelle connexion depuis " + clientIP + " (Clients connectés : " + clients.size() + ")");

                } catch (IOException e) {
                    if (running) {
                        logger.logError("Erreur lors de l'acceptation d'un client : " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            logger.logError("Impossible de démarrer le serveur : " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        ChatLogger logger = ChatLogger.getInstance();

        System.out.println("\n Arrêt du serveur...");

        // Fermer tous les clients
        for (ClientHandler client : clients) {
            client.disconnect();
        }

        // Arrêter le pool de threads
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }

        // Fermer le ServerSocket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.logError("Erreur lors de la fermeture du serveur : " + e.getMessage());
        }

        logger.logServerStop();
        logger.close();
    }

    // Diffuser un message à tous les clients sauf l'expéditeur
    public void broadcast(String message, ClientHandler sender) {
        // Enregistrer le message dans les logs si c'est un message utilisateur (pas un message système)
        if (sender != null && sender.getUsername() != null && message.contains(": ")) {
            ChatLogger.getInstance().logMessage(sender.getUsername(), message);
        }
        
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }
    
    // Envoyer la liste des utilisateurs connectés à tous les clients
    public void broadcastUserList() {
        StringBuilder userList = new StringBuilder("USERS:");
        for (ClientHandler client : clients) {
            if (client.getUsername() != null) {
                if (userList.length() > 6) {
                    userList.append(",");
                }
                userList.append(client.getUsername());
            }
        }
        String userListStr = userList.toString();
        for (ClientHandler client : clients) {
            client.sendMessage(userListStr);
        }
    }
    
    // Envoyer un message privé à un utilisateur spécifique
    public boolean sendPrivateMessage(String fromUsername, String toUsername, String message) {
        for (ClientHandler client : clients) {
            if (client.getUsername() != null && client.getUsername().equals(toUsername)) {
                client.sendMessage("PRIVATE:" + fromUsername + ":" + message);
                ChatLogger.getInstance().logMessage(fromUsername + " -> " + toUsername, message);
                return true;
            }
        }
        return false;
    }

    // Retirer un client déconnecté
    public void removeClient(ClientHandler client) {
        clients.remove(client);
        ChatLogger.getInstance().logServerEvent(" Client déconnecté (Clients connectés : " + clients.size() + ")");
    }

    // Enregistrer un nouveau pseudo, retourne false s'il est déjà pris
    public boolean registerUsername(String username) {
        return usernames.add(username);
    }

    // Supprimer un pseudo lors de la déconnexion
    public void unregisterUsername(String username) {
        if (username != null) {
            usernames.remove(username);
        }
    }

    // Afficher les adresses IP locales pour faciliter la connexion depuis d'autres machines
    private void printLocalIPAddresses() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                // Ignorer les interfaces loopback et non actives
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    // Afficher uniquement les adresses IPv4
                    if (!address.isLoopbackAddress() && address.getAddress().length == 4) {
                        System.out.println("   - " + address.getHostAddress() + " (réseau local)");
                    }
                }
            }
        } catch (Exception e) {
            // Ignorer les erreurs d'affichage des adresses IP
        }
    }
}