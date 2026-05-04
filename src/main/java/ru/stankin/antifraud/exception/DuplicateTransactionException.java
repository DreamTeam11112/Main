package ru.stankin.antifraud.exception;

public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String transactionId) {
        super("transaction already exists: " + transactionId);
    }
}
