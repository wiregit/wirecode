package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.io.Shutdownable;

/**
 * A specialized ConnectObserver, with more callbacks for dealing
 * with events specific to Gnutella connections.
 */
public interface GnetConnectObserver extends Shutdownable {
    public void handleNoGnutellaOk(int code, String msg);
    public void handleBadHandshake();
    public void handleConnect();
}