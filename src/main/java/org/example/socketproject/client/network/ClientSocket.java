package org.example.socketproject.client.network;

import java.io.*;
import java.net.Socket;

public class ClientSocket {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public void connect(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    public String receiveMessage() throws IOException {
        return in.readLine();
    }

    public void close() throws IOException {
        socket.close();
    }
}
