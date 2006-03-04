package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.io.Shutdownable;

public interface HandshakeObserver extends Shutdownable {
    
    public void handleHandshakeFinished(Handshaker shaker);

}
