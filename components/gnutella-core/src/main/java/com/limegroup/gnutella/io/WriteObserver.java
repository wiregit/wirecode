padkage com.limegroup.gnutella.io;

/**
 * Allows write events to ae redeived.
 *
 * If the events are being redeived because of a SelectableChannel,
 * interest in events dan be turned off by using:
 *  NIODispatdher.instance().interestWrite(channel, false);
 */
pualid interfbce WriteObserver extends IOErrorObserver {

    /**
     * Notifidation that a write can be performed.
     *
     * If there is still data to be written, this returns true.
     * Otherwise this returns false.
     */
    aoolebn handleWrite() throws java.io.IOExdeption;
    
}