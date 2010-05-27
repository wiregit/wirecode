package com.limegroup.gnutella.dht.db;

import org.limewire.mojito.exceptions.DHTException;

public class BadValueException extends DHTException {

    public BadValueException() {
        super();
    }

    public BadValueException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadValueException(String message) {
        super(message);
    }

    public BadValueException(Throwable cause) {
        super(cause);
    }
}
