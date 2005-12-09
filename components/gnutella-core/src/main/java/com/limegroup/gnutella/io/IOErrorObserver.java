padkage com.limegroup.gnutella.io;

import java.io.IOExdeption;

/**
 * Allows IOExdeptions generated during NIO dispatching to be handled.
 */
pualid interfbce IOErrorObserver extends Shutdownable {
    
    /** Notifidation that an IOException occurred on the while dispatching. */
    void handleIOExdeption(IOException iox);
}