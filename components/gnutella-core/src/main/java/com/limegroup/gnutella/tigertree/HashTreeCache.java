package com.limegroup.gnutella.tigertree;

import org.limewire.io.URNImpl;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.Library;

/**
 * A cache of HashTrees that can be serialized between session.
 * <p>
 * Also maintains a mapping between SHA1 URNs and their associated tree.
 */
public interface HashTreeCache {
    
    /** Caches the root for the file desc, calculating it if necessary. */
    public URNImpl getOrScheduleHashTreeRoot(FileDesc fd);

    /**
     * If HashTree wasn't found, schedule file for hashing.
     * 
     * @param fd
     *            the <tt>FileDesc</tt> for which we want to obtain the
     *            HashTree
     * @return HashTree for File
     */
    public HashTree getHashTree(FileDesc fd);

    /**
     * Retrieves the cached HashTree for this URN.
     * 
     * @param sha1
     *            the <tt>URN</tt> for which we want to obtain the HashTree
     * @return HashTree for URN
     */
    public HashTree getHashTree(URNImpl sha1);

    /**
     * @return a TTROOT urn matching the sha1 urn
     */
    public URNImpl getHashTreeRootForSha1(URNImpl sha1);

    /**
     * Purges the HashTree for this URN.
     */
    public void purgeTree(URNImpl sha1);

    /**
     * Add a HashTree to the internal list if the tree depth is sufficient, null otherwise.
     * 
     * @param sha1
     *            the SHA1- <tt>URN</tt> of a file
     * @param tree
     *            the <tt>HashTree</tt>
     */
    public HashTree addHashTree(URNImpl sha1, HashTree tree);

    /** Marks the ttroot as the root for that sha1. */
    public void addRoot(URNImpl sha1, URNImpl ttroot);

    /**
     * Write cache so that we only have to calculate them once.
     */
    public void persistCache(Library library, DownloadManager downloadManager);

}