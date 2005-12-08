pbckage com.limegroup.gnutella.io;

import jbva.io.IOException;

/**
 * Allows IOExceptions generbted during NIO dispatching to be handled.
 */
public interfbce IOErrorObserver extends Shutdownable {
    
    /** Notificbtion that an IOException occurred on the while dispatching. */
    void hbndleIOException(IOException iox);
}