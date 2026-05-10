package com.smartstock.model;

import java.time.LocalDateTime;

/**
 * Maps to the socket_event_log table (M4 — real-time event tracking).
 * event_type: TRANSACTION_SYNC | INVENTORY_UPDATE | ALERT
 * status: RECEIVED | PROCESSED | FAILED
 */
public class SocketEventLog implements PhantomEntity {
    private int eventId;
    private int branchId;
    private String eventType;  // TRANSACTION_SYNC, INVENTORY_UPDATE, ALERT
    private String payload;
    private LocalDateTime receivedAt;
    private String status;     // RECEIVED, PROCESSED, FAILED

    public SocketEventLog() {}

    @Override public int getId() { return eventId; }
    @Override public void setId(int id) { this.eventId = id; }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isFailed() { return "FAILED".equals(status); }
    public boolean isProcessed() { return "PROCESSED".equals(status); }
}
