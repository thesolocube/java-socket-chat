package org.example.socketproject.client;

import java.io.BufferedReader;
import java.io.IOException;

//listner dial les message li kay jiw mn serveur
public class ClientListener implements Runnable {

    private final BufferedReader in;

    public ClientListener(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(message);
            }
            System.out.println("Connexion au serveur perdue.");
        } catch (IOException e) {
            System.err.println("Erreur de réception depuis le serveur : " + e.getMessage());
        }
    }
}









