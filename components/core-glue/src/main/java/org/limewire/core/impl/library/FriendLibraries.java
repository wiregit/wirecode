package org.limewire.core.impl.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.collection.MultiIterator;
import org.limewire.collection.StringTrie;
import org.limewire.collection.glazedlists.AbstractListEventListener;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FriendLibraries {
    private final Map<String, Library> libraries;
    private static final Log LOG = LogFactory.getLog(FriendLibraries.class);

    FriendLibraries() {
        this.libraries = new ConcurrentHashMap<String, Library>();
    }
    
    private class Library {
        private final LockableStringTrie<List<RemoteFileItem>> fileIndex = new LockableStringTrie<List<RemoteFileItem>>(true);
        private final LockableStringTrie<List<RemoteFileItem>> metaDataIndex = new LockableStringTrie<List<RemoteFileItem>>(true);
        public void readLock() {
            fileIndex.getLock().readLock().lock();
            metaDataIndex.getLock().readLock().lock();
        }
        public void readUnlock() {
            fileIndex.getLock().readLock().unlock();
            metaDataIndex.getLock().readLock().unlock();
        }
        public void writeLock() {
            fileIndex.getLock().writeLock().lock();
            metaDataIndex.getLock().writeLock().lock();
        }
        public void writeUnlock() {
            fileIndex.getLock().writeLock().unlock();
            metaDataIndex.getLock().writeLock().unlock();
        }
    }
    @Inject
    void register(RemoteLibraryManager remoteLibraryManager) {
        remoteLibraryManager.getFriendLibraryList().addListEventListener(new ListEventListener<FriendLibrary>() {
            @Override
            public void listChanged(ListEvent<FriendLibrary> listChanges) {
                while(listChanges.next()) {
                    int type = listChanges.getType();
                    if(type == ListEvent.INSERT) {
                        FriendLibrary friendLibrary = listChanges.getSourceList().get(listChanges.getIndex());
                        new AbstractListEventListener<PresenceLibrary>() {
                            private final Map<String, LibraryListener> listeners = new HashMap<String, LibraryListener>();
                            
                            @Override
                            protected void itemAdded(PresenceLibrary item) {
                                Library library = new Library();
                                LOG.debugf("adding library for presence {0} to index", item.getPresence().getPresenceId());
                                libraries.put(item.getPresence().getPresenceId(), library);
                                LibraryListener listener = new LibraryListener(item.getPresence().getPresenceId(), library);
                                listeners.put(item.getPresence().getPresenceId(), listener);
                                item.getModel().addListEventListener(listener);
                            }
                            @Override
                            protected void itemRemoved(PresenceLibrary item) {
                                LOG.debugf("removing library for presence {0} from index", item.getPresence());
                                libraries.remove(item.getPresence().getPresenceId());
                                LibraryListener listener = listeners.remove(item.getPresence().getPresenceId());
                                item.getModel().removeListEventListener(listener);
                            }
                            @Override
                            protected void itemUpdated(PresenceLibrary item) {
                            }                            
                        }.install(friendLibrary.getPresenceLibraryList());
                    }
                }                
            }
        });
    }

    private class LibraryListener implements ListEventListener<RemoteFileItem> {

        private final String presenceId;
        private final Library library;

        LibraryListener(String presenceId, Library library) {
            this.presenceId = presenceId;
            this.library = library;
        }

        public void listChanged(ListEvent<RemoteFileItem> listChanges) {
            while(listChanges.next()) {
                if(listChanges.getType() == ListEvent.INSERT) {
                    RemoteFileItem newFile = listChanges.getSourceList().get(listChanges.getIndex());
                    LOG.debugf("adding file {0} for {1}, indexing under:", newFile.getName(), presenceId);
                    addToIndex(newFile, newFile.getName());
                    StringTokenizer st = new StringTokenizer(newFile.getName());
                    if(st.countTokens() > 1) {
                        while (st.hasMoreElements()) {
                            String word = st.nextToken();
                            addToIndex(newFile, word);
                        }
                    }
                }
            }
        }

        private void addToIndex(RemoteFileItem newFile, String word) {
            LOG.debugf("\t {0}", word);
            List<RemoteFileItem> filesForWord;            
            library.writeLock();
            try {
                filesForWord = library.fileIndex.get(word);
                if (filesForWord == null) {
                    filesForWord = new ArrayList<RemoteFileItem>();
                    library.fileIndex.add(word, filesForWord);
                }
                filesForWord.add(newFile);
            } finally {
                library.writeUnlock();
            }
        }
    }

    public Iterator<RemoteFileItem> iterator(SearchCategory category) {
        List<Iterator<RemoteFileItem>> iterators = new ArrayList<Iterator<RemoteFileItem>>();
        for(Library library : libraries.values()) {
            Iterator<List<RemoteFileItem>> libraryResultsCopy;
            library.readLock();
            try {
                libraryResultsCopy = filterAndCopy(library.fileIndex.getIterator(), category);
            } finally {
                library.readUnlock();
            }
            iterators.add(iterator(libraryResultsCopy));
        }
        return new MultiIterator<RemoteFileItem>(iterators);
    }

    private Iterator<RemoteFileItem> iterator(final Iterator<List<RemoteFileItem>> iterator) {
        List<Iterator<RemoteFileItem>> iterators = new ArrayList<Iterator<RemoteFileItem>>();        
        while(iterator.hasNext()) {
            iterators.add(iterator.next().iterator());
        }
        return new MultiIterator<RemoteFileItem>(iterators);
    }

    public Iterator<RemoteFileItem> iterator(String s, SearchCategory category, boolean includeMetaDataMatches) {
        List<Iterator<RemoteFileItem>> iterators = new ArrayList<Iterator<RemoteFileItem>>();
        for(Library library : libraries.values()) {
            Iterator<List<RemoteFileItem>> libraryResultsCopy;
            library.readLock();
            try {
                libraryResultsCopy = filterAndCopy(library.fileIndex.getPrefixedBy(s), category);
            } finally {
                library.readUnlock();
            }
            iterators.add(iterator(libraryResultsCopy));
        }
        return new MultiIterator<RemoteFileItem>(iterators);
    }

    private Iterator<List<RemoteFileItem>> filterAndCopy(Iterator<List<RemoteFileItem>> prefixedBy, SearchCategory category) {
        List<List<RemoteFileItem>> copy = new ArrayList<List<RemoteFileItem>>();
        while(prefixedBy.hasNext()) {
            copy.add(filter(prefixedBy.next(), category));
        }
        return copy.iterator();
    }

    private List<RemoteFileItem> filter(List<RemoteFileItem> remoteFileItems, SearchCategory category) {
        if(category == SearchCategory.ALL) {
            return remoteFileItems;    
        } else {
            List<RemoteFileItem> filtered = new ArrayList<RemoteFileItem>();
            for(RemoteFileItem file : remoteFileItems) {
                if(category == SearchCategory.forCategory(file.getCategory())) {
                    filtered.add(file);    
                }
            }
            return filtered;
        }
    }

    private class LockableStringTrie<V> extends StringTrie<V> {

        final ReadWriteLock lock;
        
        public LockableStringTrie(boolean ignoreCase) {
            super(ignoreCase);
            lock = new ReentrantReadWriteLock();
        }
        
        ReadWriteLock getLock() {
            return lock;
        }
    }
}

