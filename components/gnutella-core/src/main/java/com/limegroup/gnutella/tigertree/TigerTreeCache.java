package com.limegroup.gnutella.tigertree;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.ConverterObjectInputStream;
import com.limegroup.gnutella.util.ProcessingQueue;

/**
 * @author Gregorio Roper
 * 
 * This class maps SHA1_URNs to TigerTreeCache
 */
public final class TigerTreeCache {

    /**
     * TigerTreeCache instance variable.
     */
    private static TigerTreeCache instance = null;
    
    /**
     * The ProcessingQueue to do the hashing.
     */
    private static final ProcessingQueue QUEUE = 
        new ProcessingQueue("TreeHashTread");

    private static final Log LOG =
        LogFactory.getLog(TigerTreeCache.class);

    /**
     * A tree that is not fully constructed yet
     */
    private static final Object BUSH = new Object();
    
    /**
     * TigerTreeCache container.
     */
    private static Map /* SHA1_URN -> HashTree */ TREE_MAP;

    /**
     * File where the Mapping SHA1->TIGERTREE is stored
     */
    private static final File CACHE_FILE =
        new File(CommonUtils.getUserSettingsDir(), "ttree.cache");
        
    /**
     * Whether or not data dirtied since the last time we saved.
     */
    private static boolean dirty = false;        

    /**
     * Returns the <tt>TigerTreeCache</tt> instance.
     * 
     * @return the <tt>TigerTreeCache</tt> instance
     */
    public synchronized static TigerTreeCache instance() {
        if(instance == null)
            instance = new TigerTreeCache();
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
        Object obj = TREE_MAP.get(fd.getSHA1Urn());
        if (obj != null && obj.equals(BUSH))
            return null;
        HashTree tree = (HashTree) obj;
        if (tree == null) {
            TREE_MAP.put(fd.getSHA1Urn(), BUSH);
            QUEUE.add(new HashRunner(fd));
        }
        return tree;
    }

    /**
     * Retrieves the cached HashTree for this URN.
     * 
     * @param sha1
     *            the <tt>URN</tt> for which we want to obtain the HashTree
     * @return HashTree for URN
     */
    public synchronized HashTree getHashTree(URN sha1) {
        Object tree = TREE_MAP.get(sha1);
        
        if (tree != null && tree.equals(BUSH))
            return null;
        
        return (HashTree)tree;
    }
    
    /**
     * Purges the HashTree for this URN.
     */
    public synchronized void purgeTree(URN sha1) {
        if(TREE_MAP.remove(sha1) != null)
            dirty = true;
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
            dirty = true;
            if (LOG.isDebugEnabled())
                LOG.debug("added hashtree for urn " +
                          sha1 + ";" + tree.getRootHash());
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
            ois = new ConverterObjectInputStream(
                    new BufferedInputStream(
                        new FileInputStream(CACHE_FILE)));
            Map map = (Map)ois.readObject();
            if(map != null) {
                for(Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
                    // Remove values that aren't correct.
                    Map.Entry next = (Map.Entry)i.next();
                    Object key = next.getKey();
                    Object value = next.getValue();
                    if( !(key instanceof URN) || !(value instanceof HashTree) )
                        i.remove();
                }
            }
            return map;
        } catch(Throwable t) {
            LOG.error("Can't read tiger trees", t);
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
            if (map.get(sha1) != BUSH) {
                if (RouterService.getFileManager().getFileDescForUrn(sha1) != null)
                    continue;
                else if (RouterService.getDownloadManager()
                        .getIncompleteFileManager().getFileForUrn(sha1) != null)
                    continue;
                else if (Math.random() > map.size() / 200)
                    // lazily removing entries if we don't have
                    // that many anyway. Maybe some of the files are
                    // just temporarily unshared.
                    continue;
            }
            iter.remove();
            dirty = true;
        }
    }

    /**
     * Write cache so that we only have to calculate them once.
     */
    public synchronized void persistCache() {
        if(!dirty)
            return;
        
        //It's not ideal to hold a lock while writing to disk, but I doubt
        // think
        //it's a problem in practice.
        removeOldEntries(TREE_MAP);

        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(CACHE_FILE)));
            oos.writeObject(TREE_MAP);
        } catch (IOException e) {
            ErrorService.error(e);
        } finally {
            if(oos != null) {
                try {
                    oos.close();
                } catch(IOException ignored) {}
            }
        }
        
        dirty = true;
    }

    /**
     * Simple runnable that processes the hash of a FileDesc.
     */
    private static class HashRunner implements Runnable {
        private final FileDesc FD;

        HashRunner(FileDesc fd) {
            FD = fd;
        }

        public void run() {
            try {
                URN sha1 = FD.getSHA1Urn();
                // if it was scheduled multiple times, ignore latter times.
                if(TigerTreeCache.instance().getHashTree(sha1) == null) {
                    HashTree tree = HashTree.createHashTree(FD);
                    addHashTree(sha1, tree);
                }
            } catch(IOException ignored) {}
        }
    }
}
