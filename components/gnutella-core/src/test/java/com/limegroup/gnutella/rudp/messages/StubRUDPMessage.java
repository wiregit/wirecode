package com.limegroup.gnutella.rudp.messages;

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.rudp.messages.RUDPMessage;

public class StubRUDPMessage implements RUDPMessage {

    public void extendSequenceNumber(long seqNo) {
    }

    public byte getConnectionID() {
        return 0;
    }

    public int getDataLength() {
        return 0;
    }

    public OpCode getOpCode() {
        return null;
    }

    public long getSequenceNumber() {
        return 0;
    }

    public void write(OutputStream out) throws IOException {
    }

    public int getLength() {
        return 0;
    }

}
