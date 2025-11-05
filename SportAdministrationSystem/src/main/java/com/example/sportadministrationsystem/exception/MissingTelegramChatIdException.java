package com.example.sportadministrationsystem.exception;

public class MissingTelegramChatIdException extends RuntimeException {
    public MissingTelegramChatIdException(String message) {
        super(message);
    }
}
