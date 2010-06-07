package org.limewire.mojito.exceptions;

import java.io.IOException;

public class DHTException extends IOException {

    public DHTException() {
        super();
    }

    public DHTException(String message, Throwable cause) {
        super(message, cause);
    }

    public DHTException(String message) {
        super(message);
    }

    public DHTException(Throwable cause) {
        super(cause);
    }
}
