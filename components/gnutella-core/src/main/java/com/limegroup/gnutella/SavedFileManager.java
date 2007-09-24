package com.limegroup.gnutella;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Comparators;
import org.limewire.concurrent.ExecutorsHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.settings.SharingSettings;

/**
 * Singleton that manages saved files.
 *
 * Every three minutes it erases the stored data and adds new information,
 * as read from the disk.
 */
@Singleton
public final class SavedFileManager implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(SavedFileManager.class);
    
    /** The queue that the task runs in. */
    private static final ExecutorService QUEUE = ExecutorsHelper.newProcessingQueue("SavedFileLoader");
       
    private final UrnCache urnCache;
    
    @Inject
    SavedFileManager(UrnCache urnCache,
                     @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        this.urnCache = urnCache;
        
        // TODO: move to an initialize method!
        // Run the task every three minutes, starting in 10 seconds.
        backgroundExecutor.scheduleWithFixedDelay(this, 10 * 1000, 3 * 60 * 1000, TimeUnit.MILLISECONDS);
    }
    
    /**
     * A Set of URNs in the saved folder.
     *
     * LOCKING: Obtain this
     */
    private Set<URN> _urns = new HashSet<URN>();

    /**
     * A set of the filenames of the saved files.
     *
     * LOCKING: Obtain this
     */
    private Set<String> _names =
        new TreeSet<String>(Comparators.caseInsensitiveStringComparator());
        
    /**
     * Adds a new Saved File with the given URNs.
     */
    public synchronized void addSavedFile(File f, Set<? extends URN> urns) {
        if(LOG.isDebugEnabled())
            LOG.debug("Adding: " + f + " with: " + urns);
        
        _names.add(f.getName());
        for(URN urn : urns)
            _urns.add(urn);
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
        QUEUE.execute(new Runnable() {
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
        Set<URN> urns = new HashSet<URN>();
        Set<String> names = new TreeSet<String>(Comparators.caseInsensitiveStringComparator());
        UrnCallback callback = new UrnCallback() {
            public void urnsCalculated(File f, Set<? extends URN> urns) {
                synchronized(SavedFileManager.this) {
                    _urns.addAll(urns);
                }
            }
            
            public boolean isOwner(Object o) {
                return o == SavedFileManager.this;
            }
        };
        
        Set<File> saveDirs = SharingSettings.getAllSaveDirectories();
        for(File next : saveDirs)
            loadDirectory(next, urns, names, callback);
            
        synchronized(this) {
            _urns.addAll(urns);
            _names.addAll(names);
        }
    }
    
    /**
     * Loads a single saved directory.
     */
    private void loadDirectory(File directory, Set<? super URN> tempUrns, Set<String> tempNames, UrnCallback callback) {
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
            Set<URN> urns = urnCache.getUrns(file);
            if(urns.isEmpty()) // if not calculated, calculate at some point.
                urnCache.calculateAndCacheUrns(file, callback);
            else // otherwise, add without waiting.
                ((Collection<? super URN>)tempUrns).addAll(urns);
        }
    }
}
