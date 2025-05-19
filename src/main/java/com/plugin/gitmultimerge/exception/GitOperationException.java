package com.plugin.gitmultimerge.exception;

import java.io.Serial;

public class GitOperationException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public GitOperationException(String message) {
        super(message);
    }

    public GitOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}