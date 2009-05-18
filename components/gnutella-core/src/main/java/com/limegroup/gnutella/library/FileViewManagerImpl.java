package com.limegroup.gnutella.library;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.collection.CollectionUtils;
import org.limewire.collection.Comparators;
import org.limewire.collection.IntSet;
import org.limewire.collection.IntSet.IntSetIterator;
import org.limewire.core.settings.MessageSettings;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionPoint;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.listener.SourcedEventMulticasterImpl;
import org.limewire.statistic.StatsUtils;
import org.limewire.util.RPNParser;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileViewChangeEvent.Type;
import com.limegroup.gnutella.routing.HashFunction;
import com.limegroup.gnutella.routing.QueryRouteTable;

/**
 * The default implementation of {@link FileViewManager}.
 * 
 * This uses {@link MultiFileView}s to represent a {@link FileView} backed by
 * multiple other views. Any mutable operation on the MultiFileView is performed
 * by this class, and this class is responsible for properly locking all mutable
 * operations.
 */
@Singleton
class FileViewManagerImpl implements FileViewManager {
    
    private final LibraryImpl library;
    private final GnutellaFileCollectionImpl gnutellaView;
    private final IncompleteFileCollectionImpl incompleteView;
    
    /** Lock held to mutate any structure in this class or to mutate a MultiFileView. */
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    /** 
     * The share id mapped to the MultiFileView of all FileDescs visible by that id.
     * This is lazily built as people request views for a given id, which is why
     * scattered throughout this class places check for fileViewsPerFriend(id) != null.
     *  */
    private final Map<String, MultiFileView> fileViewsPerFriend = new HashMap<String, MultiFileView>();

    /** Any collection this is mapping to a MultiFileView. */
    private final Collection<SharedFileCollection> sharedCollections = new ArrayList<SharedFileCollection>();
    
    /** Multicaster used to broadcast events for a MultiFileView. */
    private final SourcedEventMulticaster<FileViewChangeEvent, FileView> multicaster =
        new SourcedEventMulticasterImpl<FileViewChangeEvent, FileView>();

    @Inject
    public FileViewManagerImpl(LibraryImpl library, GnutellaFileCollectionImpl gnutellaCollection,
            IncompleteFileCollectionImpl incompleteCollection) {
        this.library = library;
        this.gnutellaView = gnutellaCollection;
        this.incompleteView = incompleteCollection;
    }
    
