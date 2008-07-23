package org.limewire.swarm.file.verifier;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.swarm.SwarmBlockVerifier;
import org.limewire.swarm.SwarmFile;
import org.limewire.swarm.SwarmFileSystem;
import org.limewire.util.FileUtils;

public class MD5SumFileVerifier implements SwarmBlockVerifier {

    private final Range range;

    private final String md5String;

    public MD5SumFileVerifier(Range range, String md5String) {
        this.range = range;
        this.md5String = md5String;
    }

    public long getBlockSize() {
        return 1024;
    }

    public List<Range> scanForVerifiableRanges(IntervalSet writtenBlocks, long completeSize) {
        if (writtenBlocks.contains(range)) {
            List<Range> ret = new ArrayList<Range>();
            ret.add(range);
            return ret;
        }
        return Collections.emptyList();
    }

    public boolean verify(Range range, SwarmFileSystem swarmFileSystem) {
        SwarmFile swarmFile = swarmFileSystem.getSwarmFile(range.getLow());
        String testMd5;
        try {
            testMd5 = FileUtils.getMD5(swarmFile.getFile());
        } catch (Exception e) {
            // TODO handle error properly.
            e.printStackTrace();
            return false;
        }
        return testMd5.equals(md5String);
    }

}
