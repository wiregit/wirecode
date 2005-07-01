package com.limegroup.gnutella;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.Comparators;
import com.limegroup.gnutella.util.ProcessingQueue;

/**
 * Singleton that manages saved files.
 *
 * Every three minutes it erases the stored data and adds new information,
 * as read from the disk.
 */
public final class SavedFileManager implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(SavedFileManager.class);
    
    private static SavedFileManager INSTANCE = new SavedFileManager();
    public static SavedFileManager instance() { return INSTANCE; }
    private SavedFileManager() {
        // Run the task every three minutes, starting in 10 seconds.
        RouterService.schedule(this, 10 * 1000, 3 * 60 * 1000);
    }
    
    /**
     * The queue that the task runs in.
     */
    private static final ProcessingQueue QUEUE = new ProcessingQueue("SavedFileLoader");
    
    
    /**
     * A Set of URNs in the saved folder.
     *
     * LOCKING: Obtain this
     */
    private Set /* of URN  */ _urns = new HashSet();

    /**
     * A set of the filenames of the saved files.
     *
     * LOCKING: Obtain this
     */
    private Set /* of String */ _names =
        new TreeSet(Comparators.caseInsensitiveStringComparator());
        
    /**
     * Adds a new Saved File with the given URNs.
     */
    public synchronized void addSavedFile(File f, Set urns) {
        if(LOG.isDebugEnabled())
            LOG.debug("Adding: " + f + " with: " + urns);
        
        _names.add(f.getName());
        for(Iterator i = urns.iterator(); i.hasNext();)
            _urns.add(i.next());
    }
    
    /**
     * Determines if the given URN or name is saved.
     */
    public synchronized boolean isSaved(URN urn, String name) {
        return (urn != null && _urns.contains(urn)) || _names.contains(name);
    }
    
    /**
     * Attempts to load the saved files.
     */
    public void run() {
        QUEUE.add(new Runnable() {
            public void run() {
                load();
            }
        });
    }
    
    /**
     * Loads up any names & urns 
     */
    private void load() {
        LOG.trace("Loading Saved Files");
        Set urns = new HashSet();
        Set names = new TreeSet(Comparators.caseInsensitiveStringComparator());
        UrnCallback callback = new UrnCallback() {
            public void urnsCalculated(File f, Set urns) {
                synchronized(SavedFileManager.this) {
                    _urns.addAll(urns);
                }
            }
            
            public boolean isOwner(Object o) {
                return o == SavedFileManager.this;
            }
        };
        
        Set saveDirs = SharingSettings.getAllSaveDirectories();
        for(Iterator i = saveDirs.iterator(); i.hasNext(); )
            loadDirectory((File)i.next(), urns, names, callback);
            
        synchronized(this) {
            _urns.addAll(urns);
            _names.addAll(names);
        }
    }
    
    /**
     * Loads a single saved directory.
     */
    private void loadDirectory(File directory, Set tempUrns, Set tempNames, UrnCallback callback) {
        File[] savedFiles = directory.listFiles();
        if(savedFiles == null)
            return;
            
        for(int i = 0; i < savedFiles.length; i++) {
            File file = savedFiles[i];
            if(!file.isFile() || !file.exists())
                continue;
            if(LOG.isTraceEnabled())
                LOG.trace("Loading: " + file);
                
            tempNames.add(file.getName());
            Set urns = UrnCache.instance().getUrns(file);
            if(urns.isEmpty()) // if not calculated, calculate at some point.
                UrnCache.instance().calculateAndCacheUrns(file, callback);
            else // otherwise, add without waiting.
                tempUrns.addAll(urns);
        }
    }
}
