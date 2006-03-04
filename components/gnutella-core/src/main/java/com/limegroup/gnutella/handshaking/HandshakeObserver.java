package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.io.Shutdownable;

public interface HandshakeObserver extends Shutdownable {
    public void handleNoGnutellaOk(int code, String msg);
    public void handleBadHandshake();
    public void handleHandshakeFinished(Handshaker shaker);

}
