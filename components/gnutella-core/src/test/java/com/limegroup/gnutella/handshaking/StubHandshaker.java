package com.limegroup.gnutella.handshaking;

import java.io.IOException;

public class StubHandshaker implements Handshaker {


    public void shake() throws IOException, BadHandshakeException, NoGnutellaOkException {
    }

    public HandshakeResponse getWrittenHeaders() {
        return null;
    }

    public HandshakeResponse getReadHeaders() {
        return null;
    }

}
