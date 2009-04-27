package com.limegroup.gnutella.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.collection.CollectionUtils;
import org.limewire.collection.Comparators;
import org.limewire.core.settings.MessageSettings;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionPoint;
import org.limewire.lifecycle.Service;
import org.limewire.statistic.StatsUtils;
import org.limewire.util.RPNParser;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.routing.HashFunction;
import com.limegroup.gnutella.routing.QueryRouteTable;

/**
 * The list of all known files. This creates and maintains a list of 
 * directories and FileDescs. It also creates a set of FileLists which 
 * may contain subsets of all FileDescs. Files can be added to just the 
 * FileManager or loaded into both the FileManager and a specified FileList
 * once the FileDesc has been created. <p>
 *
 * This class is thread-safe.
 */
@Singleton 
class FileManagerImpl implements FileManager, Service {
    
    private final LibraryImpl managedFileList;
    
    @InspectionPoint("gnutella shared file list")
    private final GnutellaFileCollectionImpl gnutellaCollection;
    
    @InspectionPoint("incomplete file list")
    private final IncompleteFileCollectionImpl incompleteCollection;
    
    private final SharedFileCollectionImplFactory sharedFileCollectionImplFactory;
    
    private final GnutellaFileView gnutellaFileView;
    
    private final Map<String, SharedFileCollectionImpl> sharedCollections =
        new TreeMap<String,SharedFileCollectionImpl>(String.CASE_INSENSITIVE_ORDER);
    
    private Saver saver;
    
    /**
     * Whether the FileManager has been shutdown.
     */
    private volatile boolean shutdown;
    
    /** The background executor. */
    private final ScheduledExecutorService backgroundExecutor;

	/**
	 * Creates a new <tt>FileManager</tt> instance.
	 */
    @Inject
    public FileManagerImpl(LibraryImpl managedFileList,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            GnutellaFileCollectionImpl gnutellaCollectionImpl,
            IncompleteFileCollectionImpl incompleteFileCollectionImpl,
            SharedFileCollectionImplFactory sharedFileCollectionImplFactory) {
        
        this.backgroundExecutor = backgroundExecutor;
        this.managedFileList = managedFileList;
        this.gnutellaCollection = gnutellaCollectionImpl;
        this.gnutellaCollection.initialize();
        this.incompleteCollection = incompleteFileCollectionImpl;
        this.incompleteCollection.initialize();
        this.sharedFileCollectionImplFactory = sharedFileCollectionImplFactory;
        
        this.gnutellaFileView = new GnutellaFileViewImpl(gnutellaCollection);
    }

