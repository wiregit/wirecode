package com.limegroup.gnutella;

import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.Comparators;
import com.limegroup.gnutella.settings.SharingSettings;

import java.io.File;
import java.io.IOException;

import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Iterator;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Manages saved files.
 *
 * Every minute it erases the stored data and adds new information,
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
     * A Set of URNs in the saved folder.
     *
     * This structure is replaced every time the runnable
     * finishes loading.
     *
     * LOCKING: Obtain this
     */
    private Set /* of URN  */ _urns = new HashSet();

    /**
     * A set of the filenames of the saved files.
     *
     * This structure is replaced every time the runnable
     * finishes loading.
     *
     * LOCKING: Obtain this
     */
    private Set /* of String */ _names =
        new TreeSet(Comparators.caseInsensitiveStringComparator());
        
    /**
     * Whether or not we are currently loading the saved files.
     */
    private volatile boolean _loading = false;
        
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
     * Returns true if this is already loading or if FileManager
     * is still loading.
     */
    public synchronized boolean isLoading() {
        return _loading || !RouterService.getFileManager().isLoadFinished();
    }
    
    /**
     * Attempts to load the saved files.
     */
    public void run() {
        synchronized(this) {
            if(!isLoading()) {
                _loading = true;
                Thread t = new ManagedThread(new Runnable() {
                    public void run() {
                        try {
                            load();
                        } finally {
                            _loading = false;
                        }
                    }
                }, "SavedFileProcessor");
                t.setDaemon(true);
                t.start();
            }
        }
    }    
    
    /**
     * Resets the map to contain all the files in the saved directory.
     *
     * Does nothing if this is already loading, or if FileManager
     * is still loading.
     */
    private void load() {
        LOG.trace("Loading Saved Files");
        Set urns = new HashSet();
        Set names = new TreeSet(Comparators.caseInsensitiveStringComparator());
        File saveDirectory = SharingSettings.getSaveDirectory();
        String[] saved = saveDirectory.list();
        if(saved == null)
            saved = new String[0];
        for(int i = 0; i < saved.length; i++) {
            String name = saved[i];
            File file = new File(saveDirectory, name);
            if(!file.isFile() || !file.exists())
                continue;
            if(LOG.isTraceEnabled())
                LOG.trace("Loading: " + file);
            names.add(name);
            Set fileUrns;
            try {
                fileUrns = FileDesc.calculateAndCacheURN(file);  
                for(Iterator j = fileUrns.iterator(); j.hasNext(); )
                    urns.add(j.next());
            }
            catch(IOException ignored) {}
            catch(InterruptedException ignored) {}
        }
        synchronized(this) {
            // Now that we have a new set of URNs & names,
            // replace the old ones.
            _names = names;
            _urns = urns;
        }
        LOG.trace("Finished loading saved Files.");
    }
}
