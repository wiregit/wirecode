package com.limegroup.gnutella.tigertree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.security.MessageDigest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bitzi.util.Base32;
import com.bitzi.util.TigerTree;
import com.bitzi.util.Tiger;
import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.Interval;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.sun.java.util.collections.Collections;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.ArrayList;
import com.sun.java.util.collections.List;

/**
 * This class stores HashTrees and is capable of verifying a file it is also
 * used for storing them in a file.
 *
 * Be careful when modifying any non transient variables, as this
 * class serialized to disk.
 * 
 * @author Gregorio Roper
 */
public final class HashTree implements HTTPHeaderValue, Serializable {

    private static transient final Log LOG = LogFactory.getLog(HashTree.class);

    // some static constants
    private static transient final int  KB                   = 1024;
    private static transient final int  MB                   = 1024 * KB;
            static transient final int  BLOCK_SIZE           = 1024;
    private static transient final byte INTERNAL_HASH_PREFIX = 0x01;
    
    /**
     * The maximum depth we'll store the intermediary nodes.
     */
    private static transient final int MAX_DEPTH_TO_STORE = 5;

    // constants written to the outputstream when serialized.
    
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
     * All intermediary nodes.
     *
     * This is lazily allocated for deserialized HashTrees the first
     * time that getAllNodes is called.
     */
    private transient List /* of List of byte[] */ ALL_NODES;

    /*
     * Constructs a new HashTree out of the given nodes, root, sha1
     * and filesize.
     */
    private HashTree(List allNodes, String sha1, long fileSize) {
        List nodes = (List)allNodes.get(allNodes.size()-1);
        byte[] root = (byte[])((List)allNodes.get(0)).get(0);
        
        THEX_URI = HTTPConstants.URI_RES_N2X + sha1;
        NODES = Collections.unmodifiableList(nodes);
        FILE_SIZE = fileSize;
        ROOT_HASH = root;
        // calculate the actual depth we read from the stream by calculating
        // the log2.
        DEPTH = log2Ceil(NODES.size());
        
        // Only store smaller trees.
        if(DEPTH <= MAX_DEPTH_TO_STORE)
            ALL_NODES = Collections.unmodifiableList(allNodes);
        else
            ALL_NODES = null;
    }

    /**
     * Creates a new HashTree for the given FileDesc.
     */
    static HashTree createHashTree(FileDesc fd) {
        if (LOG.isDebugEnabled())
            LOG.debug("creating hashtree for file " + fd);
        try {
            return createHashTree(fd.getSize(), fd.createInputStream(),
                                  fd.getSHA1Urn());
        } catch(FileNotFoundException fnfe) {
            LOG.debug(fnfe);
            return null;
        }
    }
    
    /**
     * Creates a new HashTree for the given file size, input stream and SHA1.
     *
     * Exists as a hook for tests, to create a HashTree from a File
     * when no FileDesc exists.
     */
    private static HashTree createHashTree(long fileSize, InputStream is,
                                           URN sha1) {
        int depth = calculateDepth(fileSize);
        
        // don't create more than this many nodes
        int maxNodes = 1 << depth;        
        // calculate ideal node size, rounding up!
        int idealNodeSize = (int) (fileSize + 1) / maxNodes;
        // calculate nodes size, node size must equal to 2^n, n in {10,11,...}
        int n = log2Ceil(idealNodeSize);
        // 2^n
        int nodeSize = 1 << n;
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("fileSize " + fileSize);
            LOG.debug("depth " + depth);
            LOG.debug("nodeSize " + nodeSize);
        }

        // this is just to make sure we have the right nodeSize for our depth
        // of choice
        Assert.that(nodeSize >= fileSize / maxNodes);
        Assert.that(nodeSize < (fileSize / maxNodes) * 2);

        List nodes;
        // do the actual hashing
        try {
            nodes = createTTNodes(nodeSize, fileSize, is);
        } catch (IOException ioe) {
            LOG.debug(ioe);
            return null;
        }

        // calculate the intermediary nodes to get the root hash & others.
        List allNodes = createAllNodes(nodes);
        return new HashTree(allNodes, sha1.toString(), fileSize);
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
     * @return HashTree if we successfully read from the network or null if
     *         there was an error.
     */
    public static HashTree createHashTree(InputStream is, String sha1,
                                          String root32, long fileSize)
                                          throws IOException {
        if(LOG.isTraceEnabled())
            LOG.trace("reading " + sha1 + "." + root32 + " dime data.");
        return new HashTree(HashTreeHandler.read(is, fileSize, root32),
                            sha1, fileSize);
    }

