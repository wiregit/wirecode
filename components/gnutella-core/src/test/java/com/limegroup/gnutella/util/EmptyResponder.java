package com.limegroup.gnutella.util;

import com.limegroup.gnutella.handshaking.*;
import java.io.*;
import java.util.*;

public final class EmptyResponder implements HandshakeResponder {
    public HandshakeResponse respond(HandshakeResponse response, 
                                     boolean outgoing) throws IOException {
        return HandshakeResponse.createResponse(new Properties());
    }
}
