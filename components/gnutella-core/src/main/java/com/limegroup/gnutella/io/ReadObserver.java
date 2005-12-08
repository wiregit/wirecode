pbckage com.limegroup.gnutella.io;

import jbva.io.IOException;

/**
 * Allows rebd events to be received.
 *
 * If the events bre being received because of a SelectableChannel,
 * interest in events cbn be turned off by using:
 *  NIODispbtcher.instance().interestRead(channel, false);
 */
public interfbce ReadObserver extends IOErrorObserver {
    
    /** Notificbtion that a read can be performed */
    void hbndleRead() throws IOException;    
}
    
    
