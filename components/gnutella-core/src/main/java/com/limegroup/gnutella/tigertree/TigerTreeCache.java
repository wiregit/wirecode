package com.limegroup.gnutella.tigertree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.ManagedThread;
import com.sun.java.util.collections.HashMap;
import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.Map;
import com.sun.java.util.collections.Set;

/**
 * @author Gregorio Roper
 * 
 * This class maps SHA1_URNs to TigerTreeCache
 */
public final class TigerTreeCache implements Serializable {

    /**
     * TigerTreeCache instance variable.
     */
    private static TigerTreeCache instance = null;

    /**
     * List of FileDesc containing the Files to hash next
     */
    private static Set toHash = new HashSet();

    /**
     * ManagedThread doing the hashing, must be null when inactive
     */
    static Thread _runner = null;

    private static transient final Log LOG =
        LogFactory.getLog(TigerTreeCache.class);

    /**
     * TigerTreeCache container.
     */
    private static Map /* SHA1_URN -> HashTree */
    TREE_MAP;

    /**
     * File where the Mapping SHA1->TIGERTREE is stored
     */
    private static final File CACHE_FILE =
        new File(CommonUtils.getUserSettingsDir(), "tigertree.cache");

    /**
     * Returns the <tt>TigerTreeCache</tt> instance.
     * 
     * @return the <tt>TigerTreeCache</tt> instance
     */
    public static synchronized TigerTreeCache instance() {
        if (instance == null) {
            instance = new TigerTreeCache();
        }
        return instance;
    }

    /**
     * If HashTree wasn't found, schedule file for hashing
     * 
     * @param fd
     *            the <tt>FileDesc</tt> for which we want to obtain the
     *            HashTree
     * @return HashTree for File
     */
    public synchronized HashTree getHashTree(FileDesc fd) {
        HashTree tree = (HashTree) TREE_MAP.get(fd.getSHA1Urn());
        if (tree == null)
            scheduleForHashing(fd);
        if (LOG.isDebugEnabled() && tree != null)
            LOG.debug("returned hashtree for urn " + fd.getSHA1Urn() + " -> " + tree.getRootHash() );

        return tree;
    }

    /**
     * Doesn't do any hashing!
     * 
     * @param sha1
     *            the <tt>URN</tt> for which we want to obtain the HashTree
     * @return HashTree for URN
     */
    public synchronized HashTree getHashTree(URN sha1) {
        HashTree tree = (HashTree) TREE_MAP.get(sha1);
        return tree;
    }

    /**
     * add a hashtree to the internal list if the tree depth is sufficient
     * 
     * @param sha1
     *            the SHA1- <tt>URN</tt> of a file
     * @param tree
     *            the <tt>HashTree</tt>
     */
    public static synchronized void addHashTree(URN sha1, HashTree tree) {
        if (tree.isGoodDepth()) {
            TREE_MAP.put(sha1, tree);
            if (LOG.isDebugEnabled())
                LOG.debug("added hashtree for urn " + sha1 + ";" + tree.getRootHash());
        } else if (LOG.isDebugEnabled())
            LOG.debug("hashtree for urn " + sha1 + " had bad depth");
    }

    /**
     * private constructor
     */
    private TigerTreeCache() {
        TREE_MAP = createMap();
    }

    /**
     * Loads values from cache file, if available
     * 
     * @return Map of SHA1->HashTree
     */
    private static Map createMap() {
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(CACHE_FILE));
            return (Map) ois.readObject();
        } catch (IOException e) {
            return new HashMap();
        } catch (ClassCastException e) {
            return new HashMap();
        } catch (ClassNotFoundException e) {
            return new HashMap();
        } catch (ArrayStoreException e) {
            return new HashMap();
        } catch (IndexOutOfBoundsException e) {
            return new HashMap();
        } catch (NegativeArraySizeException e) {
            return new HashMap();
        } catch (IllegalStateException e) {
            return new HashMap();
        } catch (SecurityException e) {
            return new HashMap();
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    // all we can do is try to close it
                }
            }
        }
    }

    /**
     * Removes any stale entries from the map so that they will automatically
     * be replaced.
     * 
     * @param map
     *            the <tt>Map</tt> to check
     */
    private static void removeOldEntries(Map map) {
        // discard outdated info
        Iterator iter = map.keySet().iterator();
        while (iter.hasNext()) {
            URN sha1 = (URN) iter.next();
            if (RouterService.getFileManager().getFileDescForUrn(sha1) != null)
                continue;
            else if (
                RouterService
                    .getDownloadManager()
                    .getIncompleteFileManager()
                    .getFileForUrn(
                    sha1)
                    != null)
                continue;
            else if (Math.random() > map.size() / 200)
                // lazily removing entries if we don't have
                // that many anyway. Maybe some of the files are
                // just temporarily unshared.
                continue;
            map.remove(sha1);
        }
    }

    private static void scheduleForHashing(FileDesc fd) {
        toHash.add(fd);
        if (_runner == null) {
            _runner = new ManagedThread(new HashRunner(toHash));
            toHash = new HashSet();
            _runner.setName("TreeHashThread");
            _runner.setDaemon(true);
            _runner.start();
        }
    }

    /**
     * Write cache so that we only have to calculate them once.
     */
    public synchronized void persistCache() {
        //It's not ideal to hold a lock while writing to disk, but I doubt
        // think
        //it's a problem in practice.
        removeOldEntries(TREE_MAP);
        try {
            ObjectOutputStream oos =
                new ObjectOutputStream(new FileOutputStream(CACHE_FILE));
            oos.writeObject(TREE_MAP);
            oos.close();
        } catch (Exception e) {
            ErrorService.error(e);
        }
    }

    private static class HashRunner implements Runnable {
        private final Set TODO;

        HashRunner(Set set) {
            TODO = set;
        }

        /**
         * @see java.lang.Runnable#run()
         */
        public void run() {
            try {
                for (Iterator iter = TODO.iterator(); iter.hasNext();) {
                    FileDesc desc = (FileDesc) iter.next();
                    HashTree tree = HashTree.createHashTree(desc);
                    addHashTree(desc.getSHA1Urn(), tree);
                }
            } finally {
                _runner = null;
            }
        }
    }
}
