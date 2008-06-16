package com.limegroup.gnutella.downloader.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.nio.ByteBufferCache;
import org.limewire.swarm.file.SwarmFile;
import org.limewire.swarm.file.SwarmFileVerifier;

import com.limegroup.gnutella.tigertree.FileStream;
import com.limegroup.gnutella.tigertree.HashTree;
/**
 * A {@link SwarmFileVerifier} that uses a HashTree for verification.
 */
// TODO: Add support for a full file-scan.
public class HashTreeSwarmVerifier implements SwarmFileVerifier {
    
    private static final Log LOG = LogFactory.getLog(HashTreeSwarmVerifier.class);

    /** The block size to use before we have a hash tree suggesting a size. */
    private static final long DEFAULT_BLOCK_SIZE = 128 * 1024;
    
    /** The blocksize to verify chunks with. */
    private static final int VERIFY_BUFFER_SIZE = 64 * 1024;
    
    private final ByteBufferCache byteBufferCache;

    private volatile HashTree hashTree;
    
    public HashTreeSwarmVerifier(ByteBufferCache byteBufferCache) {
        this.byteBufferCache = byteBufferCache;
    }

    public long getBlockSize() {
        if(hashTree == null)
            return DEFAULT_BLOCK_SIZE;
        else
            return hashTree.getNodeSize();
    }

    public List<Range> scanForVerifiableRanges(IntervalSet writtenBlocks, long completeSize) {
        if(hashTree == null)
            return Collections.emptyList();

        List<Range> verifiable = new ArrayList<Range>(2);
        long blockSize = getBlockSize();
        
        for(Range range : writtenBlocks) {
            // find the beginning of the first chunk offset
            long lowChunkOffset = range.getLow() - range.getLow() % blockSize;
            if (range.getLow() % blockSize != 0) {
                lowChunkOffset += blockSize;
            }
            while (range.getHigh() >= lowChunkOffset + blockSize - 1) {
                Range complete = Range.createRange(lowChunkOffset, lowChunkOffset + blockSize - 1);
                verifiable.add(complete);
                lowChunkOffset += blockSize;
            }
        }

        // special case for the last chunk
        if(!writtenBlocks.isEmpty()) {
            long lastChunkOffset = completeSize - (completeSize % blockSize);
            if (lastChunkOffset == completeSize) {
                lastChunkOffset -= blockSize;
            }
            Range last = writtenBlocks.getLast();
            if (last.getHigh() == completeSize - 1 && last.getLow() <= lastChunkOffset) {
                LOG.debug("adding the last chunk for verification");
                verifiable.add(Range.createRange(lastChunkOffset, last.getHigh()));
            }
        }
        
        return verifiable;
    }

    public boolean verify(Range range, SwarmFile swarmFile) {
        ByteBuffer buffer = byteBufferCache.get(VERIFY_BUFFER_SIZE);
        boolean verified = !hashTree.isCorrupt(range, new SwarmFileStream(swarmFile), buffer);
        byteBufferCache.release(buffer);
        return verified;
    }
    
    private static class SwarmFileStream implements FileStream {
        private final SwarmFile swarmFile;
        
        public SwarmFileStream(SwarmFile swarmFile) {
            this.swarmFile = swarmFile;
        }
        
        public void read(ByteBuffer buffer, long position) throws IOException {
            long required = buffer.remaining();
            long read = 0;
            while(read < required) {
                read += swarmFile.transferTo(buffer, position + read); 
            }
        }
    }

    public HashTree getHashTree() {
        return hashTree;
    }
    
    /** Returns true if the tree was accepted, false otherwise. */
    public TreeUpgradeResponse setHashTree(HashTree tree, long fileSize, long verifiedSoFar) {
        if(tree.getFileSize() == fileSize) {
            HashTree oldTree = hashTree;
            if(oldTree == null) {
                this.hashTree = tree;
                return TreeUpgradeResponse.NEW_TREE;
            } else if(!oldTree.getRootHash().equals(hashTree.getRootHash())) {
                if(verifiedSoFar > 2 * DEFAULT_BLOCK_SIZE) {
                    // If we've already verified too much, don't accept the new tree.
                    return TreeUpgradeResponse.NOT_ACCEPTED;
                } else if(verifiedSoFar == 0) {
                    this.hashTree = tree;
                    return TreeUpgradeResponse.UPGRADE;
                } else {
                    this.hashTree = tree;
                    return TreeUpgradeResponse.REVERIFY;
                }
            } else {
                this.hashTree = tree;
                return TreeUpgradeResponse.UPGRADE;
            }
        } else {
            return TreeUpgradeResponse.NOT_ACCEPTED;
        }
    }
    
    public static enum TreeUpgradeResponse {
        /** The tree wasn't accepted. */
        NOT_ACCEPTED,
        /** A brand new tree was set -> try to verify as much as possible. */
        NEW_TREE,
        /** The new tree doesn't match the old one -> reverify all verified data */
        REVERIFY,
        /** The new tree has no impact on verified data. */
        UPGRADE;
    }

}
