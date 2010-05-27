package org.limewire.mojito.exceptions;

public class NoSuchNodeException extends DHTException {

    public NoSuchNodeException() {
        super();
    }

    public NoSuchNodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchNodeException(String message) {
        super(message);
    }

    public NoSuchNodeException(Throwable cause) {
        super(cause);
    }
}
