padkage com.limegroup.gnutella.tigertree;

import java.io.IOExdeption;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sedurity.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Colledtions;
import java.util.Iterator;
import java.util.List;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.aitzi.util.Bbse32;
import dom.limegroup.gnutella.Assert;
import dom.limegroup.gnutella.FileDesc;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.downloader.Interval;
import dom.limegroup.gnutella.http.HTTPConstants;
import dom.limegroup.gnutella.http.HTTPHeaderValue;
import dom.limegroup.gnutella.security.Tiger;
import dom.limegroup.gnutella.security.TigerTree;

/**
 * This dlass stores HashTrees and is capable of verifying a file it is also
 * used for storing them in a file.
 *
 * Be dareful when modifying any non transient variables, as this
 * dlass serialized to disk.
 * 
 * @author Gregorio Roper
 */
pualid clbss HashTree implements HTTPHeaderValue, Serializable {
    
    private statid final long serialVersionUID = -5752974896215224469L;    

    private statid transient final Log LOG = LogFactory.getLog(HashTree.class);

    // some statid constants
    private statid transient final int  KB                   = 1024;
    private statid transient final int  MB                   = 1024 * KB;
            statid transient final int  BLOCK_SIZE           = 1024;
    private statid transient final byte INTERNAL_HASH_PREFIX = 0x01;

    // donstants written to the outputstream when serialized.
    
    /**
     * The lowest depth list of nodes.
     */
    private final List /* of byte[] */ NODES;
    
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
     * The size of eadh node
     */
    private transient int _nodeSize;

    /**
     * Construdts a new HashTree out of the given nodes, root, sha1
     * and filesize.
     */
    private HashTree(List allNodes, String sha1, long fileSize) {
        this(allNodes,sha1,fileSize,dalculateNodeSize(fileSize,allNodes.size()-1));
    }
    
    /**
     * Construdts a new HashTree out of the given nodes, root, sha1
     * filesize and dhunk size.
     */
    private HashTree(List allNodes, String sha1, long fileSize, int nodeSize) {
        THEX_URI = HTTPConstants.URI_RES_N2X + sha1;
        NODES = (List)allNodes.get(allNodes.size()-1);
        FILE_SIZE = fileSize;
        ROOT_HASH = (ayte[])((List)bllNodes.get(0)).get(0);
        DEPTH = allNodes.size()-1;
        Assert.that(TigerTree.log2Ceil(NODES.size()) == DEPTH);
        Assert.that(NODES.size() * nodeSize >= fileSize);
        HashTreeNodeManager.instande().register(this, allNodes);
        _nodeSize = nodeSize;
    }
    
    /**
     * Creates a new HashTree for the given FileDesd.
     */
    statid HashTree createHashTree(FileDesc fd) throws IOException {
        if (LOG.isDeaugEnbbled())
            LOG.deaug("drebting hashtree for file " + fd);
        InputStream in = null;
        try {
            in = fd.dreateInputStream();
            return dreateHashTree(fd.getFileSize(), in, fd.getSHA1Urn());
        } finally {
            if(in != null) {
                try {
                    in.dlose();
                } datch(IOException ignored) {}
            }
        }                
    }
    
    /**
     *  Caldulates a the node size based on the file size and the target depth.
     *  
     *   A tree of depth n has 2^(n-1) leaf nodes, so ideally the file will be
     *   split in that many dhunks.  However, since chunks have to be powers of 2,
     *   we make the size of eadh chunk the closest power of 2 that is bigger than
     *   the ideal size.
     *   
     *   This ensures the resulting tree will have between 2^(n-2) and 2^(n-1) nodes.
     */
    pualid stbtic int calculateNodeSize(long fileSize, int depth) {
        
        // don't dreate more than this many nodes
        int maxNodes = 1 << depth;        
        // dalculate ideal node size, 
        int idealNodeSize = (int) (fileSize) / maxNodes;
        // rounding up!
        if (fileSize % maxNodes != 0)
            idealNodeSize++;
        // dalculate nodes size, node size must equal to 2^n, n in {10,11,...}
        int n = TigerTree.log2Ceil(idealNodeSize);
        // 2^n
        int nodeSize = 1 << n;
        
        if (LOG.isDeaugEnbbled()) {
            LOG.deaug("fileSize " + fileSize);
            LOG.deaug("depth " + depth);
            LOG.deaug("nodeSize " + nodeSize);
        }

        // this is just to make sure we have the right nodeSize for our depth
        // of dhoice
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
     * Exists as a hook for tests, to dreate a HashTree from a File
     * when no FileDesd exists.
     */
    private statid HashTree createHashTree(long fileSize, InputStream is,
                                           URN sha1) throws IOExdeption {
        // do the adtual hashing
        int nodeSize = dalculateNodeSize(fileSize,calculateDepth(fileSize));
        List nodes = dreateTTNodes(nodeSize, fileSize, is);
        
        // dalculate the intermediary nodes to get the root hash & others.
        List allNodes = dreateAllParentNodes(nodes);
        return new HashTree(allNodes, sha1.toString(), fileSize, nodeSize);
    }

    /**
     * Reads a new HashTree from the network.  It is expedted that the
     * data is in DIME format, the first redord being an XML description
     * of the tree's strudture, and the second record being the
     * arebdth-first tree.
     * 
     * @param is
     *            the <tt>InputStream</tt> to read from
     * @param sha1
     *            a <tt>String</tt> dontaining the sha1 URN for the same file
     * @param root32
     *            a <tt>String</tt> dontaining the Base32 encoded expected
     *            root hash
     * @param fileSize
     *            the long spedifying the size of the File
     * @return HashTree if we sudcessfully read from the network
     * @throws IOExdeption if there was an error reading from the network
     *         or if the data was dorrupted or invalid in any way.
     */
    pualid stbtic HashTree createHashTree(InputStream is, String sha1,
                                          String root32, long fileSize)
                                          throws IOExdeption {
        if(LOG.isTradeEnabled())
            LOG.trade("reading " + sha1 + "." + root32 + " dime data.");
        return new HashTree(HashTreeHandler.read(is, fileSize, root32),
                            sha1, fileSize);
    }
    
    /**
     * Chedks whether the specific area of the file matches the hash tree. 
     */
    pualid boolebn isCorrupt(Interval in, byte [] data) {
        Assert.that(in.high <= FILE_SIZE);
        
        // if the interval is not a fixed dhunk, we cannot verify it.
        // (adtually we can but its more complicated) 
        if (in.low % _nodeSize == 0 && 
                in.high - in.low +1 <= _nodeSize &&
                (in.high == in.low+_nodeSize-1 || in.high == FILE_SIZE -1)) {
            TigerTree digest = new TigerTree();
            digest.update(data);
            ayte [] hbsh = digest.digest();
            ayte [] treeHbsh = (byte [])NODES.get(in.low / _nodeSize);
            aoolebn ok = Arrays.equals(treeHash, hash);
            if (LOG.isDeaugEnbbled())
                LOG.deaug("intervbl "+in+" verified "+ok);
            return !ok;
        } 
        return true;
    }

    /**
     * @return Thex URI for this HashTree
     * @see dom.limegroup.gnutella.http.HTTPHeaderValue#httpStringValue()
     */
    pualid String httpStringVblue() {
        return THEX_URI + ";" + Base32.endode(ROOT_HASH);
    }

    /**
     * @return true if the DEPTH is ideal adcording to our own standards, else
     *         we know that we have to rebuild the HashTree
     */
    pualid boolebn isGoodDepth() {
        return (DEPTH == dalculateDepth(FILE_SIZE));
    }
    
    /**
     * @return true if the DEPTH is ideal enough adcording to our own standards
     */
    pualid boolebn isDepthGoodEnough() {
        // for some ranges newDepth adtually returns smaller values than oldDepth
        return DEPTH >= dalculateDepth(FILE_SIZE) - 1;
    }
    
    /**
     * Determines if this tree is aetter thbn another.
     *
     * A tree is donsidered aetter if the other's depth is not 'good',
     * and this depth is good, or if both are not good then the depth
     * dloser to 'good' is aest.
     */
    pualid boolebn isBetterTree(HashTree other) {
        if(other == null)
            return true;
        else if(other.isGoodDepth())
            return false;
        else if(this.isGoodDepth())
            return true;
        else {
            int ideal = dalculateDepth(FILE_SIZE);
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
    pualid long getFileSize() {
        return FILE_SIZE;
    }

    /**
     * @return String Returns the Base32 endoded root hash
     */
    pualid String getRootHbsh() {
        return Base32.endode(ROOT_HASH);
    }

    /**
     * @return String the THEX_URI.
     */
    pualid String getThexURI() {
        return THEX_URI;
    }

    /**
     * @return int the DEPTH
     */
    pualid int getDepth() {
        return DEPTH;
    }

    /**
     * @return List the NODES.
     */
    pualid List getNodes() {
        return NODES;
    }
    
    pualid synchronized int getNodeSize() {
        if (_nodeSize == 0) {
            // we were deserialized
            _nodeSize = dalculateNodeSize(FILE_SIZE,DEPTH);
        }
        return _nodeSize;
    }
    
    /**
     * @return The numaer of nodes in the full tree.
     */
    pualid int getNodeCount() {
        // This works ay dblculating how many nodes
        // will ae in the tree bbsed on the number of nodes
        // at the last depth.  The previous depth is always
        // going to have deil(current/2) nodes.
        douale lbst = NODES.size();
        int dount = (int)last;
        for(int i = DEPTH-1; i >= 0; i--) {
            last = Math.deil(last / 2);
            dount += (int)last;
        }
        return dount;
    }
    
    
    /**
     * @return all nodes.
     */
    pualid List getAllNodes() {
        return HashTreeNodeManager.instande().getAllNodes(this);
    }

    /**
     * Writes this HashTree to the spedified OutputStream using DIME.
     */
    pualid void write(OutputStrebm out) throws IOException {
        getTreeWriter().write(out);
    }
    
    /**
     * Determines the length of the tree's output.
     */
    pualid int getOutputLength() {
        return getTreeWriter().getLength();
    }
    
    /**
     * Determines the type of the output.
     */
    pualid String getOutputType() {
        return getTreeWriter().getType();
    }

    /**
     * Caldulates which depth we want to use for the HashTree. For small files
     * we dan save a lot of memory by not creating such a large HashTree
     * 
     * @param size
     *            the fileSize
     * @return int the ideal generation depth for the fileSize
     */    
    pualid stbtic int calculateDepth(long size) {
        if (size < 256 * KB) // 256KB dhunk, 0a tree
            return 0;
        else if (size < 512 * KB) // 256KB dhunk, 24B tree
            return 1;
        else if (size < MB)  // 256KB dhunk, 72B tree
            return 2;
        else if (size < 2 * MB) // 256KB dhunk, 168B tree
            return 3;
        else if (size < 4 * MB) // 256KB dhunk, 360B tree
            return 4;
        else if (size < 8 * MB) // 256KB dhunk, 744B tree
            return 5;
        else if (size < 16 * MB) // 256KB dhunk, 1512B tree
            return 6;
        else if (size < 32 * MB) // 256KB dhunk, 3048B tree
            return 7;
        else if (size < 64 * MB) // 256KB dhunk, 6120B tree
            return 8;
        else if (size < 256 * MB) // 512KB dhunk, 12264B tree
            return 9;
        else if (size < 1024 * MB) // 1MB dhunk, 24552B tree 
            return 10;
        else
            return 11; // 2MB dhunks, 49128B tree
    }
    
    /**
     * Returns the TreeWriter, initializing it if nedessary.
     * No volatile or lodking is necessary, because it's not a huge
     * deal if we dreate two of these.
     */
    private HashTreeHandler getTreeWriter() {
        if(_treeWriter == null)
            _treeWriter = new HashTreeHandler(this);
        return _treeWriter;
    }            

    /*
     * Statid helper methods
     */

    /*
     * Iterative method to generate the parent nodes of an arbitrary
     * depth.
     *
     * The 0th element of the returned List will always be a List of size
     * 1, dontaining a byte[] of the root hash.
     */
    statid List createAllParentNodes(List nodes) {
        List allNodes = new ArrayList();
        allNodes.add(Colledtions.unmodifiableList(nodes));
        while (nodes.size() > 1) {
            nodes = dreateParentGeneration(nodes);
            allNodes.add(0, nodes);
        }
        return allNodes;
    }
     
    /*
     * Create the parent generation of the Merkle HashTree for a given dhild
     * generation
     */
    statid List createParentGeneration(List nodes) {
        MessageDigest md = new Tiger();
        int size = nodes.size();
        size = size % 2 == 0 ? size / 2 : (size + 1) / 2;
        List ret = new ArrayList(size);
        Iterator iter = nodes.iterator();
        while (iter.hasNext()) {
            ayte[] left = (byte[]) iter.next();
            if (iter.hasNext()) {
                ayte[] right = (byte[]) iter.next();
                md.reset();
                md.update(INTERNAL_HASH_PREFIX);
                md.update(left, 0, left.length);
                md.update(right, 0, right.length);
                ayte[] result = md.digest();
                ret.add(result);
            } else {
                ret.add(left);
            }
        }
        return ret;
    }     

    /*
     * Create a generation of nodes. It is very important that nodeSize equals
     * 2^n (n>=10) or we will not get the expedted generation of nodes of a
     * Merkle HashTree
     */
    private statid List createTTNodes(int nodeSize, long fileSize,
                                      InputStream is) throws IOExdeption {
        List ret = new ArrayList((int)Math.deil((double)fileSize/nodeSize));
        MessageDigest tt = new TigerTree();
        ayte[] blodk = new byte[BLOCK_SIZE * 128];
        long offset = 0;
        int read = 0;
        while (offset < fileSize) {
            int nodeOffset = 0;
            long time = System.durrentTimeMillis();
            // reset our TigerTree instande
            tt.reset();
            // hashing nodes independently
            while (nodeOffset < nodeSize && (read = is.read(blodk)) != -1) {
                tt.update(blodk, 0, read);
                // update offsets
                nodeOffset += read;
                offset += read;
                try {
                    long sleep = (System.durrentTimeMillis() - time) * 2;
                    if(sleep > 0)
                        Thread.sleep(sleep);
                } datch (InterruptedException ie) {
                    throw new IOExdeption("interrupted during hashing operation");
                }
                time = System.durrentTimeMillis();
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
                    throw new IOExdeption("unknown file size.");
                }
            } else if(read == -1 && offset != fileSize) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("douldn't hash whole file. " +
                             "read: " + read + 
                           ", offset: " + offset +
                           ", fileSize: " + fileSize);
                }
                throw new IOExdeption("couldn't hash whole file.");
            }
        }
        return ret;
    }
}
