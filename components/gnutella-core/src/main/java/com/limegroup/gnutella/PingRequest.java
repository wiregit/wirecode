package com.limegroup.gnutella;

import java.io.*;

/**
 * A Gnutella ping message.
 */

public class PingRequest extends Message {
    public PingRequest(byte ttl) {
        super((byte)0x0, ttl, (byte)0);
    }

    public PingRequest(byte[] guid, byte ttl, byte hops) {
        super(guid, Message.F_PING, ttl, hops, 0);
    }

    public void writePayload(OutputStream out) {
        //Does nothing...there is no payload!
    }

    public String toString() {
        return "PingRequest("+super.toString()+")";
    }
}
