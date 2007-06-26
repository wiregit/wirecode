package com.limegroup.gnutella.tigertree;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Range;
import org.limewire.io.IOUtils;
import org.limewire.util.Base32;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.security.Tiger;
import com.limegroup.gnutella.security.TigerTree;

/**
 * This class stores HashTrees and is capable of verifying a file it is also
 * used for storing them in a file.
 *
 * Be careful when modifying any non transient variables, as this
 * class serialized to disk.
 * 
 * @author Gregorio Roper
 */
public class HashTree implements HTTPHeaderValue, Serializable {
    
    private static final long serialVersionUID = -5752974896215224469L;    

    private static transient final Log LOG = LogFactory.getLog(HashTree.class);

    // some static constants
    private static transient final int  KB                   = 1024;
    private static transient final int  MB                   = 1024 * KB;
            static transient final int  BLOCK_SIZE           = 1024;
    private static transient final byte INTERNAL_HASH_PREFIX = 0x01;
    
    /** An invalid HashTree. */
    public static final HashTree INVALID = new HashTree();

    // constants written to the outputstream when serialized.
    
    /**
     * The lowest depth list of nodes.
     */
    private final List<byte[]> NODES;
    
    /**
     * The tigertree root hash.
     */
    private final byte[] ROOT_HASH;
    
    /**
     * The size of the file this hash identifies.
     */
    private final long FILE_SIZE;
    
    /*
     * The depth of this tree.
     */
    private final int DEPTH;
    
    /**
     * The URI for this hash tree.
     */
    private final String THEX_URI;
    
    /**
     * The tree writer.
     */
    private transient HashTreeHandler _treeWriter;
    
    /**
     * The size of each node
     */
    private transient int _nodeSize;
    
    /** Constructs an invalid HashTree. */
    private HashTree() {
        NODES = null;
        ROOT_HASH = null;
        FILE_SIZE = -1;
        DEPTH = -1;
        THEX_URI = null;
    }

    /**
     * Constructs a new HashTree out of the given nodes, root, sha1
     * and filesize.
     */
    HashTree(List<List<byte[]>> allNodes, String sha1, long fileSize) {
        this(allNodes,sha1,fileSize,calculateNodeSize(fileSize,allNodes.size()-1));
    }
    
    /**
     * Constructs a new HashTree out of the given nodes, root, sha1
     * filesize and chunk size.
     */
    private HashTree(List<List<byte[]>> allNodes, String sha1, long fileSize, int nodeSize) {
        THEX_URI = HTTPConstants.URI_RES_N2X + sha1;
        NODES = allNodes.get(allNodes.size()-1);
        FILE_SIZE = fileSize;
        ROOT_HASH = allNodes.get(0).get(0);
        DEPTH = allNodes.size()-1;
        Assert.that(TigerTree.log2Ceil(NODES.size()) == DEPTH);
        Assert.that(NODES.size() * nodeSize >= fileSize);
        HashTreeNodeManager.instance().register(this, allNodes);
        _nodeSize = nodeSize;
    }
    
