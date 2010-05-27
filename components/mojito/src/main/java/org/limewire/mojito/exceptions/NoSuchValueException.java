package org.limewire.mojito.exceptions;

public class NoSuchValueException extends DHTException {

    public NoSuchValueException() {
        super();
    }

    public NoSuchValueException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchValueException(String message) {
        super(message);
    }

    public NoSuchValueException(Throwable cause) {
        super(cause);
    }
}
