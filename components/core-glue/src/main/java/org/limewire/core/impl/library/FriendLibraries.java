package org.limewire.core.impl.library;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.collection.MultiIterator;
import org.limewire.collection.StringTrie;
import org.limewire.collection.glazedlists.AbstractListEventListener;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

@Singleton
public class FriendLibraries {
    private final ConcurrentHashMap<String, LockableStringTrie<ConcurrentLinkedQueue<RemoteFileItem>>> libraries;
    private static final Log LOG = LogFactory.getLog(FriendLibraries.class);

    FriendLibraries() {
        this.libraries = new ConcurrentHashMap<String, LockableStringTrie<ConcurrentLinkedQueue<RemoteFileItem>>>();
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
                            @Override
                            protected void itemAdded(PresenceLibrary item) {
                                LockableStringTrie<ConcurrentLinkedQueue<RemoteFileItem>> trie = new LockableStringTrie<ConcurrentLinkedQueue<RemoteFileItem>>(true);
                                if(libraries.putIfAbsent(item.getPresence().getPresenceId(), trie) == null) {
                                    LOG.debugf("adding library for presence {0} to index", item.getPresence().getPresenceId());
                                    item.getModel().addListEventListener(new LibraryListener(trie));
                                }
                            }
                            @Override
                            protected void itemRemoved(PresenceLibrary item) {
                                LOG.debugf("removing library for presence {0} from index", item.getPresence());
                                libraries.remove(item.getPresence().getPresenceId());
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

    class LibraryListener implements ListEventListener<RemoteFileItem> {

        final LockableStringTrie<ConcurrentLinkedQueue<RemoteFileItem>> library;

        LibraryListener(LockableStringTrie<ConcurrentLinkedQueue<RemoteFileItem>> library) {
            this.library = library;
        }

        public void listChanged(ListEvent<RemoteFileItem> listChanges) {
            while(listChanges.next()) {
                if(listChanges.getType() == ListEvent.INSERT) {
                    RemoteFileItem newFile = listChanges.getSourceList().get(listChanges.getIndex());
                    LOG.debugf("adding file {0}, indexing under:", newFile.getName());
                    addToIndex(newFile, newFile.getName());
                    StringTokenizer st = new StringTokenizer(newFile.getName());
                    while (st.hasMoreElements()) {
                        String word = st.nextToken();
                        addToIndex(newFile, word);
                    }
                }
            }
        }

        private void removeFromIndex(RemoteFileItem newFile, String word) {
            ConcurrentLinkedQueue<RemoteFileItem> filesForWord;
            library.getLock().writeLock().lock();
            try {
                filesForWord = library.get(word);
                if (filesForWord == null) {
                    filesForWord = new ConcurrentLinkedQueue<RemoteFileItem>();
                    library.add(word, filesForWord);
                }
            } finally {
                library.getLock().writeLock().unlock();
            }
            filesForWord.remove(newFile);
        }

        private void addToIndex(RemoteFileItem newFile, String word) {
            LOG.debugf("\t {0}", word);
            ConcurrentLinkedQueue<RemoteFileItem> filesForWord;
            library.getLock().writeLock().lock();
            try {
                filesForWord = library.get(word);
                if (filesForWord == null) {
                    filesForWord = new ConcurrentLinkedQueue<RemoteFileItem>();
                    library.add(word, filesForWord);
                }
            } finally {
                library.getLock().writeLock().unlock();
            }
            filesForWord.add(newFile);
        }
    }

    @SuppressWarnings("unchecked")
    public Iterator<RemoteFileItem> iterator() {
        List<Iterator<RemoteFileItem>> iterators = new ArrayList<Iterator<RemoteFileItem>>();
        for(LockableStringTrie<ConcurrentLinkedQueue<RemoteFileItem>> library : libraries.values()) {
            library.getLock().readLock().lock();
            Iterator<ConcurrentLinkedQueue<RemoteFileItem>> libraryResultsCopy = copy(library.getIterator());
            library.getLock().readLock().unlock();
            iterators.add(iterator(libraryResultsCopy));      
        }
        return new MultiIterator<RemoteFileItem>(iterators.toArray(new Iterator[iterators.size()]));
    }

    @SuppressWarnings("unchecked")
    private Iterator<RemoteFileItem> iterator(final Iterator<ConcurrentLinkedQueue<RemoteFileItem>> iterator) {
        List<Iterator<RemoteFileItem>> iterators = new ArrayList<Iterator<RemoteFileItem>>();        
        while(iterator.hasNext()) {
            iterators.add(iterator.next().iterator());
        }
        return new MultiIterator<RemoteFileItem>(iterators.toArray(new Iterator[iterators.size()]));
    }

    @SuppressWarnings("unchecked")
    public Iterator<RemoteFileItem> iterator(String s) {
        List<Iterator<RemoteFileItem>> iterators = new ArrayList<Iterator<RemoteFileItem>>();
        for(LockableStringTrie<ConcurrentLinkedQueue<RemoteFileItem>> library : libraries.values()) {
            library.getLock().readLock().lock();
            Iterator<ConcurrentLinkedQueue<RemoteFileItem>> libraryResultsCopy = copy(library.getPrefixedBy(s));
            library.getLock().readLock().unlock();
            iterators.add(iterator(libraryResultsCopy));            
        }
        return new MultiIterator<RemoteFileItem>(iterators.toArray(new Iterator[iterators.size()]));
    }

    private Iterator<ConcurrentLinkedQueue<RemoteFileItem>> copy(Iterator<ConcurrentLinkedQueue<RemoteFileItem>> prefixedBy) {
        List<ConcurrentLinkedQueue<RemoteFileItem>> copy = new ArrayList<ConcurrentLinkedQueue<RemoteFileItem>>();
        while(prefixedBy.hasNext()) {
            copy.add(prefixedBy.next());
        }
        return copy.iterator();
    }

    class LockableStringTrie<V> extends StringTrie<V> {

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

