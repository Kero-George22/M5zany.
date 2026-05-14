package com.smartstock.network;

import com.smartstock.model.Transaction;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client for branches to send sales data to the Admin Server.
 */
public class SocketClient {
    private static SocketClient instance;
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;
    
    private Socket socket;
    private ObjectOutputStream out;
    private final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor();

    private SocketClient() {
        connect();
    }

    public static synchronized SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    private void connect() {
        asyncExecutor.execute(() -> {
            try {
                socket = new Socket(SERVER_HOST, SERVER_PORT);
                out = new ObjectOutputStream(socket.getOutputStream());
                System.out.println("[SocketClient] Connected to Admin Server at " + SERVER_HOST + ":" + SERVER_PORT);
            } catch (IOException e) {
                System.err.println("[SocketClient] Could not connect to Admin Server: " + e.getMessage());
            }
        });
    }

    /**
     * Sends a transaction to the server asynchronously.
     */
    public void sendTransaction(Transaction transaction) {
        asyncExecutor.execute(() -> {
            try {
                if (socket == null || socket.isClosed() || out == null) {
                    System.err.println("[SocketClient] Not connected. Attempting to reconnect...");
                    connect();
                    // Wait a bit for reconnection attempt (simplified)
                    Thread.sleep(1000);
                }
                
                if (out != null) {
                    out.writeObject(transaction);
                    out.flush();
                    // out.reset() is important when sending the same object type repeatedly
                    out.reset();
                    System.out.println("[SocketClient] Transaction " + transaction.getTransactionId() + " sent to Admin.");
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("[SocketClient] Error sending transaction: " + e.getMessage());
            }
        });
    }

    public void close() {
        try {
            if (out != null) out.close();
            if (socket != null) socket.close();
            asyncExecutor.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
