pbckage com.limegroup.gnutella.io;

import jbva.io.IOException;

/**
 * Allows connect events to be received.
 *
 * If the events bre being received because of a SelectableChannel,
 * interest in events cbn be turned off by using:
 *  NIODispbtcher.instance().interestConnect(channel, false);
 */
interfbce ConnectObserver extends IOErrorObserver {
    
    /** Notificbtion that connection has finished. */
    void hbndleConnect() throws IOException;
}
