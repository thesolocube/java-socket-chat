package org.example.socketproject.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Entrez l'adresse du serveur (vide pour localhost) : ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) {
            host = "localhost";
        }

        int port = 55555;
        System.out.print("Port du serveur (vide pour " + port + ") : ");
        String portInput = scanner.nextLine().trim();
        if (!portInput.isEmpty()) {
            try {
                port = Integer.parseInt(portInput);
            } catch (NumberFormatException e) {
                System.err.println("Port invalide, utilisation de " + port);
            }
        }

        System.out.print("Entrez votre pseudo : ");
        String username = scanner.nextLine();

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // envoyer le pseudo au serveur
            out.println(username);

            // thread qui écoute les messages du serveur
            new Thread(new ClientListener(in)).start();

            System.out.println("Vous êtes connecté. Tapez /quit pour quitter.");

            String message;
            while ((message = scanner.nextLine()) != null) {
                out.println(message);
                if ("/quit".equalsIgnoreCase(message.trim())) {
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("Impossible de se connecter au serveur : " + e.getMessage());
        }
    }
}
