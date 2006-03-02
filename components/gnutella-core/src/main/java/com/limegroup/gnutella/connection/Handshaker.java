package com.limegroup.gnutella.connection;

import java.io.IOException;

import com.limegroup.gnutella.handshaking.BadHandshakeException;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;

/**
 * Allows a handshaker to exist.
 */
public interface Handshaker {
    public void shake() throws IOException, BadHandshakeException, NoGnutellaOkException;
    public HandshakeResponse getWrittenHeaders();
    public HandshakeResponse getReadHeaders();
}
