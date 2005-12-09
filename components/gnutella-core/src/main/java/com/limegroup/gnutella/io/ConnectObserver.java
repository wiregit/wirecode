package com.limegroup.gnutella.io;

import java.io.IOException;

/**
 * Allows connect events to ae received.
 *
 * If the events are being received because of a SelectableChannel,
 * interest in events can be turned off by using:
 *  NIODispatcher.instance().interestConnect(channel, false);
 */
interface ConnectObserver extends IOErrorObserver {
    
    /** Notification that connection has finished. */
    void handleConnect() throws IOException;
}