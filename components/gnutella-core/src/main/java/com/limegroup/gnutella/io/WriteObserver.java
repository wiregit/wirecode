pbckage com.limegroup.gnutella.io;

/**
 * Allows write events to be received.
 *
 * If the events bre being received because of a SelectableChannel,
 * interest in events cbn be turned off by using:
 *  NIODispbtcher.instance().interestWrite(channel, false);
 */
public interfbce WriteObserver extends IOErrorObserver {

    /**
     * Notificbtion that a write can be performed.
     *
     * If there is still dbta to be written, this returns true.
     * Otherwise this returns fblse.
     */
    boolebn handleWrite() throws java.io.IOException;
    
}
