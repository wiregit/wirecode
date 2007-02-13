package com.limegroup.gnutella.rudp.messages;

import java.nio.ByteBuffer;

import org.limewire.rudp.messages.DataMessage;

class DataMessageWireImpl extends AbstractMessageWire<DataMessage> implements
        DataMessage {

    DataMessageWireImpl(DataMessage delegate) {
        super(delegate);
    }

    public ByteBuffer getChunk() {
        return delegate.getChunk();
    }

    public ByteBuffer getData1Chunk() {
        return delegate.getData1Chunk();
    }

    public ByteBuffer getData2Chunk() {
        return delegate.getData2Chunk();
    }

    public byte getDataAt(int i) {
        return delegate.getDataAt(i);
    }

}
