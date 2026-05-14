package com.smartstock.network;

import com.smartstock.dao.TransactionDAO;
import com.smartstock.model.Transaction;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * Handles communication with a specific branch client.
 */
public class BranchHandler implements Runnable {
    private final Socket socket;
    private final TransactionDAO transactionDAO;

    public BranchHandler(Socket socket) {
        this.socket = socket;
        this.transactionDAO = new TransactionDAO();
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            while (!socket.isClosed()) {
                try {
                    Object received = in.readObject();
                    if (received instanceof Transaction transaction) {
                        System.out.println("[BranchHandler] Received transaction from branch " + transaction.getBranchId());
                        
                        // Persist to central database
                        // We use the existing DAO which handles JDBC transactions
                        int txnId = transactionDAO.insertWithItems(transaction);
                        
                        if (txnId > 0) {
                            System.out.println("[BranchHandler] Transaction " + txnId + " logged successfully.");
                        } else {
                            System.err.println("[BranchHandler] Failed to log transaction.");
                        }
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("[BranchHandler] Invalid object received: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("[BranchHandler] Connection closed by branch.");
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("[BranchHandler] Error in branch communication: " + e.getMessage());
        } finally {
            closeSocket();
        }
    }

    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