    @Inject void register(ListenerSupport<FileViewChangeEvent> viewListeners,
                          ListenerSupport<SharedFileCollectionChangeEvent> collectionListeners) {
        viewListeners.addListener(new EventListener<FileViewChangeEvent>() {
            @Override
            @BlockingEvent(queueName="FileViewManager")
            public void handleEvent(FileViewChangeEvent event) {
                switch(event.getType()) {
                case FILE_ADDED:
                    fileAddedToCollection(event.getFileDesc(), (SharedFileCollection)event.getFileView());
                    break;
                case FILE_REMOVED:
                    fileRemovedFromCollection(event.getFileDesc(), (SharedFileCollection)event.getFileView());
                    break;
                case FILES_CLEARED:
                    collectionCleared((SharedFileCollection)event.getFileView());
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
                case FRIEND_ADDED:
                    friendAddedToCollection(event.getSource(), event.getFriendId());
                    break;
                case FRIEND_REMOVED:
                    friendRemovedFromCollection(event.getSource(), event.getFriendId());
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
    
    /**
     * Notification that a new collection was created. This looks at all the
     * share ids the collection is shared with any adds itself as a backing view
     * to any {@link MultiFileView}s that are mapped by that id.
     * 
     * An event will be triggered for each {@link FileDesc} that was added to
     * each {@link MultiFileView}.
     */
    private void collectionAdded(SharedFileCollection collection) {
        Map<FileView, List<FileDesc>> addedFiles = null;
        rwLock.writeLock().lock();
        try {
            sharedCollections.add(collection);
            for(String id : collection.getFriendList()) {
                MultiFileView view = fileViewsPerFriend.get(id);
                if(view != null) {
                    List<FileDesc> added = view.addNewBackingView(collection);
                    if(!added.isEmpty()) {
                        if(addedFiles == null) {
                            addedFiles = new HashMap<FileView, List<FileDesc>>();
                        }
                        addedFiles.put(view, added);
                    }
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }

        if(addedFiles != null) {
            for(Map.Entry<FileView, List<FileDesc>> entry : addedFiles.entrySet()) {
                for(FileDesc fd : entry.getValue()) {
                    multicaster.broadcast(new FileViewChangeEvent(entry.getKey(), Type.FILE_ADDED, fd));
                }
            }
        }
    }
    
    /**
     * Notification that a collection was removed. This will remove the
     * collection as a backing view for any {@link MultiFileView}s that were
     * mapped to by any share ids the collection is shared with.
     * 
     * An event will be sent for each {@link FileDesc} that was removed from
     * each {@link MultiFileView}.
     */
    private void collectionRemoved(SharedFileCollection collection) {
        Map<FileView, List<FileDesc>> removedFiles = null;
        
        rwLock.writeLock().lock();
        try {
            sharedCollections.remove(collection);
            for(MultiFileView view : fileViewsPerFriend.values()) {
                List<FileDesc> removed = view.removeBackingView(collection);
                if(!removed.isEmpty()) {
                    if(removedFiles == null) {
                        removedFiles = new HashMap<FileView, List<FileDesc>>();
                    }
                    removedFiles.put(view, removed);
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }   
        
        if(removedFiles != null) {
            for(Map.Entry<FileView, List<FileDesc>> entry : removedFiles.entrySet()) {
                for(FileDesc fd : entry.getValue()) {
                    multicaster.broadcast(new FileViewChangeEvent(entry.getKey(), Type.FILE_REMOVED, fd));
                }
            }
        }
    }
    
    /**
     * Notification that a collection is shared with another person.
     * 
     * This will add the collection as a backing view for the
     * {@link MultiFileView} that exists for this id, if any view exists.
     * 
     * An event will be sent for each {@link FileDesc} that was added to the
     * {@link MultiFileView}.
     */
    private void friendAddedToCollection(SharedFileCollection collection, String id) {
        MultiFileView view = null;
        List<FileDesc> added = null;
        rwLock.writeLock().lock();
        try {
            view = fileViewsPerFriend.get(id);
            if(view != null) {
                added = view.addNewBackingView(collection);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(view != null && added != null) {
            for(FileDesc fd : added) {
                multicaster.broadcast(new FileViewChangeEvent(view, Type.FILE_ADDED, fd));
            }
        }
    }
    
    /**
     * Notification that a collection is no longer shared with a person.
     * 
     * This will remove the collection as a backing view from the
     * {@link MultiFileView} for that id, if any view exists.
     * 
     * An event will be sent for each {@link FileDesc} that was removed from the
     * {@link MultiFileView}.
     */
    private void friendRemovedFromCollection(SharedFileCollection collection, String id) {
        MultiFileView view = null;
        List<FileDesc> removed = null;
        rwLock.writeLock().lock();
        try {
            view = fileViewsPerFriend.get(id);
            if(view != null) {
                removed = view.removeBackingView(collection);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(view != null && removed != null) {
            for(FileDesc fd : removed) {
                multicaster.broadcast(new FileViewChangeEvent(view, Type.FILE_REMOVED, fd));
            }
        }
    }
    
    /**
     * Notification that a particular collection was cleared.
     * @param collection
     */
    private void collectionCleared(SharedFileCollection collection) {
        Map<FileView, List<FileDesc>> removedFiles = null;
        
        rwLock.writeLock().lock();
        try {
            for(String id : collection.getFriendList()) {
                MultiFileView view = fileViewsPerFriend.get(id);
                if(view != null) {
                    List<FileDesc> removed = view.fileViewCleared(collection);
                    if(!removed.isEmpty()) {
                        if(removedFiles == null) {
                            removedFiles = new HashMap<FileView, List<FileDesc>>();
                        }
                        removedFiles.put(view, removed);
                    }
                }
                
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(removedFiles != null) {
            for(Map.Entry<FileView, List<FileDesc>> entry : removedFiles.entrySet()) {
                for(FileDesc fd : entry.getValue()) {
                    multicaster.broadcast(new FileViewChangeEvent(entry.getKey(), Type.FILE_REMOVED, fd));
                }
            }
        }
    }

    private void fileRemovedFromCollection(FileDesc fileDesc, SharedFileCollection collection) {
        List<FileView> removedViews = null;
        
        rwLock.writeLock().lock();
        try {
            for(String id : collection.getFriendList()) {
                MultiFileView view = fileViewsPerFriend.get(id);
                if(view != null) {
                    if(view.fileRemovedFromView(fileDesc, collection)) {
                        if(removedViews == null) {
                            removedViews = new ArrayList<FileView>();
                        }
                        removedViews.add(view);
                    }
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(removedViews != null) {
            for(FileView view : removedViews) {
                multicaster.broadcast(new FileViewChangeEvent(view, Type.FILE_REMOVED, fileDesc));
            }
        }
    }

    private void fileAddedToCollection(FileDesc fileDesc, SharedFileCollection collection) {
        List<FileView> addedViews = null;
        
        rwLock.writeLock().lock();
        try {
            for(String id : collection.getFriendList()) {
                MultiFileView view = fileViewsPerFriend.get(id);
                if(view != null) {
                    if(view.fileAddedFromView(fileDesc, collection)) {
                        if(addedViews == null) {
                            addedViews = new ArrayList<FileView>();
                        }
                        addedViews.add(view);
                    }
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(addedViews != null) {
            for(FileView view : addedViews) {
                multicaster.broadcast(new FileViewChangeEvent(view, Type.FILE_ADDED, fileDesc));
            }
        }
    }
    
    @Override
    public FileView getFileViewForId(String id) {
        MultiFileView view;
        rwLock.readLock().lock();
        try {
            view = fileViewsPerFriend.get(id);
        } finally {
            rwLock.readLock().unlock();
        }
        
        if(view == null) {        
            rwLock.writeLock().lock();
            try {
                // recheck within lock -- it may have been created
                view = fileViewsPerFriend.get(id);
                if(view == null) {
                    view = createFileView(id);
                    fileViewsPerFriend.put(id, view);
                }
            } finally {
                rwLock.writeLock().unlock();   
            }
        }
        
        return view;
    }
    
    private MultiFileView createFileView(String id) {
        MultiFileView view = new MultiFileView();
        initialize(view, id);
        return view;
    }
    
    private void initialize(MultiFileView view, String id) {
        for(SharedFileCollection collection : sharedCollections) {
            if(collection.getFriendList().contains(id)) {
                view.addNewBackingView(collection);
            }
        }
    }

    /**
     * An implementation of {@link FileView} that is backed by other FileViews.
     * This implementation is intended to work in concert with {@link FileViewManagerImpl}
     * and is used to return the view of files that any single person in a 
     * {@link SharedFileCollection#getFriendList()}.  Many different collections
     * can be shared with a single id.  This {@link MultiFileView} represents a view
     * of everything that is shared with that id.
     */
    private class MultiFileView extends AbstractFileView {
        
        /*
         * A note about locking:
         *  All write locking is performed by FileViewManagerImpl.
         *  This works because there are no public methods in this class
         *  that are mutable.
         */
        
        /** All views this is backed off of. */
        private final List<FileView> backingViews = new ArrayList<FileView>();

        MultiFileView() {
            super(FileViewManagerImpl.this.library);
        }

        @Override
        public void addListener(EventListener<FileViewChangeEvent> listener) {
            multicaster.addListener(this, listener);
        }

        @Override
        public Lock getReadLock() {
            return rwLock.readLock();
        }

        @Override
        public Iterator<FileDesc> iterator() {
            rwLock.readLock().lock();
            try {
                return new FileViewIterator(library, new IntSet(getInternalIndexes()));
            } finally {
                rwLock.readLock().unlock();
            }
        }

        @Override
        public Iterable<FileDesc> pausableIterable() {
            return new Iterable<FileDesc>() {
                @Override
                public Iterator<FileDesc> iterator() {
                    return MultiFileView.this.iterator();
                }
            };
        }

        @Override
        public boolean removeListener(EventListener<FileViewChangeEvent> listener) {
            return multicaster.removeListener(this, listener);
        }
        
        /**
         * Removes a backing {@link FileView}. Not every FileDesc in the backing
         * view will necessarily be removed. This is because the FileDesc may exist
         * in another view that this is backed by.
         * 
         * @return A list of {@link FileDesc}s that were removed from this view.
         */
        List<FileDesc> removeBackingView(FileView view) {
            if (backingViews.remove(view)) {
                return validateItems();
            } else {
                return Collections.emptyList();
            }
        }

        /**
         * Adds a new backing {@link FileView}. Not every FileDesc in the backing
         * view will necessarily be added. This is because some FileDescs may
         * already exist due to other backing views.
         * 
         * @return A list of {@link FileDesc}s that were added.
         */
        List<FileDesc> addNewBackingView(FileView view) {
            if(!backingViews.contains(view)) {
                backingViews.add(view);
            }
            
            // lock the backing view in order to iterate through its
            // indexes.
            List<FileDesc> added = new ArrayList<FileDesc>(view.size());
            view.getReadLock().lock();
            try {
                IntSetIterator iter = ((AbstractFileView)view).getInternalIndexes().iterator();
                while(iter.hasNext()) {
                    int i = iter.next();
                    if(getInternalIndexes().add(i)) {
                        added.add(library.getFileDescForIndex(i));
                    }
                }
            } finally {
                view.getReadLock().unlock();
            }
            return added;
        }

        /**
         * Notification that a backing {@link FileView} has been cleared.
         * 
         * @return A list of {@link FileDesc}s that were removed from this view due
         *         to being removed from the backing view.
         */
        List<FileDesc> fileViewCleared(FileView fileView) {
            return validateItems();
        }

        /**
         * Notification that a {@link FileDesc} was removed from a backing {@link FileView}.
         * 
         * @return true if the file used to exist in this view (and is now removed).
         *         false if it did not exist in this view.
         */
        boolean fileRemovedFromView(FileDesc fileDesc, FileView fileView) {
            for(FileView view : backingViews) {
                if(view.contains(fileDesc)) {
                    return false;
                }
            }
            getInternalIndexes().remove(fileDesc.getIndex());
            return true;
        }

        /**
         * Notification that a {@link FileDesc} was adding to a backing
         * {@link FileView}.
         * 
         * @return true if the {@link FileDesc} was succesfully added to this view.
         *         false if the FileDesc already existed in the view.
         */
        boolean fileAddedFromView(FileDesc fileDesc, FileView fileView) {
            return getInternalIndexes().add(fileDesc.getIndex());
        }
        
        /**
         * Calculates what should be in this list based on the current
         * views in {@link #backingViews}.  This will return a list
         * of {@link FileDesc} that is every removed item.
         * This method does not expect items to be added that were
         * not already contained.
         */
        private List<FileDesc> validateItems() {
            // Calculate the current FDs in the set.
            IntSet newItems = new IntSet();
            for(FileView view : backingViews) {
                view.getReadLock().lock();
                try {
                    newItems.addAll(((AbstractFileView)view).getInternalIndexes());                
                } finally {
                    view.getReadLock().unlock();
                }
            }
            
            // Calculate the FDs that were removed.
            List<FileDesc> removedFds;
            IntSet indexes = getInternalIndexes();
            indexes.removeAll(newItems);
            if(indexes.size() == 0) {
                removedFds = Collections.emptyList();
            } else {
                removedFds = new ArrayList<FileDesc>(indexes.size());
                IntSetIterator iter = indexes.iterator();
                while(iter.hasNext()) {
                    FileDesc fd = library.getFileDescForIndex(iter.next());
                    if(fd != null) {
                        removedFds.add(fd);
                    }                
                }
            }
            
            // Set the current FDs & return the removed ones.
            indexes.clear();
            indexes.addAll(newItems);
            return removedFds;
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
                    List<Integer> sizes = new ArrayList<Integer>(fileViewsPerFriend.size());
                    for (FileView friendFileList : fileViewsPerFriend.values()) {
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
