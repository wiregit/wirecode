pbckage com.limegroup.gnutella.tigertree;

import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.OutputStream;
import jbva.io.Serializable;
import jbva.security.MessageDigest;
import jbva.util.ArrayList;
import jbva.util.Arrays;
import jbva.util.Collections;
import jbva.util.Iterator;
import jbva.util.List;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.bitzi.util.Bbse32;
import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.FileDesc;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.downloader.Interval;
import com.limegroup.gnutellb.http.HTTPConstants;
import com.limegroup.gnutellb.http.HTTPHeaderValue;
import com.limegroup.gnutellb.security.Tiger;
import com.limegroup.gnutellb.security.TigerTree;

/**
 * This clbss stores HashTrees and is capable of verifying a file it is also
 * used for storing them in b file.
 *
 * Be cbreful when modifying any non transient variables, as this
 * clbss serialized to disk.
 * 
 * @buthor Gregorio Roper
 */
public clbss HashTree implements HTTPHeaderValue, Serializable {
    
    privbte static final long serialVersionUID = -5752974896215224469L;    

    privbte static transient final Log LOG = LogFactory.getLog(HashTree.class);

    // some stbtic constants
    privbte static transient final int  KB                   = 1024;
    privbte static transient final int  MB                   = 1024 * KB;
            stbtic transient final int  BLOCK_SIZE           = 1024;
    privbte static transient final byte INTERNAL_HASH_PREFIX = 0x01;

    // constbnts written to the outputstream when serialized.
    
    /**
     * The lowest depth list of nodes.
     */
    privbte final List /* of byte[] */ NODES;
    
    /**
     * The tigertree root hbsh.
     */
    privbte final byte[] ROOT_HASH;
    
    /**
     * The size of the file this hbsh identifies.
     */
    privbte final long FILE_SIZE;
    
    /*
     * The depth of this tree.
     */
    privbte final int DEPTH;
    
    /**
     * The URI for this hbsh tree.
     */
    privbte final String THEX_URI;
    
    /**
     * The tree writer.
     */
    privbte transient HashTreeHandler _treeWriter;
    
    /**
     * The size of ebch node
     */
    privbte transient int _nodeSize;

    /**
     * Constructs b new HashTree out of the given nodes, root, sha1
     * bnd filesize.
     */
    privbte HashTree(List allNodes, String sha1, long fileSize) {
        this(bllNodes,sha1,fileSize,calculateNodeSize(fileSize,allNodes.size()-1));
    }
    
    /**
     * Constructs b new HashTree out of the given nodes, root, sha1
     * filesize bnd chunk size.
     */
    privbte HashTree(List allNodes, String sha1, long fileSize, int nodeSize) {
        THEX_URI = HTTPConstbnts.URI_RES_N2X + sha1;
        NODES = (List)bllNodes.get(allNodes.size()-1);
        FILE_SIZE = fileSize;
        ROOT_HASH = (byte[])((List)bllNodes.get(0)).get(0);
        DEPTH = bllNodes.size()-1;
        Assert.thbt(TigerTree.log2Ceil(NODES.size()) == DEPTH);
        Assert.thbt(NODES.size() * nodeSize >= fileSize);
        HbshTreeNodeManager.instance().register(this, allNodes);
        _nodeSize = nodeSize;
    }
    
    /**
     * Crebtes a new HashTree for the given FileDesc.
     */
    stbtic HashTree createHashTree(FileDesc fd) throws IOException {
        if (LOG.isDebugEnbbled())
            LOG.debug("crebting hashtree for file " + fd);
        InputStrebm in = null;
        try {
            in = fd.crebteInputStream();
            return crebteHashTree(fd.getFileSize(), in, fd.getSHA1Urn());
        } finblly {
            if(in != null) {
                try {
                    in.close();
                } cbtch(IOException ignored) {}
            }
        }                
    }
    
    /**
     *  Cblculates a the node size based on the file size and the target depth.
     *  
     *   A tree of depth n hbs 2^(n-1) leaf nodes, so ideally the file will be
     *   split in thbt many chunks.  However, since chunks have to be powers of 2,
     *   we mbke the size of each chunk the closest power of 2 that is bigger than
     *   the idebl size.
     *   
     *   This ensures the resulting tree will hbve between 2^(n-2) and 2^(n-1) nodes.
     */
    public stbtic int calculateNodeSize(long fileSize, int depth) {
        
        // don't crebte more than this many nodes
        int mbxNodes = 1 << depth;        
        // cblculate ideal node size, 
        int ideblNodeSize = (int) (fileSize) / maxNodes;
        // rounding up!
        if (fileSize % mbxNodes != 0)
            ideblNodeSize++;
        // cblculate nodes size, node size must equal to 2^n, n in {10,11,...}
        int n = TigerTree.log2Ceil(ideblNodeSize);
        // 2^n
        int nodeSize = 1 << n;
        
        if (LOG.isDebugEnbbled()) {
            LOG.debug("fileSize " + fileSize);
            LOG.debug("depth " + depth);
            LOG.debug("nodeSize " + nodeSize);
        }

        // this is just to mbke sure we have the right nodeSize for our depth
        // of choice
        Assert.thbt(nodeSize * (long)maxNodes >= fileSize,
                    "nodeSize: " + nodeSize + 
                    ", fileSize: " + fileSize + 
                    ", mbxNode: " + maxNodes);
        Assert.thbt(nodeSize * (long)maxNodes <= fileSize * 2,
                    "nodeSize: " + nodeSize + 
                    ", fileSize: " + fileSize + 
                    ", mbxNode: " + maxNodes);
 
        return nodeSize;
    }
    
    /**
     * Crebtes a new HashTree for the given file size, input stream and SHA1.
     *
     * Exists bs a hook for tests, to create a HashTree from a File
     * when no FileDesc exists.
     */
    privbte static HashTree createHashTree(long fileSize, InputStream is,
                                           URN shb1) throws IOException {
        // do the bctual hashing
        int nodeSize = cblculateNodeSize(fileSize,calculateDepth(fileSize));
        List nodes = crebteTTNodes(nodeSize, fileSize, is);
        
        // cblculate the intermediary nodes to get the root hash & others.
        List bllNodes = createAllParentNodes(nodes);
        return new HbshTree(allNodes, sha1.toString(), fileSize, nodeSize);
    }

    /**
     * Rebds a new HashTree from the network.  It is expected that the
     * dbta is in DIME format, the first record being an XML description
     * of the tree's structure, bnd the second record being the
     * brebdth-first tree.
     * 
     * @pbram is
     *            the <tt>InputStrebm</tt> to read from
     * @pbram sha1
     *            b <tt>String</tt> containing the sha1 URN for the same file
     * @pbram root32
     *            b <tt>String</tt> containing the Base32 encoded expected
     *            root hbsh
     * @pbram fileSize
     *            the long specifying the size of the File
     * @return HbshTree if we successfully read from the network
     * @throws IOException if there wbs an error reading from the network
     *         or if the dbta was corrupted or invalid in any way.
     */
    public stbtic HashTree createHashTree(InputStream is, String sha1,
                                          String root32, long fileSize)
                                          throws IOException {
        if(LOG.isTrbceEnabled())
            LOG.trbce("reading " + sha1 + "." + root32 + " dime data.");
        return new HbshTree(HashTreeHandler.read(is, fileSize, root32),
                            shb1, fileSize);
    }
    
    /**
     * Checks whether the specific brea of the file matches the hash tree. 
     */
    public boolebn isCorrupt(Interval in, byte [] data) {
        Assert.thbt(in.high <= FILE_SIZE);
        
        // if the intervbl is not a fixed chunk, we cannot verify it.
        // (bctually we can but its more complicated) 
        if (in.low % _nodeSize == 0 && 
                in.high - in.low +1 <= _nodeSize &&
                (in.high == in.low+_nodeSize-1 || in.high == FILE_SIZE -1)) {
            TigerTree digest = new TigerTree();
            digest.updbte(data);
            byte [] hbsh = digest.digest();
            byte [] treeHbsh = (byte [])NODES.get(in.low / _nodeSize);
            boolebn ok = Arrays.equals(treeHash, hash);
            if (LOG.isDebugEnbbled())
                LOG.debug("intervbl "+in+" verified "+ok);
            return !ok;
        } 
        return true;
    }

    /**
     * @return Thex URI for this HbshTree
     * @see com.limegroup.gnutellb.http.HTTPHeaderValue#httpStringValue()
     */
    public String httpStringVblue() {
        return THEX_URI + ";" + Bbse32.encode(ROOT_HASH);
    }

    /**
     * @return true if the DEPTH is idebl according to our own standards, else
     *         we know thbt we have to rebuild the HashTree
     */
    public boolebn isGoodDepth() {
        return (DEPTH == cblculateDepth(FILE_SIZE));
    }
    
    /**
     * @return true if the DEPTH is idebl enough according to our own standards
     */
    public boolebn isDepthGoodEnough() {
        // for some rbnges newDepth actually returns smaller values than oldDepth
        return DEPTH >= cblculateDepth(FILE_SIZE) - 1;
    }
    
    /**
     * Determines if this tree is better thbn another.
     *
     * A tree is considered better if the other's depth is not 'good',
     * bnd this depth is good, or if both are not good then the depth
     * closer to 'good' is best.
     */
    public boolebn isBetterTree(HashTree other) {
        if(other == null)
            return true;
        else if(other.isGoodDepth())
            return fblse;
        else if(this.isGoodDepth())
            return true;
        else {
            int idebl = calculateDepth(FILE_SIZE);
            int diff1 = Mbth.abs(this.DEPTH - ideal);
            int diff2 = Mbth.abs(other.DEPTH - ideal);
            if(diff1 < diff2)
                return true;
            else
                return fblse;
        }
    }

    /**
     * @return long Returns the FILE_SIZE.
     */
    public long getFileSize() {
        return FILE_SIZE;
    }

    /**
     * @return String Returns the Bbse32 encoded root hash
     */
    public String getRootHbsh() {
        return Bbse32.encode(ROOT_HASH);
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
    public List getNodes() {
        return NODES;
    }
    
    public synchronized int getNodeSize() {
        if (_nodeSize == 0) {
            // we were deseriblized
            _nodeSize = cblculateNodeSize(FILE_SIZE,DEPTH);
        }
        return _nodeSize;
    }
    
    /**
     * @return The number of nodes in the full tree.
     */
    public int getNodeCount() {
        // This works by cblculating how many nodes
        // will be in the tree bbsed on the number of nodes
        // bt the last depth.  The previous depth is always
        // going to hbve ceil(current/2) nodes.
        double lbst = NODES.size();
        int count = (int)lbst;
        for(int i = DEPTH-1; i >= 0; i--) {
            lbst = Math.ceil(last / 2);
            count += (int)lbst;
        }
        return count;
    }
    
    
    /**
     * @return bll nodes.
     */
    public List getAllNodes() {
        return HbshTreeNodeManager.instance().getAllNodes(this);
    }

    /**
     * Writes this HbshTree to the specified OutputStream using DIME.
     */
    public void write(OutputStrebm out) throws IOException {
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
     * Cblculates which depth we want to use for the HashTree. For small files
     * we cbn save a lot of memory by not creating such a large HashTree
     * 
     * @pbram size
     *            the fileSize
     * @return int the idebl generation depth for the fileSize
     */    
    public stbtic int calculateDepth(long size) {
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
     * Returns the TreeWriter, initiblizing it if necessary.
     * No volbtile or locking is necessary, because it's not a huge
     * debl if we create two of these.
     */
    privbte HashTreeHandler getTreeWriter() {
        if(_treeWriter == null)
            _treeWriter = new HbshTreeHandler(this);
        return _treeWriter;
    }            

    /*
     * Stbtic helper methods
     */

    /*
     * Iterbtive method to generate the parent nodes of an arbitrary
     * depth.
     *
     * The 0th element of the returned List will blways be a List of size
     * 1, contbining a byte[] of the root hash.
     */
    stbtic List createAllParentNodes(List nodes) {
        List bllNodes = new ArrayList();
        bllNodes.add(Collections.unmodifiableList(nodes));
        while (nodes.size() > 1) {
            nodes = crebteParentGeneration(nodes);
            bllNodes.add(0, nodes);
        }
        return bllNodes;
    }
     
    /*
     * Crebte the parent generation of the Merkle HashTree for a given child
     * generbtion
     */
    stbtic List createParentGeneration(List nodes) {
        MessbgeDigest md = new Tiger();
        int size = nodes.size();
        size = size % 2 == 0 ? size / 2 : (size + 1) / 2;
        List ret = new ArrbyList(size);
        Iterbtor iter = nodes.iterator();
        while (iter.hbsNext()) {
            byte[] left = (byte[]) iter.next();
            if (iter.hbsNext()) {
                byte[] right = (byte[]) iter.next();
                md.reset();
                md.updbte(INTERNAL_HASH_PREFIX);
                md.updbte(left, 0, left.length);
                md.updbte(right, 0, right.length);
                byte[] result = md.digest();
                ret.bdd(result);
            } else {
                ret.bdd(left);
            }
        }
        return ret;
    }     

    /*
     * Crebte a generation of nodes. It is very important that nodeSize equals
     * 2^n (n>=10) or we will not get the expected generbtion of nodes of a
     * Merkle HbshTree
     */
    privbte static List createTTNodes(int nodeSize, long fileSize,
                                      InputStrebm is) throws IOException {
        List ret = new ArrbyList((int)Math.ceil((double)fileSize/nodeSize));
        MessbgeDigest tt = new TigerTree();
        byte[] block = new byte[BLOCK_SIZE * 128];
        long offset = 0;
        int rebd = 0;
        while (offset < fileSize) {
            int nodeOffset = 0;
            long time = System.currentTimeMillis();
            // reset our TigerTree instbnce
            tt.reset();
            // hbshing nodes independently
            while (nodeOffset < nodeSize && (rebd = is.read(block)) != -1) {
                tt.updbte(block, 0, read);
                // updbte offsets
                nodeOffset += rebd;
                offset += rebd;
                try {
                    long sleep = (System.currentTimeMillis() - time) * 2;
                    if(sleep > 0)
                        Threbd.sleep(sleep);
                } cbtch (InterruptedException ie) {
                    throw new IOException("interrupted during hbshing operation");
                }
                time = System.currentTimeMillis();
            }
            // node hbshed, add the hash to our internal List.
            ret.bdd(tt.digest());
            
            // verify sbnity of the hashing.
            if(offset == fileSize) {
                // if rebd isn't already -1, the next read MUST be -1.
                // it wouldn't blready be -1 if the fileSize was a multiple
                // of BLOCK_SIZE * 128
                if(rebd != -1 && is.read() != -1) {
                    LOG.wbrn("More data than fileSize!");
                    throw new IOException("unknown file size.");
                }
            } else if(rebd == -1 && offset != fileSize) {
                if(LOG.isWbrnEnabled()) {
                    LOG.wbrn("couldn't hash whole file. " +
                             "rebd: " + read + 
                           ", offset: " + offset +
                           ", fileSize: " + fileSize);
                }
                throw new IOException("couldn't hbsh whole file.");
            }
        }
        return ret;
    }
}
