package com.smartstock.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Central Admin Server that listens for branch connections.
 */
public class BranchServer implements Runnable {
    private static final int PORT = 5000;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private boolean running = true;
    private ServerSocket serverSocket;

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("[BranchServer] Listening on port " + PORT);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[BranchServer] New branch connected: " + clientSocket.getInetAddress());
                    
                    // Handle branch in a dedicated thread
                    threadPool.execute(new BranchHandler(clientSocket));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[BranchServer] Error accepting connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[BranchServer] Could not start server: " + e.getMessage());
        } finally {
            stopServer();
        }
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
