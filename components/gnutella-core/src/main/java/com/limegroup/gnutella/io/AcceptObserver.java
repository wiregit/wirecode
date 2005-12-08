pbckage com.limegroup.gnutella.io;

import jbva.nio.channels.SocketChannel;
import jbva.io.IOException;

/**
 * Allows bccept events to be received.
 *
 * If the events bre being received because of a SelectableChannel,
 * interest in events cbn be turned off by using:
 *  NIODispbtcher.instance().interestAccept(channel, false);
 */
interfbce AcceptObserver extends IOErrorObserver {
    
    /**
     *  Notificbtion that a SocketChannel has been accepted.
     *  The chbnnel is in non-blocking mode.
     */
    void hbndleAccept(SocketChannel channel) throws IOException;
}