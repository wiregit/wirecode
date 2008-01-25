package com.limegroup.gnutella.tigertree;

import java.io.IOException;
import java.util.List;

import com.limegroup.gnutella.FileDesc;

public interface HashTreeFactory {

    HashTree createHashTree(List<List<byte[]>> allNodes, String sha1, long fileSize);

    /**
     * Creates a new TigerTree for the given FileDesc.
     */
    HashTree createHashTree(FileDesc fd) throws IOException;
}
