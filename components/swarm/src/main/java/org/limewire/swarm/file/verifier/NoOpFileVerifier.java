package org.limewire.swarm.file.verifier;

import java.util.Collections;
import java.util.List;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.swarm.SwarmFileSystem;
import org.limewire.swarm.SwarmBlockVerifier;

public class NoOpFileVerifier implements SwarmBlockVerifier {

    public long getBlockSize() {
        return 1024;
    }

    public List<Range> scanForVerifiableRanges(IntervalSet writtenBlocks, long completeSize) {
        return Collections.emptyList();
    }

    public boolean verify(Range range, SwarmFileSystem swarmFile) {
        throw new UnsupportedOperationException();
    }

}
