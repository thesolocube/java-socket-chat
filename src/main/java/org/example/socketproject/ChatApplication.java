package org.example.socketproject;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.socketproject.client.ChatController;


public class ChatApplication extends Application {

    private ChatController chatController;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(ChatApplication.class.getResource("chat.fxml"));
        Parent root = loader.load();
        this.chatController = loader.getController();

        Scene scene = new Scene(root, 600, 400);
        stage.setTitle("Chat LAN");
        stage.setScene(scene);

        // Quand on ferme la fenêtre, on déconnecte proprement le client
        stage.setOnCloseRequest(event -> {
            if (chatController != null) {
                chatController.shutdown();
            }
        });

        stage.show();
    }

    @Override
    public void stop() {
        if (chatController != null) {
            chatController.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

