package org.limewire.mojito.exceptions;

/**
 * Thrown if a <tt>FIND_NODE</tt> lookup failed.
 */
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
