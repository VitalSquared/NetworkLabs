package ru.nsu.spirin.transfer.exceptions;

public class UnknownResponseCodeException extends Exception {
    public UnknownResponseCodeException(String message) {
        super(message);
    }
}
