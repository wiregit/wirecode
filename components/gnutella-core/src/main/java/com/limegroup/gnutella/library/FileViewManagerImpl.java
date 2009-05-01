package com.limegroup.gnutella.library;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.collection.CollectionUtils;
import org.limewire.collection.Comparators;
import org.limewire.core.settings.MessageSettings;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionPoint;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.statistic.StatsUtils;
import org.limewire.util.RPNParser;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.routing.HashFunction;
import com.limegroup.gnutella.routing.QueryRouteTable;

@Singleton
class FileViewManagerImpl implements FileViewManager {
    
    private final LibraryImpl library;
    private final GnutellaFileCollectionImpl gnutellaView;
    private final IncompleteFileCollectionImpl incompleteView;
    private final SharedCollectionBackedFileViewImpl compositeView;
    
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Map<String, SharedCollectionBackedFileViewImpl> fileViews = new HashMap<String, SharedCollectionBackedFileViewImpl>();

    private final Collection<SharedFileCollection> sharedCollections = new ArrayList<SharedFileCollection>();

    @Inject
    public FileViewManagerImpl(LibraryImpl library, GnutellaFileCollectionImpl gnutellaCollection,
            IncompleteFileCollectionImpl incompleteCollection) {
        this.library = library;
        this.gnutellaView = gnutellaCollection;
        this.incompleteView = incompleteCollection;
        this.compositeView = new SharedCollectionBackedFileViewImpl(null, rwLock.readLock(),
                library);
    }
    
    @Inject void register(@AllFileCollections ListenerSupport<FileViewChangeEvent> viewListeners,
                          @AllFileCollections ListenerSupport<SharedFileCollectionChangeEvent> collectionListeners) {
        viewListeners.addListener(new EventListener<FileViewChangeEvent>() {
            @Override
            @BlockingEvent(queueName="FileViewManager")
            public void handleEvent(FileViewChangeEvent event) {
                switch(event.getType()) {
                case FILE_ADDED:
                    fileAdded(event.getFileDesc(), event.getFileView());
                    break;
                case FILE_REMOVED:
                    fileRemoved(event.getFileDesc(), event.getFileView());
                    break;
                case FILES_CLEARED:
                    collectionCleared(event.getFileView());
                    break;
                }
            }
        });
        
        collectionListeners.addListener(new EventListener<SharedFileCollectionChangeEvent>() {
            @Override
            @BlockingEvent(queueName="FileViewManager") 
            public void handleEvent(SharedFileCollectionChangeEvent event) {
                switch(event.getType()) {
                case COLLECTION_ADDED:
                    collectionAdded(event.getSource());
                    break;
                case COLLECTION_REMOVED:
                    collectionRemoved(event.getSource());
                    break;
                case SHARE_ID_ADDED:
                    shareIdAdded(event.getSource(), event.getShareId());
                    break;
                case SHARE_ID_REMOVED:
                    shareIdRemoved(event.getSource(), event.getShareId());
                    break;
                }
            }
        });
    }
    
    @Override
    public FileView getIncompleteFileView() {
        return incompleteView;
    }
    
    @Override
    public GnutellaFileView getGnutellaFileView() {
        return gnutellaView;
    }
    
    private void collectionAdded(SharedFileCollection collection) {
        rwLock.writeLock().lock();
        try {
            sharedCollections.add(collection);
            for(String id : collection.getSharedIdList()) {
                SharedCollectionBackedFileViewImpl view = fileViews.get(id);
                if(view != null) {
                    view.addNewBackingCollection(collection);
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    private void collectionRemoved(SharedFileCollection collection) {
        rwLock.writeLock().lock();
        try {
            sharedCollections.remove(collection);
            for(SharedCollectionBackedFileViewImpl view : fileViews.values()) {
                view.removeBackingCollection(collection);
            }
        } finally {
            rwLock.writeLock().unlock();
        }   
    }
    
    private void shareIdAdded(SharedFileCollection collection, String id) {
        rwLock.writeLock().lock();
        try {
            SharedCollectionBackedFileViewImpl view = fileViews.get(id);
            if(view != null) {
                view.addNewBackingCollection(collection);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    private void shareIdRemoved(SharedFileCollection collection, String id) {
        rwLock.writeLock().lock();
        try {
            SharedCollectionBackedFileViewImpl view = fileViews.get(id);
            if(view != null) {
                view.removeBackingCollection(collection);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    private void collectionCleared(FileView fileView) {
        rwLock.writeLock().lock();
        try {
            for(String id : ((SharedFileCollection)fileView).getSharedIdList()) {
                SharedCollectionBackedFileViewImpl view = fileViews.get(id);
                if(view != null) {
                    view.fileCollectionCleared(fileView);
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }   
    }

    private void fileRemoved(FileDesc fileDesc, FileView fileView) {
        rwLock.writeLock().lock();
        try {
            for(String id : ((SharedFileCollection)fileView).getSharedIdList()) {
                SharedCollectionBackedFileViewImpl view = fileViews.get(id);
                if(view != null) {
                    view.fileRemovedFromCollection(fileDesc, fileView);
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void fileAdded(FileDesc fileDesc, FileView fileView) {
        rwLock.writeLock().lock();
        try {
            for(String id : ((SharedFileCollection)fileView).getSharedIdList()) {
                SharedCollectionBackedFileViewImpl view = fileViews.get(id);
                if(view != null) {
                    view.fileAddedFromCollection(fileDesc, fileView);
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public FileView getCompositeSharedView() {
        return compositeView;
    }
    
    @Override
    public FileView getFileViewForId(String id) {
        SharedCollectionBackedFileViewImpl view;
        rwLock.readLock().lock();
        try {
            view = fileViews.get(id);
        } finally {
            rwLock.readLock().unlock();
        }
        
        if(view == null) {        
            rwLock.writeLock().lock();
            try {
                // recheck within lock -- it may have been created
                view = fileViews.get(id);
                if(view == null) {
                    view = createFileView(id);
                    fileViews.put(id, view);
                }
            } finally {
                rwLock.writeLock().unlock();   
            }
        }
        
        return view;
    }
    
    private SharedCollectionBackedFileViewImpl createFileView(String id) {
        SharedCollectionBackedFileViewImpl view = new SharedCollectionBackedFileViewImpl(id, rwLock.readLock(), library);
        initialize(view, id);
        return view;
    }
    
    private void initialize(SharedCollectionBackedFileViewImpl view, String id) {
        for(SharedFileCollection collection : sharedCollections) {
            if(collection.getSharedIdList().contains(id)) {
                view.addNewBackingCollection(collection);
            }
        }
    }
    
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
        
        @InspectionPoint("friend file views")
        public final Inspectable FRIEND = new Inspectable() {
            @Override
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                rwLock.readLock().lock();
                try {
                    List<Integer> sizes = new ArrayList<Integer>(fileViews.size());
                    for (FileView friendFileList : fileViews.values()) {
                        sizes.add(friendFileList.size());
                    }
                    data.put("sizes", sizes);
                } finally {
                    rwLock.readLock().unlock();
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

}
