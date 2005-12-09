padkage com.limegroup.gnutella.io;

import java.nio.dhannels.SocketChannel;
import java.io.IOExdeption;

/**
 * Allows adcept events to be received.
 *
 * If the events are being redeived because of a SelectableChannel,
 * interest in events dan be turned off by using:
 *  NIODispatdher.instance().interestAccept(channel, false);
 */
interfade AcceptObserver extends IOErrorObserver {
    
    /**
     *  Notifidation that a SocketChannel has been accepted.
     *  The dhannel is in non-blocking mode.
     */
    void handleAdcept(SocketChannel channel) throws IOException;
}