    @Override
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Shared Files");
    }

    @Override
    public void initialize() {
        managedFileList.initialize();
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }

    @Override
    public void start() {
      //  _data.clean(); // avoid cleaning for now, too slow.
      //  cleanIndividualFiles();
        LibraryConverter converter = new LibraryConverter();
        if(converter.isOutOfDate()) {
            managedFileList.fireLoading();
            converter.convert(managedFileList.getLibraryData());
        }
        managedFileList.loadManagedFiles();
        
        synchronized (this) {
            if (saver == null) {
                this.saver = new Saver();
                backgroundExecutor.scheduleWithFixedDelay(saver, 1, 1, TimeUnit.MINUTES);
            }
        }
    }

    @Override
    public void stop() {
        managedFileList.save();
        shutdown = true;
    }    
    
    @Override
    public Library getLibrary() {
        return managedFileList;
    }

    @Override
    public GnutellaFileCollection getGnutellaCollection() {
        return gnutellaCollection;
    }    
    
    @Override
    public synchronized SharedFileCollection getSharedCollection(String name) {
        return sharedCollections.get(name);
    }
    
    @Override
    public synchronized FileView getFileViewForId(String friendId) {
        return sharedCollections.get(friendId);
    }
    
    @Override
    public GnutellaFileView getGnutellaFileView() {
        return gnutellaFileView;
    }

    // TODO: The places these are used are in the UI and broken and based on "views" even
    //       though they should be based on collections.
    @Override
    public synchronized SharedFileCollection getOrCreateSharedCollection(String name) {
        SharedFileCollectionImpl fileList = sharedCollections.get(name);
        if(fileList == null) {
            fileList = sharedFileCollectionImplFactory.createSharedFileCollectionImpl(name);
            fileList.initialize();
            sharedCollections.put(name, fileList);
        }
        return fileList;
    }

    // TODO: The places these are used are in the UI and broken and based on "views"
    //       even though they should be based on collections.
    @Override
    public synchronized void removeSharedCollection(String name) {
        // if it was a valid key, remove saved references to it
        SharedFileCollectionImpl removeFileList = sharedCollections.get(name);
        if(removeFileList != null) {
            removeFileList.dispose();
            sharedCollections.remove(name);
        }
    }

    // TODO: This should be removing the FileView for the friend and have
    //       nothing to do with collections.
    public void unloadFilesForFriend(String friendName) {
        SharedFileCollectionImpl removeFileList = null;

        synchronized (this) {
            removeFileList = sharedCollections.get(friendName);

            if (removeFileList == null) {
                return;
            }
            sharedCollections.remove(friendName);
        }
        removeFileList.unload();

    }

    @Override
    public IncompleteFileCollection getIncompleteFileCollection() {
        return incompleteCollection;
    }
    
    /** A bunch of inspectables for FileManager */
    @InspectableContainer
    private class FMInspectables {
        /*
         * 1 - used to create smaller qrp table
         * 2 - just sends the current table
         */
        private static final int VERSION = 2;

        /** An inspectable that returns stats about hits, uploads & alts */
        @InspectionPoint("FileManager h/u/a stats")
        public final Inspectable FDS = new FDInspectable(false);
        /** An inspectable that returns stats about hits, uploads & alts > 0 */
        @InspectionPoint("FileManager h/u/a stats > 0")
        public final Inspectable FDSNZ = new FDInspectable(true);
        
        /** An inspectable that counts how many shared fds match a custom criteria */
        @InspectionPoint("FileManager custom criteria")
        public final Inspectable CUSTOM = new Inspectable() {
            @Override
            public Object inspect() {
                Map<String, Object> ret = new HashMap<String,Object>();
                ret.put("ver",1);
                ret.put("crit", MessageSettings.CUSTOM_FD_CRITERIA.getValueAsString());
                int total = 0;
                int matched = 0;
                try {
                    RPNParser parser = new RPNParser(MessageSettings.CUSTOM_FD_CRITERIA.get());
                    FileView shareList = getGnutellaFileView();
                    shareList.getReadLock().lock();
                    try {
                        for (FileDesc fd : shareList){
                            total++;
                            if (parser.evaluate(fd))
                                matched++;
                        }
                    } finally {
                        shareList.getReadLock().unlock();
                    }
                } catch (IllegalArgumentException badSimpp) {
                    ret.put("error",badSimpp.toString());
                    return ret;
                }
                ret.put("match",matched);
                ret.put("total",total);
                return ret;
            }
        };
        
        @InspectionPoint("friend file lists")
        public final Inspectable FRIEND = new Inspectable() {
            @Override
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                synchronized (FileManagerImpl.this) {
                    List<Integer> sizes = new ArrayList<Integer>(sharedCollections.size());
                    for (SharedFileCollection friendFileList : sharedCollections.values()) {
                        sizes.add(friendFileList.size());
                    }
                    data.put("sizes", sizes);
                }
                return data;
            }
        };
    }
    
    /** Inspectable with information about File Descriptors */
    private class FDInspectable implements Inspectable {
        private final boolean nonZero;
        /**
         * @param nonZero whether to return only results greater than 0
         */
        FDInspectable(boolean nonZero) {
            this.nonZero = nonZero;
        }
        
        @Override
        public Object inspect() {
            Map<String, Object> ret = new HashMap<String, Object>();
            ret.put("ver", FMInspectables.VERSION);
            // the actual values
            ArrayList<Double> hits = new ArrayList<Double>();
            ArrayList<Double> uploads = new ArrayList<Double>();
            ArrayList<Double> completeUploads = new ArrayList<Double>();
            ArrayList<Double> alts = new ArrayList<Double>();
            ArrayList<Double> keywords = new ArrayList<Double>();
            
            // differences for t-test 
            ArrayList<Double> altsHits = new ArrayList<Double>();
            ArrayList<Double> altsUploads = new ArrayList<Double>();
            ArrayList<Double> hitsUpload = new ArrayList<Double>();
            ArrayList<Double> hitsKeywords = new ArrayList<Double>();
            ArrayList<Double> uploadsToComplete = new ArrayList<Double>();
            
            Map<Integer, FileDesc> topHitsFDs = new TreeMap<Integer, FileDesc>(Comparators.inverseIntegerComparator());
            Map<Integer, FileDesc> topUpsFDs = new TreeMap<Integer, FileDesc>(Comparators.inverseIntegerComparator());
            Map<Integer, FileDesc> topAltsFDs = new TreeMap<Integer, FileDesc>(Comparators.inverseIntegerComparator());
            Map<Integer, FileDesc> topCupsFDs = new TreeMap<Integer, FileDesc>(Comparators.inverseIntegerComparator());

            List<FileDesc> fds;
            FileView shareList = getGnutellaFileView();
            shareList.getReadLock().lock();
            try {
                fds = CollectionUtils.listOf(shareList);
            } finally {
                shareList.getReadLock().unlock();
            }
            hits.ensureCapacity(fds.size());
            uploads.ensureCapacity(fds.size());
            int rare = 0;
            int total = 0;
            for(FileDesc fd : fds ) {
                if (fd instanceof IncompleteFileDesc)
                    continue;
                total++;
                if (fd.isRareFile())
                    rare++;
                // locking FM->ALM ok.
//                int numAlts = altLocManager.get().getNumLocs(fd.getSHA1Urn());
//                if (!nonZero || numAlts > 0) {
//                    alts.add((double)numAlts);
//                    topAltsFDs.put(numAlts,fd);
//                }
                int hitCount = fd.getHitCount();
                if (!nonZero || hitCount > 0) {
                    hits.add((double)hitCount);
                    topHitsFDs.put(hitCount, fd);
                }
                int upCount = fd.getAttemptedUploads();
                if (!nonZero || upCount > 0) {
                    uploads.add((double)upCount);
                    topUpsFDs.put(upCount, fd);
                }
                int cupCount = fd.getCompletedUploads();
                if (!nonZero || cupCount > 0) {
                    completeUploads.add((double)upCount);
                    topCupsFDs.put(cupCount, fd);
                }
                
                // keywords per fd
                double keywordsCount = 
                    HashFunction.getPrefixes(HashFunction.keywords(fd.getPath())).length;
                keywords.add(keywordsCount);
                
                // populate differences
                if (!nonZero) {
                    int index = hits.size() - 1;
                    hitsUpload.add(hits.get(index) - uploads.get(index));
                    altsHits.add(alts.get(index) - hits.get(index));
                    altsUploads.add(alts.get(index)  - uploads.get(index));
                    hitsKeywords.add(hits.get(index) - keywordsCount);
                    uploadsToComplete.add(uploads.get(index) - completeUploads.get(index));
                }
                ret.put("rare",Double.doubleToLongBits((double)rare / total));
            }

            ret.put("hits",StatsUtils.quickStatsDouble(hits).getMap());
            ret.put("hitsh", StatsUtils.getHistogram(hits, 10)); // small, will compress
            ret.put("ups",StatsUtils.quickStatsDouble(uploads).getMap());
            ret.put("upsh", StatsUtils.getHistogram(uploads, 10));
            ret.put("cups",StatsUtils.quickStatsDouble(completeUploads).getMap());
            ret.put("cupsh", StatsUtils.getHistogram(completeUploads, 10));
            ret.put("alts", StatsUtils.quickStatsDouble(alts).getMap());
            ret.put("altsh", StatsUtils.getHistogram(alts, 10));
            ret.put("kw", StatsUtils.quickStatsDouble(keywords).getMap());
            ret.put("kwh", StatsUtils.getHistogram(keywords, 10));
            
            // t-test values
            ret.put("hut",StatsUtils.quickStatsDouble(hitsUpload).getTTestMap());
            ret.put("aht",StatsUtils.quickStatsDouble(altsHits).getTTestMap());
            ret.put("aut",StatsUtils.quickStatsDouble(altsUploads).getTTestMap());
            ret.put("hkt",StatsUtils.quickStatsDouble(hitsKeywords).getTTestMap());
            ret.put("ucut",StatsUtils.quickStatsDouble(uploadsToComplete).getTTestMap());
            
            QueryRouteTable topHits = new QueryRouteTable();
            QueryRouteTable topUps = new QueryRouteTable();
            QueryRouteTable topCups = new QueryRouteTable();
            QueryRouteTable topAlts = new QueryRouteTable();
            Iterator<FileDesc> hitIter = topHitsFDs.values().iterator();
            Iterator<FileDesc> upIter = topUpsFDs.values().iterator();
            Iterator<FileDesc> cupIter = topCupsFDs.values().iterator();
            Iterator<FileDesc> altIter = topAltsFDs.values().iterator();
            for (int i = 0; i < 10; i++) {
                if (hitIter.hasNext())
                    topHits.add(hitIter.next().getPath());
                if (upIter.hasNext())
                    topUps.add(upIter.next().getPath());
                if (altIter.hasNext())
                    topAlts.add(altIter.next().getPath());
                if (cupIter.hasNext())
                    topCups.add(cupIter.next().getPath());
            }
            // we return all qrps, but since they will have very few entries
            // they will compress very well
            ret.put("hitsq",topHits.getRawDump());
            ret.put("upsq",topUps.getRawDump());
            ret.put("cupsq",topCups.getRawDump());
            ret.put("altsq",topAlts.getRawDump());
            
            return ret;
        }
    }
    
    private class Saver implements Runnable {
        public void run() {
            if (!shutdown && managedFileList.isLoadFinished()) {
                managedFileList.save();
            }
        }
    }
}