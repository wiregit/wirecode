package com.limegroup.gnutella.tigertree;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.io.IOUtils;
import org.limewire.util.CommonUtils;
import org.limewire.util.ConverterObjectInputStream;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;

import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.URN;

/**
 * @author Gregorio Roper
 * 
 * This class maps SHA1_URNs to TigerTreeCache
 */
@Singleton
public final class TigerTreeCache {
    
    /**
     * The ProcessingQueue to do the hashing.
     */
    private static final ExecutorService QUEUE = 
        ExecutorsHelper.newProcessingQueue("TreeHashTread");

    private static final Log LOG =
        LogFactory.getLog(TigerTreeCache.class);

    /**
     * A root tree that is not fully constructed yet
     */
    private static final URN BUSH = URN.INVALID;
    
    /**
     * SHA1 -> TTROOT mapping
     */
    private static Map<URN, URN> ROOT_MAP;
    /**
     * TigerTreeCache container.
     */
    private static Map<URN, HashTree> TREE_MAP;

    /**
     * File where the old Mapping SHA1->TIGERTREE is stored
     */
    private static final File OLD_CACHE_FILE =
        new File(CommonUtils.getUserSettingsDir(), "ttree.cache");
    
    /**
     * File where the Mapping SHA1->TTROOT is stored
     */
    private static final File ROOT_CACHE_FILE =
        new File(CommonUtils.getUserSettingsDir(), "ttroot.cache");
    
    /**
     * File where the Mapping TTROOT->TIGERTREE is stored
     */
    private static final File TREE_CACHE_FILE =
        new File(CommonUtils.getUserSettingsDir(), "ttrees.cache");
        
