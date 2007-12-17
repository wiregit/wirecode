package org.limewire.nio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;

/** A simple writer that maintains statistics about how much was written. */
public class StatisticGatheringWriter extends AbstractChannelInterestWriter {
    
    private volatile long amountWrote;
    
    @Override
    public int write(ByteBuffer src) throws IOException {
        int wrote = super.write(src);
        amountWrote += wrote;
        return wrote;
    }
    
    public long getAmountWritten() {
        return amountWrote;
    }

}
