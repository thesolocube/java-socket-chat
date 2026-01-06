package org.example.socketproject.server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Système de logging pour enregistrer tous les événements et messages du serveur.
 * Les logs sont sauvegardés dans le dossier "logs" :
 * - server.log : événements du serveur (démarrage, connexions, déconnexions, erreurs)
 * - messages.log : tous les messages échangés entre les utilisateurs
 */
public class ChatLogger {
    private static final String LOGS_DIR = "logs";
    private static final String SERVER_LOG_FILE = LOGS_DIR + File.separator + "server.log";
    private static final String MESSAGES_LOG_FILE = LOGS_DIR + File.separator + "messages.log";
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static ChatLogger instance;
    private PrintWriter serverLogWriter;
    private PrintWriter messagesLogWriter;
    
    private ChatLogger() {
        try {
            File logsDir = new File(LOGS_DIR);
            if (!logsDir.exists()) {logsDir.mkdirs();
            }
            
            // Initialiser les writers avec append=true pour ajouter aux fichiers existants
            serverLogWriter = new PrintWriter(new FileWriter(SERVER_LOG_FILE, true), true);
            messagesLogWriter = new PrintWriter(new FileWriter(MESSAGES_LOG_FILE, true), true);
            
        } catch (IOException e) {
            System.err.println(" Erreur lors de l'initialisation du système de logs : " + e.getMessage());
        }
    }
    
    public static synchronized ChatLogger getInstance() {
        if (instance == null) {
            instance = new ChatLogger();
        }
        return instance;
    }
    
    /**
     * Enregistre un événement du serveur (démarrage, connexions, déconnexions, erreurs)
     */
    public void logServerEvent(String event) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String logEntry = String.format("[%s] %s", timestamp, event);
        
        // Afficher dans la console
        System.out.println(logEntry);
        
        // Enregistrer dans le fichier
        if (serverLogWriter != null) {
            serverLogWriter.println(logEntry);
        }
    }
    
    /**
     * Enregistre un message échangé entre utilisateurs
     */
    public void logMessage(String username, String message) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String logEntry = String.format("[%s] [%s] %s", timestamp, username, message);
        
        // Enregistrer dans le fichier messages.log
        if (messagesLogWriter != null) {
            messagesLogWriter.println(logEntry);
        }
    }
    
    /**
     * Enregistre une erreur
     */
    public void logError(String error) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String logEntry = String.format("[%s] [ERROR] %s", timestamp, error);
        
        // Afficher dans la console
        System.err.println(logEntry);
        
        // Enregistrer dans le fichier
        if (serverLogWriter != null) {
            serverLogWriter.println(logEntry);
        }
    }
    

    public void logConnection(String username, String ipAddress) {logServerEvent(String.format("Connexion : %s depuis %s", username, ipAddress));
    }

    public void logDisconnection(String username) {logServerEvent(String.format("Déconnexion : %s", username));
    }

    public void logServerStart(int port) {
        logServerEvent(String.format("Serveur démarré sur le port %d", port));
    }

    public void logServerStop() {
        logServerEvent("Serveur arrêté");
    }

    public void close() {
        if (serverLogWriter != null) {serverLogWriter.close();
        }
        if (messagesLogWriter != null) {messagesLogWriter.close();
        }
    }
}