    /**
     * This method returns the ranges of a file that do no match the expected
     * TigerTree hashes
     * 
     * @param is
     *            an <tt>InputStream</tt>
     * @return List of <tt>Interval</tt>
     * @throws IOException
     *             if there was a problem reading the file
     */
    public List getCorruptRanges(InputStream is) throws IOException {
        LOG.trace("getting corrupt ranges ");

        List ret = new ArrayList();
        // calculate the node size using FILE_SIZE and DEPTH.
        // nodeSize = 2^(log2Ceil(FILE_SIZE) - DEPTH);
        int n = log2Ceil((int) FILE_SIZE) - DEPTH;
        // 2^n
        int nodeSize = 1 << n;
        List fileHashes = createTTNodes(nodeSize, FILE_SIZE, is);
        for (int i = 0; i < fileHashes.size(); i++) {
            byte[] aHash = (byte[]) fileHashes.get(i);
            byte[] bHash = (byte[]) NODES.get(i);
            for (int j = 0; j < aHash.length; j++) {
                if (aHash[j] != bHash[j]) {
                    Interval in =
                        new Interval(
                            i * nodeSize,
                            (int) Math.min(
                                (i + 1) * nodeSize - 1,
                                FILE_SIZE - 1));
                    ret.add(in);
                    if (LOG.isDebugEnabled())
                        LOG.debug(
                            Base32.encode(ROOT_HASH)
                                + " -> found corrupted range: "
                                + in);
                    break;
                } 
            }
        }
        return ret;
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
        if (LOG.isDebugEnabled())
            LOG.debug("depth " + DEPTH + " file " + FILE_SIZE);
        return (DEPTH == calculateDepth(FILE_SIZE));
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
    
    /**
     * @return all nodes.
     */
    public List getAllNodes() {
        if(ALL_NODES == null) {
            if(DEPTH <= MAX_DEPTH_TO_STORE)
                ALL_NODES =
                    Collections.unmodifiableList(createAllNodes(NODES));
            else
                return createAllNodes(NODES);
        }
        
        return ALL_NODES;
    }

    /**
     * Writes this HashTree to the specified OutputStream using DIME.
     */
    public void write(OutputStream out) throws IOException {
        new HashTreeHandler(this).write(out);
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
        if (size < 256 * KB)
            return 0;
        else if (size < 512 * KB)
            return 1;
        else if (size < MB)
            return 2;
        else if (size < 2 * MB)
            return 3;
        else if (size < 5 * MB)
            return 4;
        else if (size < 10 * MB)
            return 5;
        else if (size < 20 * MB)
            return 6;
        else if (size < 50 * MB)
            return 7;
        else if (size < 100 * MB)
            return 8;
        else
            return 9;
    }

    /*
     * Static helper methods
     */

    /*
     * Create a generation of nodes. It is very important that nodeSize equals
     * 2^n (n>=10) or we will not get the expected generation of nodes of a
     * Merkle HashTree
     */
    private static List createTTNodes(int nodeSize, long fileSize,
                                      InputStream is) throws IOException {
        List ret = new ArrayList();
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
                    Thread.sleep((System.currentTimeMillis() - time) * 2);
                } catch (InterruptedException ie) {
                    throw new IOException("interrupted during hashing operation");
                }
                time = System.currentTimeMillis();
            }
            // node hashed, add the hash to our internal Vector.
            ret.add(tt.digest());
            // if read == -1 && offset != fileSize there is something wrong
            if(!(read == -1) == (offset == fileSize))
                Assert.that(false, "read: " + read + 
                            ", offset: " + offset +
                            ", fileSize: " + fileSize);
        }
        return ret;
    }

    /*
     * Iterative method to generate the parent nodes of an arbitrary
     * depth.
     *
     * The 0th element of the returned List will always be a List of size
     * 1, containing a byte[] of the root hash.
     */
    private static List createAllNodes(List nodes) {
        List allNodes = new ArrayList();
        allNodes.add(nodes);
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
    static List createParentGeneration(List nodes) {
        MessageDigest md = new Tiger();
        List ret = new ArrayList();
        Iterator iter = nodes.iterator();
        while (iter.hasNext()) {
            byte[] left = (byte[]) iter.next();
            if (iter.hasNext()) {
                byte[] right = (byte[]) iter.next();
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

    // calculates the next n with 2^n > number
    private static int log2Ceil(int number) {
        int n = 0;
        while (number > 1) {
            number++; // for rounding up.
            number >>>= 1;
            n++;
        }
        return n;
    }
}