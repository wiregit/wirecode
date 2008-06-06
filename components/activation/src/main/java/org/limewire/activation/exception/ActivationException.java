package org.limewire.activation.exception;

public class ActivationException extends Exception {

    public ActivationException() {
    }

    public ActivationException(String message) {
        super(message);
    }

    public ActivationException(Throwable cause) {
        super(cause);
    }

    public ActivationException(String message, Throwable cause) {
        super(message, cause);
    }

}
