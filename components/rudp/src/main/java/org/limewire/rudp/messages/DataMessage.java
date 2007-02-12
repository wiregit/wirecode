package org.limewire.rudp.messages;

import java.nio.ByteBuffer;

public interface DataMessage {

    public static final int MAX_DATA = 512;

    /** Returns the chunk that was used for creation, if it was created with a chunk. */
    public abstract ByteBuffer getChunk();

    /**
     * Return the data in the GUID as the data1 chunk.
     */
    public abstract ByteBuffer getData1Chunk();

    /**
     * Return the data in the payload as the data2 chunk/
     */
    public abstract ByteBuffer getData2Chunk();

    public abstract byte getDataAt(int i);

}