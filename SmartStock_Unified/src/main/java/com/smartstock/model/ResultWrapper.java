package com.smartstock.model;

/**
 * Generic wrapper for analysis results (Module 2 - Product Analysis).
 * Used to demonstrate generics requirement.
 */
public class ResultWrapper<T> {
    private T data;
    private boolean success;
    private String message;

    public ResultWrapper() {}

    public ResultWrapper(T data, boolean success, String message) {
        this.data = data;
        this.success = success;
        this.message = message;
    }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
