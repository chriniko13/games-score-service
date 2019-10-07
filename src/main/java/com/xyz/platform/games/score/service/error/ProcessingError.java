package com.xyz.platform.games.score.service.error;

public class ProcessingError extends RuntimeException {

    public ProcessingError(String message, Throwable error) {
        super(message, error);
    }

    public ProcessingError(String message) {
        super(message);
    }
}