    /**
     * Whether or not data dirtied since the last time we saved.
     */
    private volatile static boolean dirty = false;        

    
    public HashTree getHashTreeAndWait(FileDesc fd, long timeout) throws InterruptedException, TimeoutException {
        if (fd instanceof IncompleteFileDesc) {
            throw new IllegalArgumentException("fd must not inherit from IncompleFileDesc");
        }
        
        synchronized (this) {
            URN root = ROOT_MAP.get(fd.getSHA1Urn());
            if (root != null && root != BUSH)
                return TREE_MAP.get(root);

            ROOT_MAP.put(fd.getSHA1Urn(), BUSH);
        }
        
        Future<?> future = QUEUE.submit(new HashRunner(fd));
        try {
            future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        
        synchronized (this) {
            URN root = ROOT_MAP.get(fd.getSHA1Urn());
            if (root != null && root != BUSH)
                return TREE_MAP.get(root);
        }
        
        throw new RuntimeException("hash tree was not calculated");
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
        
        URN root = ROOT_MAP.get(fd.getSHA1Urn());
        if (root == null || root == BUSH)
            return null;
        HashTree tree = TREE_MAP.get(root);
        if (tree == null) {
            ROOT_MAP.put(fd.getSHA1Urn(), BUSH);
            QUEUE.execute(new HashRunner(fd));
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
        if (!sha1.isSHA1())
            throw new IllegalArgumentException();
        URN root = ROOT_MAP.get(sha1);
        if (root == null || root == BUSH)
            return null;
        
        return TREE_MAP.get(root);
    }
    
    /**
     * @return a TTROOT urn matching the sha1 urn
     */
    public synchronized URN getTTROOT(URN sha1) {
        if (!sha1.isSHA1())
            throw new IllegalArgumentException();
        URN root = ROOT_MAP.get(sha1);
        if (root == BUSH)
            root = null;
        return root;
    }
    
    /**
     * Purges the HashTree for this URN.
     */
    public synchronized void purgeTree(URN sha1) {
        if (!sha1.isSHA1())
            throw new IllegalArgumentException();
        URN root = ROOT_MAP.remove(sha1);
        if (root == null)
            return;
        if(TREE_MAP.remove(root) != null)
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
        URN root = URN.createTTRootUrn(tree.getRootHash());
        addRoot(sha1, root);
        if (tree.isGoodDepth()) {
            ROOT_MAP.put(sha1, root);
            TREE_MAP.put(root, tree);
            dirty = true;
            if (LOG.isDebugEnabled())
                LOG.debug("added hashtree for urn " +
                          sha1 + ";" + tree.getRootHash());
        } else if (LOG.isDebugEnabled())
            LOG.debug("hashtree for urn " + sha1 + " had bad depth");
    }
    
    public static synchronized void addRoot(URN sha1, URN ttroot) {
        if (!sha1.isSHA1() || !ttroot.isTTRoot())
            throw new IllegalArgumentException();
        dirty |= ttroot.equals(ROOT_MAP.put(sha1,ttroot));
    }

    /**
     * private constructor
     */
    //DPINJ: Delay deserialization...
    private TigerTreeCache() {
        // try reading the new format first
        try {
            loadCaches();
            return;
        } catch (Throwable t) {
            LOG.info("didn't load new caches",t);
        }
        
        // otherwise read the old format.
        Map<URN, HashTree> m = createMap();
        ROOT_MAP = new HashMap<URN,URN>(m.size());
        TREE_MAP = new HashMap<URN,HashTree>(m.size());
        for (URN urn : m.keySet()) {
            HashTree tree = m.get(urn);
            URN root = URN.createTTRootUrn(tree.getRootHash());
            ROOT_MAP.put(urn, root);
            TREE_MAP.put(root,tree);
        }
    }

    /**
     * Loads values from cache file, if available
     * 
     * @return Map of SHA1->HashTree
     */
    private static Map<URN, HashTree> createMap() {
        File toRead = OLD_CACHE_FILE;
        try {
            toRead = FileUtils.getCanonicalFile(toRead);
        } catch (IOException ignore){}
        
        ObjectInputStream ois = null;
        try {
            ois = new ConverterObjectInputStream(
                    new BufferedInputStream(
                        new FileInputStream(toRead)));
            return GenericsUtils.scanForMap(ois.readObject(), URN.class, HashTree.class, GenericsUtils.ScanMode.REMOVE);
        } catch(Throwable t) {
            LOG.error("Can't read tiger trees", t);
            return new HashMap<URN, HashTree>();
        } finally {
            IOUtils.close(ois);
        }
    }
    
    /**
     * Loads values from the root and tree caches
     */
    private static void loadCaches() throws IOException, ClassNotFoundException {
        Object roots = FileUtils.readObject(ROOT_CACHE_FILE);
        Object trees = FileUtils.readObject(TREE_CACHE_FILE);

        Map<URN,URN> rootsMap = GenericsUtils.scanForMap(roots, URN.class, URN.class, GenericsUtils.ScanMode.REMOVE);
        Map<URN,HashTree> treesMap = GenericsUtils.scanForMap(trees, URN.class, HashTree.class, GenericsUtils.ScanMode.REMOVE);
        
        // remove roots that don't have a sha1 mapping for them
        // cause we can't use them.
        treesMap.keySet().retainAll(rootsMap.values());
        
        // and make sure urns are the correct type
        for (Iterator<Map.Entry<URN, URN>> iter = rootsMap.entrySet().iterator();iter.hasNext();) {
            Map.Entry<URN, URN> e = iter.next();
            if (!e.getKey().isSHA1() || !e.getValue().isTTRoot())
                iter.remove();
        }
        
        for (Iterator<URN> iter = treesMap.keySet().iterator(); iter.hasNext();) {
            URN urn = iter.next();
            if (!urn.isTTRoot())
                iter.remove();
        }
        
        // Note: its ok to have roots without trees.
        
        ROOT_MAP = rootsMap;
        TREE_MAP = treesMap;
    }

    /**
     * Removes any stale entries from the map so that they will automatically
     * be replaced.
     * 
     * @param map
     *            the <tt>Map</tt> to check
     */
    private static void removeOldEntries(Map<URN,URN> roots, Map <URN, HashTree> map, FileManager fileManager, DownloadManager downloadManager) {
        // discard outdated info
        Iterator<URN> iter = roots.keySet().iterator();
        while (iter.hasNext()) {
            URN sha1 = iter.next();
            if (roots.get(sha1) != BUSH) {
                if (fileManager.getFileDescForUrn(sha1) != null)
                    continue;
                else if (downloadManager.getIncompleteFileManager().getFileForUrn(sha1) != null)
                    continue;
                else if (Math.random() > map.size() / 200)
                    // lazily removing entries if we don't have
                    // that many anyway. Maybe some of the files are
                    // just temporarily unshared.
                    continue;
            }
            iter.remove();
            map.remove(roots.get(sha1));
            dirty = true;
        }
    }

    /**
     * Write cache so that we only have to calculate them once.
     */
    public synchronized void persistCache(FileManager fileManager, DownloadManager downloadManager) {
        if(!dirty)
            return;
        //It's not ideal to hold a lock while writing to disk, but I doubt
        // think
        //it's a problem in practice.
        removeOldEntries(ROOT_MAP, TREE_MAP, fileManager, downloadManager);

        try {
            FileUtils.writeObject(ROOT_CACHE_FILE, ROOT_MAP);
            FileUtils.writeObject(TREE_CACHE_FILE, TREE_MAP);
            dirty = false;
        } catch (IOException e) {} 
    }

    /**
     * Simple runnable that processes the hash of a FileDesc.
     */
    private class HashRunner implements Runnable {
        
        private final FileDesc FD;

        HashRunner(FileDesc fd) {
            FD = fd;
        }

        public void run() {
            try {
                URN sha1 = FD.getSHA1Urn();
                // if it was scheduled multiple times, ignore latter times.
                if (TigerTreeCache.this.getHashTree(sha1) == null) {
                    HashTree tree = HashTree.createHashTree(FD);
                    addHashTree(sha1, tree);
                }
            } catch(IOException ignored) {}
        }
    }
}
