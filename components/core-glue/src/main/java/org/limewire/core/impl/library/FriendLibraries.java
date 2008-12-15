package org.limewire.core.impl.library;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.collection.CharSequenceKeyAnalyzer;
import org.limewire.collection.PatriciaTrie;
import org.limewire.collection.glazedlists.AbstractListEventListener;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import ca.odell.glazedlists.EventList;
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
        private final LockableStringTrie<Collection<RemoteFileItem>> fileIndex = new LockableStringTrie<Collection<RemoteFileItem>>();
        private final LockableStringTrie<Collection<RemoteFileItem>> metaDataIndex = new LockableStringTrie<Collection<RemoteFileItem>>();
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
                            protected void itemAdded(PresenceLibrary item, int idx, EventList source) {
                                Library library = new Library();
                                LOG.debugf("adding library for presence {0} to index", item.getPresence().getPresenceId());
                                libraries.put(item.getPresence().getPresenceId(), library);
                                LibraryListener listener = new LibraryListener(item.getPresence().getPresenceId(), library);
                                listeners.put(item.getPresence().getPresenceId(), listener);
                                item.getModel().addListEventListener(listener);
                            }
                            @Override
                            protected void itemRemoved(PresenceLibrary item, int idx, EventList source) {
                                LOG.debugf("removing library for presence {0} from index", item.getPresence().getPresenceId());
                                libraries.remove(item.getPresence().getPresenceId());
                                LibraryListener listener = listeners.remove(item.getPresence().getPresenceId());
                                item.getModel().removeListEventListener(listener);
                            }
                            @Override
                            protected void itemUpdated(PresenceLibrary item, PresenceLibrary priorItem, int idx, EventList source) {
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
                    index(newFile);
                }
            }
        }

        private void index(RemoteFileItem newFile) {
            String fileName = newFile.getName();
            indexSentence(library.fileIndex, newFile, fileName);
            
            for(FilePropertyKey filePropertyKey : FilePropertyKey.getIndexableKeys()) {
                Object property = newFile.getProperty(filePropertyKey);
                if(property != null) {
                    String sentence = property.toString();
                    indexSentence(library.metaDataIndex, newFile, sentence);
                }
            }
        }

        private void indexSentence(LockableStringTrie<Collection<RemoteFileItem>> index, RemoteFileItem newFile, String sentence) {
            LOG.debugf("adding file {0} for {1}, indexing under:", newFile.getName(), presenceId);
            addToIndex(index, newFile, sentence);
            StringTokenizer st = new StringTokenizer(sentence);
            if(st.countTokens() > 1) {
                while (st.hasMoreElements()) {
                    String word = st.nextToken();
                    addToIndex(index, newFile, word);
                }
            }
        }

        private void addToIndex(LockableStringTrie<Collection<RemoteFileItem>> index, RemoteFileItem newFile, String word) {
            LOG.debugf("\t {0}", word);
            Collection<RemoteFileItem> filesForWord;            
            library.writeLock();
            try {
                filesForWord = index.get(word);
                if (filesForWord == null) {
                    filesForWord = new ArrayList<RemoteFileItem>();
                    index.put(word, filesForWord);
                }
                filesForWord.add(newFile);
            } finally {
                library.writeUnlock();
            }
        }
    }

    public Collection<String> getSuggestions(String prefix, SearchCategory category) {
        Set<String> contained = new HashSet<String>();
        for(Library library : libraries.values()) {
            library.readLock();
            try {
                insertMatchingKeysInto(library.fileIndex.getPrefixedBy(prefix), category, contained);
                insertMatchingKeysInto(library.metaDataIndex.getPrefixedBy(prefix), category, contained);
            } finally {
                library.readUnlock();
            }           
        }
        return contained;
    }

    private void insertMatchingKeysInto(Map<String, Collection<RemoteFileItem>> prefixedBy, SearchCategory category, Collection<String> results) {
        if(category == SearchCategory.ALL) {
            results.addAll(prefixedBy.keySet());
        } else {
            for(Map.Entry<String, Collection<RemoteFileItem>> item : prefixedBy.entrySet()) {
                if(containsCategory(category, item.getValue())) {
                    results.add(item.getKey());
                }
            }
        }
    }
    
    private boolean containsCategory(SearchCategory category, Collection<RemoteFileItem> remoteFileItems) {
        for(RemoteFileItem item : remoteFileItems) {
            if(category == SearchCategory.forCategory(item.getCategory())) {
                return true;
            }
        }
        return false;
    }

    public Collection<RemoteFileItem> getMatchingItems(String prefix, SearchCategory category, Collection<RemoteFileItem> storage) {
        if(storage == null) {
            storage = new HashSet<RemoteFileItem>();
        }
        for(Library library : libraries.values()) {
            library.readLock();
            try {
                insertMatchingItemsInto(library.fileIndex.getPrefixedBy(prefix).values(), category, storage);
                insertMatchingItemsInto(library.metaDataIndex.getPrefixedBy(prefix).values(), category, storage);
            } finally {
                library.readUnlock();
            }           
        }
        return storage;
    }

    private void insertMatchingItemsInto(Collection<Collection<RemoteFileItem>> prefixedBy, SearchCategory category, Collection<RemoteFileItem> storage) {
        for(Collection<RemoteFileItem> remoteFileItems : prefixedBy) {
            if(category == SearchCategory.ALL) {
                storage.addAll(remoteFileItems);
            } else {
                for(RemoteFileItem file : remoteFileItems) {
                    if(category == SearchCategory.forCategory(file.getCategory())) {
                        storage.add(file);    
                    }
                }
            }
        }
    }

    private static class LockableStringTrie<V> extends PatriciaTrie<String, V> {
        private final ReadWriteLock lock;
        
        public LockableStringTrie() {
            super(new CharSequenceKeyAnalyzer());
            lock = new ReentrantReadWriteLock();
        }
        
        ReadWriteLock getLock() {
            return lock;
        }
        
        private String canonicalize(final String s) {
            return s.toUpperCase(Locale.US).toLowerCase(Locale.US);
        }

        @Override
        public boolean containsKey(Object k) {
            return super.containsKey(canonicalize((String)k));
        }

        @Override
        public V get(Object k) {
            return super.get(canonicalize((String)k));
        }

        @Override
        public SortedMap<String, V> getPrefixedBy(String key, int offset, int length) {
            return super.getPrefixedBy(canonicalize(key), offset, length);
        }

        @Override
        public SortedMap<String, V> getPrefixedBy(String key, int length) {
            return super.getPrefixedBy(canonicalize(key), length);
        }

        @Override
        public SortedMap<String, V> getPrefixedBy(String key) {
            return super.getPrefixedBy(canonicalize(key));
        }

        @Override
        public SortedMap<String, V> getPrefixedByBits(String key, int bitLength) {
            return super.getPrefixedByBits(canonicalize(key), bitLength);
        }

        @Override
        public SortedMap<String, V> headMap(String toKey) {
            return super.headMap(canonicalize(toKey));
        }

        @Override
        public V put(String key, V value) {
            return super.put(canonicalize(key), value);
        }

        @Override
        public V remove(Object k) {
            return super.remove(canonicalize((String)k));
        }

        @Override
        public Entry<String, V> select(String key, Cursor<? super String, ? super V> cursor) {
            return super.select(canonicalize(key), cursor);
        }

        @Override
        public V select(String key) {
            return super.select(canonicalize(key));
        }

        @Override
        public SortedMap<String, V> subMap(String fromKey, String toKey) {
            return super.subMap(canonicalize(fromKey), canonicalize(toKey));
        }

        @Override
        public SortedMap<String, V> tailMap(String fromKey) {
            return super.tailMap(canonicalize(fromKey));
        }
    }
}

