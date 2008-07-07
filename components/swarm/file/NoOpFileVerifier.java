package org.limewire.swarm.file;

import java.util.Collections;
import java.util.List;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;

public class NoOpFileVerifier implements SwarmFileVerifier {

    public long getBlockSize() {
        return 1024;
    }

    public List<Range> scanForVerifiableRanges(IntervalSet writtenBlocks, long completeSize) {
        return Collections.emptyList();
    }

    public boolean verify(Range range, SwarmFile swarmFile) {
        throw new UnsupportedOperationException();
    }

}
