package org.example.socketproject.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.example.socketproject.client.network.ClientSocket;

public class ChatController {

    @FXML private TextArea chatArea;
    @FXML private TextField messageField;
    @FXML private Label statusLabel;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField usernameField;
    @FXML private ListView<String> usersListView;

    private ClientSocket client;
    private volatile boolean connected = false;
    private String username;
    private ObservableList<String> usersList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        statusLabel.setText("● Hors ligne get the ip");
        statusLabel.setStyle("-fx-text-fill: red;");

        if (hostField != null) {
            hostField.setText("localhost");
        }
        if (portField != null) {
            portField.setText("55555");
        }
        
        if (usersListView != null) {
            usersListView.setItems(usersList);
        }
    }


    public void shutdown() {
        if (client != null) {
            try {
                if (connected) {
                    // informe le serveur que l'utilisateur quitte
                    client.sendMessage("/quit");
                }
                client.close();
            } catch (Exception e) {

            }
        }
    }

    @FXML
    public void connectToServer() {
        if (connected) {
            chatArea.appendText("🔁 Déjà connecté au serveur\n");
            return;
        }

        String host = (hostField != null && !hostField.getText().trim().isEmpty())
                ? hostField.getText().trim()
                : "localhost";

        int port = 55555;
        if (portField != null && !portField.getText().trim().isEmpty()) {
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException e) {
                chatArea.appendText("⚠ Port invalide, utilisation de 55555\n");
            }
        }

        username = (usernameField != null && !usernameField.getText().trim().isEmpty())
                ? usernameField.getText().trim()
                : "Anonyme";

        statusLabel.setText("● Connexion...");
        statusLabel.setStyle("-fx-text-fill: orange;");
        final int finalPort = port;
        new Thread(() -> connectAndListen(host, finalPort, username), "chat-connect-listen").start();
    }

    private void connectAndListen(String host, int port, String username) {
        try {
            client = new ClientSocket();
            client.connect(host, port);
            client.sendMessage(username); // premier message = pseudo

            connected = true;
            Platform.runLater(() -> {
                statusLabel.setText("● Connecté (" + host + ":" + port + ")");
                statusLabel.setStyle("-fx-text-fill: green;");
                chatArea.appendText("✅ Connecté au serveur " + host + ":" + port + " en tant que " + username + "\n");
            });

            String msg;
            while ((msg = client.receiveMessage()) != null) {
                String finalMsg = msg;
                
                // Gérer les messages spéciaux du serveur
                if (finalMsg.startsWith("USERS:")) {
                    // Mettre à jour la liste des utilisateurs
                    Platform.runLater(() -> updateUserList(finalMsg.substring(6)));
                    continue;
                }
                
                if (finalMsg.startsWith("PRIVATE:")) {
                    // Message privé reçu
                    String[] parts = finalMsg.substring(8).split(":", 2);
                    if (parts.length == 2) {
                        String fromUser = parts[0];
                        String privateMsg = parts[1];
                        Platform.runLater(() -> {
                            chatArea.appendText("🔒 [PRIVÉ de " + fromUser + "] " + privateMsg + "\n");
                        });
                        continue;
                    }
                }
                
                // Message normal
                Platform.runLater(() -> chatArea.appendText(finalMsg + "\n"));
            }

        } catch (Exception e) {
            connected = false;
            Platform.runLater(() -> {
                statusLabel.setText("● Hors ligne");
                statusLabel.setStyle("-fx-text-fill: red;");
                chatArea.appendText("❌ Impossible de se connecter au serveur (" + host + ":" + port + ")\n");
            });
        }
    }

    @FXML
    public void sendMessage() {
        if (!connected || client == null) {
            chatArea.appendText("❌ Non connecté au serveur\n");
            return;
        }

        String msg = messageField.getText().trim();
        if (!msg.isEmpty()) {
            // Afficher le message immédiatement dans le chat avec le format username: message
            String displayMessage = (username != null ? username : "Vous") + ": " + msg;
            chatArea.appendText(displayMessage + "\n");
            
            // Envoyer le message au serveur
            client.sendMessage(msg);
            messageField.clear();
        }
    }
    
    private void updateUserList(String usersStr) {
        usersList.clear();
        if (usersStr != null && !usersStr.trim().isEmpty()) {
            String[] users = usersStr.split(",");
            for (String user : users) {
                if (user != null && !user.trim().isEmpty()) {
                    usersList.add(user.trim());
                }
            }
        }
    }
    
    @FXML
    public void sendPrivateMessage() {
        if (!connected || client == null) {
            chatArea.appendText("❌ Non connecté au serveur\n");
            return;
        }
        
        String selectedUser = usersListView.getSelectionModel().getSelectedItem();
        if (selectedUser == null || selectedUser.equals(username)) {
            chatArea.appendText("⚠ Veuillez sélectionner un utilisateur différent de vous\n");
            return;
        }
        
        String msg = messageField.getText().trim();
        if (msg.isEmpty()) {
            chatArea.appendText("⚠ Veuillez entrer un message\n");
            return;
        }
        
        // Envoyer le message privé
        String privateCommand = "/msg " + selectedUser + " " + msg;
        client.sendMessage(privateCommand);
        
        // Afficher dans le chat local
        chatArea.appendText("🔒 [PRIVÉ à " + selectedUser + "] " + msg + "\n");
        messageField.clear();
    }
}
