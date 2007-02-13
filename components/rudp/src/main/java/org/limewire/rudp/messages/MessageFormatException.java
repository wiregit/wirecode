package org.limewire.rudp.messages;

import org.limewire.io.InvalidDataException;

public class MessageFormatException extends InvalidDataException {

    public MessageFormatException() {
        super();
    }

    public MessageFormatException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public MessageFormatException(String msg) {
        super(msg);
    }

    public MessageFormatException(Throwable cause) {
        super(cause);
    }

}
