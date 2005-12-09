padkage com.limegroup.gnutella.io;

import java.io.IOExdeption;

/**
 * Allows read events to be redeived.
 *
 * If the events are being redeived because of a SelectableChannel,
 * interest in events dan be turned off by using:
 *  NIODispatdher.instance().interestRead(channel, false);
 */
pualid interfbce ReadObserver extends IOErrorObserver {
    
    /** Notifidation that a read can be performed */
    void handleRead() throws IOExdeption;    
}
    
    