    /**
     * Creates a new HashTree for the given FileDesc.
     */
    static HashTree createHashTree(FileDesc fd) throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("creating hashtree for file " + fd);
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(fd.getFile()));
            return createHashTree(fd.getFileSize(), in, fd.getSHA1Urn());
        } finally {
            IOUtils.close(in);
        }                
    }
    
    /**
     *  Calculates a the node size based on the file size and the target depth.
     *  
     *   A tree of depth n has 2^(n-1) leaf nodes, so ideally the file will be
     *   split in that many chunks.  However, since chunks have to be powers of 2,
     *   we make the size of each chunk the closest power of 2 that is bigger than
     *   the ideal size.
     *   
     *   This ensures the resulting tree will have between 2^(n-2) and 2^(n-1) nodes.
     */
    public static int calculateNodeSize(long fileSize, int depth) {
        
        // don't create more than this many nodes
        int maxNodes = 1 << depth;        
        // calculate ideal node size, 
        int idealNodeSize = (int) (fileSize) / maxNodes;
        // rounding up!
        if (fileSize % maxNodes != 0)
            idealNodeSize++;
        // calculate nodes size, node size must equal to 2^n, n in {10,11,...}
        int n = TigerTree.log2Ceil(idealNodeSize);
        // 2^n
        int nodeSize = 1 << n;
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("fileSize " + fileSize);
            LOG.debug("depth " + depth);
            LOG.debug("nodeSize " + nodeSize);
        }

        // this is just to make sure we have the right nodeSize for our depth
        // of choice
        Assert.that(nodeSize * (long)maxNodes >= fileSize,
                    "nodeSize: " + nodeSize + 
                    ", fileSize: " + fileSize + 
                    ", maxNode: " + maxNodes);
        Assert.that(nodeSize * (long)maxNodes <= fileSize * 2,
                    "nodeSize: " + nodeSize + 
                    ", fileSize: " + fileSize + 
                    ", maxNode: " + maxNodes);
 
        return nodeSize;
    }
    
    /**
     * Creates a new HashTree for the given file size, input stream and SHA1.
     *
     * Exists as a hook for tests, to create a HashTree from a File
     * when no FileDesc exists.
     */
    private static HashTree createHashTree(long fileSize, InputStream is,
                                           URN sha1) throws IOException {
        // do the actual hashing
        int nodeSize = calculateNodeSize(fileSize,calculateDepth(fileSize));
        List<byte[]> nodes = createTTNodes(nodeSize, fileSize, is);
        
        // calculate the intermediary nodes to get the root hash & others.
        List<List<byte[]>> allNodes = createAllParentNodes(nodes);
        return new HashTree(allNodes, sha1.toString(), fileSize, nodeSize);
    }

    /**
     * Reads a new HashTree from the network.  It is expected that the
     * data is in DIME format, the first record being an XML description
     * of the tree's structure, and the second record being the
     * breadth-first tree.
     * 
     * @param is
     *            the <tt>InputStream</tt> to read from
     * @param sha1
     *            a <tt>String</tt> containing the sha1 URN for the same file
     * @param root32
     *            a <tt>String</tt> containing the Base32 encoded expected
     *            root hash
     * @param fileSize
     *            the long specifying the size of the File
     * @return HashTree if we successfully read from the network
     * @throws IOException if there was an error reading from the network
     *         or if the data was corrupted or invalid in any way.
     */
    public static HashTree createHashTree(InputStream is, String sha1,
                                          String root32, long fileSize)
                                          throws IOException {
        if(LOG.isTraceEnabled())
            LOG.trace("reading " + sha1 + "." + root32 + " dime data.");
        return new HashTree(HashTreeHandler.read(is, fileSize, root32),
                            sha1, fileSize);
    }
    
    public static ThexReader createHashTreeReader(String sha1, String root32, long fileSize) {
        return HashTreeHandler.createAsyncReader(sha1, fileSize, root32);
    }
    
    /**
     * Checks whether the specific area of the file matches the hash tree. 
     */
    public boolean isCorrupt(Range in, byte [] data) {
        return isCorrupt(in, data, data.length);
    }
 
    /**
     * Checks whether the specific area of the file matches the hash tree.
     */
    public boolean isCorrupt(Range in, byte[] data, int length) {
        Assert.that(in.getHigh() <= FILE_SIZE);
        
        // if the interval is not a fixed chunk, we cannot verify it.
        // (actually we can but its more complicated) 
        if (in.getLow() % _nodeSize == 0 && 
                in.getHigh() - in.getLow() +1 <= _nodeSize &&
                (in.getHigh() == in.getLow()+_nodeSize-1 || in.getHigh() == FILE_SIZE -1)) {
            TigerTree digest = new TigerTree();
            digest.update(data, 0, length);
            byte [] hash = digest.digest();
            byte [] treeHash = NODES.get((int)(in.getLow() / _nodeSize));
            boolean ok = Arrays.equals(treeHash, hash);
            if (LOG.isDebugEnabled())
                LOG.debug("interval "+in+" verified "+ok);
            return !ok;
        } 
        return true;
    }

    /**
     * @return Thex URI for this HashTree
     * @see com.limegroup.gnutella.http.HTTPHeaderValue#httpStringValue()
     */
    public String httpStringValue() {
        return THEX_URI + ";" + Base32.encode(ROOT_HASH);
    }

    /**
     * @return true if the DEPTH is ideal according to our own standards, else
     *         we know that we have to rebuild the HashTree
     */
    public boolean isGoodDepth() {
        return (DEPTH == calculateDepth(FILE_SIZE));
    }
    
    /**
     * @return true if the DEPTH is ideal enough according to our own standards
     */
    public boolean isDepthGoodEnough() {
        // for some ranges newDepth actually returns smaller values than oldDepth
        return DEPTH >= calculateDepth(FILE_SIZE) - 1;
    }
    
    /**
     * Determines if this tree is better than another.
     *
     * A tree is considered better if the other's depth is not 'good',
     * and this depth is good, or if both are not good then the depth
     * closer to 'good' is best.
     */
    public boolean isBetterTree(HashTree other) {
        if(other == null)
            return true;
        else if(other.isGoodDepth())
            return false;
        else if(this.isGoodDepth())
            return true;
        else {
            int ideal = calculateDepth(FILE_SIZE);
            int diff1 = Math.abs(this.DEPTH - ideal);
            int diff2 = Math.abs(other.DEPTH - ideal);
            if(diff1 < diff2)
                return true;
            else
                return false;
        }
    }

    /**
     * @return long Returns the FILE_SIZE.
     */
    public long getFileSize() {
        return FILE_SIZE;
    }

    /**
     * @return String Returns the Base32 encoded root hash
     */
    public String getRootHash() {
        return Base32.encode(ROOT_HASH);
    }

    /**
     * @return Returns the root hash of the TigerTree
     */
    public byte[] getRootHashBytes() {
        return ROOT_HASH;
    }
    
    /**
     * @return String the THEX_URI.
     */
    public String getThexURI() {
        return THEX_URI;
    }

    /**
     * @return int the DEPTH
     */
    public int getDepth() {
        return DEPTH;
    }

    /**
     * @return List the NODES.
     */
    public List<byte[]> getNodes() {
        return NODES;
    }
    
    public synchronized int getNodeSize() {
        if (_nodeSize == 0) {
            // we were deserialized
            _nodeSize = calculateNodeSize(FILE_SIZE,DEPTH);
        }
        return _nodeSize;
    }
    
    /**
     * @return The number of nodes in the full tree.
     */
    public int getNodeCount() {
        // This works by calculating how many nodes
        // will be in the tree based on the number of nodes
        // at the last depth.  The previous depth is always
        // going to have ceil(current/2) nodes.
        double last = NODES.size();
        int count = (int)last;
        for(int i = DEPTH-1; i >= 0; i--) {
            last = Math.ceil(last / 2);
            count += (int)last;
        }
        return count;
    }
    
    
    /**
     * @return all nodes.
     */
    public List<List<byte[]>> getAllNodes() {
        return HashTreeNodeManager.instance().getAllNodes(this);
    }

    public ThexWriter createAsyncWriter() {
        return getTreeWriter().createAsyncWriter();
    }
    
    /**
     * Writes this HashTree to the specified OutputStream using DIME.
     */
    public void write(OutputStream out) throws IOException {
        getTreeWriter().write(out);
    }
    
    /**
     * Determines the length of the tree's output.
     */
    public int getOutputLength() {
        return getTreeWriter().getLength();
    }
    
    /**
     * Determines the type of the output.
     */
    public String getOutputType() {
        return getTreeWriter().getType();
    }

    /**
     * Calculates which depth we want to use for the HashTree. For small files
     * we can save a lot of memory by not creating such a large HashTree
     * 
     * @param size
     *            the fileSize
     * @return int the ideal generation depth for the fileSize
     */    
    public static int calculateDepth(long size) {
        if (size < 256 * KB) // 256KB chunk, 0b tree
            return 0;
        else if (size < 512 * KB) // 256KB chunk, 24B tree
            return 1;
        else if (size < MB)  // 256KB chunk, 72B tree
            return 2;
        else if (size < 2 * MB) // 256KB chunk, 168B tree
            return 3;
        else if (size < 4 * MB) // 256KB chunk, 360B tree
            return 4;
        else if (size < 8 * MB) // 256KB chunk, 744B tree
            return 5;
        else if (size < 16 * MB) // 256KB chunk, 1512B tree
            return 6;
        else if (size < 32 * MB) // 256KB chunk, 3048B tree
            return 7;
        else if (size < 64 * MB) // 256KB chunk, 6120B tree
            return 8;
        else if (size < 256 * MB) // 512KB chunk, 12264B tree
            return 9;
        else if (size < 1024 * MB) // 1MB chunk, 24552B tree 
            return 10;
        else
            return 11; // 2MB chunks, 49128B tree
    }
    
    /**
     * Returns the TreeWriter, initializing it if necessary.
     * No volatile or locking is necessary, because it's not a huge
     * deal if we create two of these.
     */
    private HashTreeHandler getTreeWriter() {
        if(_treeWriter == null)
            _treeWriter = new HashTreeHandler(this);
        return _treeWriter;
    }            

    /*
     * Static helper methods
     */

    /*
     * Iterative method to generate the parent nodes of an arbitrary
     * depth.
     *
     * The 0th element of the returned List will always be a List of size
     * 1, containing a byte[] of the root hash.
     */
    static List<List<byte[]>> createAllParentNodes(List<byte[]> nodes) {
        List<List<byte[]>> allNodes = new ArrayList<List<byte[]>>();
        allNodes.add(Collections.unmodifiableList(nodes));
        while (nodes.size() > 1) {
            nodes = createParentGeneration(nodes);
            allNodes.add(0, nodes);
        }
        return allNodes;
    }
     
    /*
     * Create the parent generation of the Merkle HashTree for a given child
     * generation
     */
    static List<byte[]> createParentGeneration(List<byte[]> nodes) {
        MessageDigest md = new Tiger();
        int size = nodes.size();
        size = size % 2 == 0 ? size / 2 : (size + 1) / 2;
        List<byte[]> ret = new ArrayList<byte[]>(size);
        Iterator<byte[]> iter = nodes.iterator();
        while (iter.hasNext()) {
            byte[] left = iter.next();
            if (iter.hasNext()) {
                byte[] right = iter.next();
                md.reset();
                md.update(INTERNAL_HASH_PREFIX);
                md.update(left, 0, left.length);
                md.update(right, 0, right.length);
                byte[] result = md.digest();
                ret.add(result);
            } else {
                ret.add(left);
            }
        }
        return ret;
    }     

    /*
     * Create a generation of nodes. It is very important that nodeSize equals
     * 2^n (n>=10) or we will not get the expected generation of nodes of a
     * Merkle HashTree
     */
    private static List<byte[]> createTTNodes(int nodeSize, long fileSize,
                                              InputStream is) throws IOException {
        List<byte[]> ret = new ArrayList<byte[]>((int)Math.ceil((double)fileSize/nodeSize));
        MessageDigest tt = new TigerTree();
        byte[] block = new byte[BLOCK_SIZE * 128];
        long offset = 0;
        int read = 0;
        while (offset < fileSize) {
            int nodeOffset = 0;
            long time = System.currentTimeMillis();
            // reset our TigerTree instance
            tt.reset();
            // hashing nodes independently
            while (nodeOffset < nodeSize && (read = is.read(block)) != -1) {
                tt.update(block, 0, read);
                // update offsets
                nodeOffset += read;
                offset += read;
                try {
                    long sleep = (System.currentTimeMillis() - time) * 2;
                    if(sleep > 0)
                        Thread.sleep(sleep);
                } catch (InterruptedException ie) {
                    throw new IOException("interrupted during hashing operation");
                }
                time = System.currentTimeMillis();
            }
            // node hashed, add the hash to our internal List.
            ret.add(tt.digest());
            
            // verify sanity of the hashing.
            if(offset == fileSize) {
                // if read isn't already -1, the next read MUST be -1.
                // it wouldn't already be -1 if the fileSize was a multiple
                // of BLOCK_SIZE * 128
                if(read != -1 && is.read() != -1) {
                    LOG.warn("More data than fileSize!");
                    throw new IOException("unknown file size.");
                }
            } else if(read == -1 && offset != fileSize) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("couldn't hash whole file. " +
                             "read: " + read + 
                           ", offset: " + offset +
                           ", fileSize: " + fileSize);
                }
                throw new IOException("couldn't hash whole file.");
            }
        }
        return ret;
    }
}
