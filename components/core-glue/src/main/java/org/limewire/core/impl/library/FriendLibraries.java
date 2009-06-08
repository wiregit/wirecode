package org.limewire.core.impl.library;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FriendLibraries {
    private static final Log LOG = LogFactory.getLog(FriendLibraries.class);

    private final Map<String, Library> libraries;

    FriendLibraries() {
        this.libraries = new ConcurrentHashMap<String, Library>();
    }

    private static class Library {
        private final String presenceId;

        private final RemoteFileItemStringTrie suggestionsIndex;

        private final RemoteFileItemStringTrie fileNameIndex;

        private final Map<FilePropertyKey, RemoteFileItemStringTrie> propertiesIndexes;

        private final Map<FilePropertyKey, RemoteFileItemStringTrie> suggestionPropertiesIndexes;

        public Library(String presenceId) {
            this.presenceId = presenceId;
            suggestionsIndex = new RemoteFileItemStringTrie();
            fileNameIndex = new RemoteFileItemStringTrie();
            propertiesIndexes = new ConcurrentHashMap<FilePropertyKey, RemoteFileItemStringTrie>();
            suggestionPropertiesIndexes = new ConcurrentHashMap<FilePropertyKey, RemoteFileItemStringTrie>();
        }
        
        public void clear() {
            suggestionsIndex.lock.writeLock().lock();
            try {
                suggestionsIndex.clear();
            } finally {
                suggestionsIndex.lock.writeLock().unlock();
            }
            fileNameIndex.lock.writeLock().lock();
            try {
                fileNameIndex.clear();
            } finally {
                fileNameIndex.lock.writeLock().unlock();
            }

            synchronized(propertiesIndexes) {
                propertiesIndexes.clear();
            }
            synchronized(suggestionPropertiesIndexes) {
                suggestionPropertiesIndexes.clear();
            }
            
        }

        public RemoteFileItemStringTrie getOrCreateFilePropertyIndex(FilePropertyKey filePropertyKey) {
            RemoteFileItemStringTrie propertiesIndex = propertiesIndexes.get(filePropertyKey);
            if (propertiesIndex == null) {
                synchronized (propertiesIndexes) {
                    propertiesIndex = propertiesIndexes.get(filePropertyKey);
                    if (propertiesIndex == null) {
                        propertiesIndex = new RemoteFileItemStringTrie();
                        propertiesIndexes.put(filePropertyKey, propertiesIndex);
                    }
                }
            }
            return propertiesIndex;
        }

        public RemoteFileItemStringTrie getFilePropertyIndex(FilePropertyKey filePropertyKey) {
            return propertiesIndexes.get(filePropertyKey);
        }

        public RemoteFileItemStringTrie getOrCreateSuggestionPropertyIndex(FilePropertyKey filePropertyKey) {
            RemoteFileItemStringTrie propertiesIndex = suggestionPropertiesIndexes.get(filePropertyKey);
            if (propertiesIndex == null) {
                synchronized (suggestionPropertiesIndexes) {
                    propertiesIndex = suggestionPropertiesIndexes.get(filePropertyKey);
                    if (propertiesIndex == null) {
                        propertiesIndex = new RemoteFileItemStringTrie();
                        suggestionPropertiesIndexes.put(filePropertyKey, propertiesIndex);
                    }
                }
            }
            return propertiesIndex;
        }

        public RemoteFileItemStringTrie getSuggestionPropertyIndex(FilePropertyKey filePropertyKey) {
            return suggestionPropertiesIndexes.get(filePropertyKey);
        }

        public RemoteFileItemStringTrie getFileNameIndex() {
            return fileNameIndex;
        }

        public RemoteFileItemStringTrie getSuggestionsIndex() {
            return suggestionsIndex;
        }

        /**
         * Indexes the file name in both the suggestions and fileName indexes.
         * The suggestions index only indexes the phrase as a whole. While the
         * filename indexes the phrase by breaking it apart into all the words
         * within.
         */
        private void indexFileName(RemoteFileItem newFile) {
            String fileName = newFile.getName();
            if (fileName != null) {
                LOG.debugf("adding file {0} for {1}, indexing under:", newFile.getName(),
                        presenceId);

                getSuggestionsIndex().lock.writeLock().lock();
                try {
                    // indexes the whole file name so suggestions return the
                    // whole
                    // name back
                    getSuggestionsIndex().addWordToIndex(newFile, fileName);
                } finally {
                    getSuggestionsIndex().lock.writeLock().unlock();
                }

                getFileNameIndex().lock.writeLock().lock();
                try {
                    getFileNameIndex().addPhraseToIndex(newFile, fileName);
                } finally {
                    getFileNameIndex().lock.writeLock().unlock();
                }
            }
        }

        /**
         * Indexes properties in both the suggestions and properties indexes.
         * <p>
         * The suggestions index only indexes the phrase as a whole. While the
         * filename indexes the phrase by breaking it apart into all the words
         * within.
         */
        private void indexProperty(RemoteFileItem newFile, FilePropertyKey filePropertyKey,
                String phrase) {

            RemoteFileItemStringTrie filePropertyIndex = getOrCreateFilePropertyIndex(filePropertyKey);

            filePropertyIndex.lock.writeLock().lock();
            try {
                filePropertyIndex.addWordToIndex(newFile, phrase);
                filePropertyIndex.addPhraseToIndex(newFile, phrase);
            } finally {
                filePropertyIndex.lock.writeLock().unlock();
            }

            getSuggestionsIndex().lock.writeLock().lock();
            try {
                // indexes the whole string so suggestions return the whole name back
                getSuggestionsIndex().addWordToIndex(newFile, phrase);
            } finally {
                getSuggestionsIndex().lock.writeLock().unlock();
            }

            RemoteFileItemStringTrie suggestionsFilePropertyIndex = getOrCreateSuggestionPropertyIndex(filePropertyKey);
            suggestionsFilePropertyIndex.lock.writeLock().lock();
            try {
                suggestionsFilePropertyIndex.addWordToIndex(newFile, phrase);
            } finally {
                suggestionsFilePropertyIndex.lock.writeLock().unlock();
            }
        }
    }

    @Inject
    void register(RemoteLibraryManager remoteLibraryManager) {
        remoteLibraryManager.getFriendLibraryList().addListEventListener(
                new ListEventListener<FriendLibrary>() {
                    @Override
                    public void listChanged(ListEvent<FriendLibrary> listChanges) {
                        while (listChanges.next()) {
                            int type = listChanges.getType();
                            if (type == ListEvent.INSERT) {
                                FriendLibrary friendLibrary = listChanges.getSourceList().get(
                                        listChanges.getIndex());
                                new AbstractListEventListener<PresenceLibrary>() {
                                    private final Map<String, LibraryListener> listeners = new HashMap<String, LibraryListener>();

                                    @Override
                                    protected void itemAdded(PresenceLibrary item, int idx,
                                            EventList<PresenceLibrary> source) {
                                        String presenceId = item.getPresence().getPresenceId();
                                        Library library = new Library(presenceId);
                                        LOG.debugf("adding library for presence {0} to index", presenceId);
                                        libraries.put(item.getPresence().getPresenceId(), library);
                                        LibraryListener listener = new LibraryListener(library);
                                        listeners.put(item.getPresence().getPresenceId(), listener);
                                        item.getModel().addListEventListener(listener);
                                    }

                                    @Override
                                    protected void itemRemoved(PresenceLibrary item, int idx,
                                            EventList<PresenceLibrary> source) {
                                        LOG.debugf("removing library for presence {0} from index",
                                                item.getPresence().getPresenceId());
                                        libraries.remove(item.getPresence().getPresenceId());
                                        LibraryListener listener = listeners.remove(item
                                                .getPresence().getPresenceId());
                                        item.getModel().removeListEventListener(listener);
                                    }

                                    @Override
                                    protected void itemUpdated(PresenceLibrary item,
                                            PresenceLibrary priorItem, int idx,
                                            EventList<PresenceLibrary> source) {
                                    }
                                }.install(friendLibrary.getPresenceLibraryList());
                            }
                        }
                    }
                });
    }

    /** Returns all suggestions for search terms based on the given prefix. */
    public Collection<String> getSuggestions(String prefix, SearchCategory category) {
        Set<String> matches = new HashSet<String>();
        for (Library library : libraries.values()) {
            library.getSuggestionsIndex().lock.readLock().lock();
            try {
                insertMatchingKeysInto(library.getSuggestionsIndex().getPrefixedBy(prefix),
                        category, matches);
            } finally {
                library.getSuggestionsIndex().lock.readLock().unlock();
            }
        }
        return matches;
    }

    public Collection<String> getSuggestions(String prefix, SearchCategory category,
            FilePropertyKey filePropertyKey) {
        Set<String> matches = new HashSet<String>();
        for (Library library : libraries.values()) {
            RemoteFileItemStringTrie propertyStringTree = library
                    .getSuggestionPropertyIndex(filePropertyKey);

            if (propertyStringTree != null) {
                propertyStringTree.lock.readLock().lock();
                try {
                    insertMatchingKeysInto(propertyStringTree.getPrefixedBy(prefix), category,
                            matches);
                } finally {
                    propertyStringTree.lock.readLock().unlock();
                }
            }
        }
        return matches;
    }

    private void insertMatchingKeysInto(Map<String, Collection<RemoteFileItem>> prefixedBy,
            SearchCategory category, Collection<String> results) {
        if (category == SearchCategory.ALL) {
            results.addAll(prefixedBy.keySet());
        } else {
            for (Map.Entry<String, Collection<RemoteFileItem>> item : prefixedBy.entrySet()) {
                if (containsCategory(category, item.getValue())) {
                    results.add(item.getKey());
                }
            }
        }
    }

    private boolean containsCategory(SearchCategory category,
            Collection<RemoteFileItem> remoteFileItems) {
        for (RemoteFileItem item : remoteFileItems) {
            if (category == SearchCategory.forCategory(item.getCategory())) {
                return true;
            }
        }
        return false;
    }

    /** Returns all RemoteFileItems that match the query. */
    public Collection<RemoteFileItem> getMatchingItems(SearchDetails searchDetails) {

        Set<RemoteFileItem> matches = standardSearch(searchDetails);
        matches = advancedSearch(searchDetails, matches);

        if (matches != null) {
            return matches;
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Returns Set of remote file items matching the advanced SearchDetails. If
     * it is a valid search but no results match, then an empty set will be
     * returned. If the search is not valid, i.e. there is advanced search data.
     * then a null set will be returned.
     */
    private Set<RemoteFileItem> advancedSearch(SearchDetails searchDetails,
            Set<RemoteFileItem> matches) {
        SearchCategory category = searchDetails.getSearchCategory();
        Map<FilePropertyKey, String> advancedDetails = searchDetails.getAdvancedDetails();

        if (advancedDetails != null && advancedDetails.size() > 0) {
            for (FilePropertyKey filePropertyKey : advancedDetails.keySet()) {
                String phrase = advancedDetails.get(filePropertyKey);
                StringTokenizer st = new StringTokenizer(phrase);
                while (st.hasMoreElements()) {
                    Set<RemoteFileItem> keywordMatches = new HashSet<RemoteFileItem>();
                    String keyword = st.nextToken();
                    for (Library library : libraries.values()) {
                        RemoteFileItemStringTrie propertyStringTrie = library
                                .getFilePropertyIndex(filePropertyKey);
                        if (propertyStringTrie != null) {
                            propertyStringTrie.lock.readLock().lock();
                            try {
                                insertMatchingItemsInto(propertyStringTrie.getPrefixedBy(keyword)
                                        .values(), category, keywordMatches, matches);
                            } finally {
                                propertyStringTrie.lock.readLock().unlock();
                            }
                        }
                    }

                    if (matches == null) {
                        matches = keywordMatches;
                    } else {
                        // Otherwise, we're looking for additional keywords
                        // -- retain only matched ones.
                        matches.retainAll(keywordMatches);
                    }

                    // Optimization: If nothing matched this keyword,
                    // nothing can be added.
                    if (matches.isEmpty()) {
                        return Collections.emptySet();
                    }
                }
            }
        }
        return matches;
    }

    /**
     * Returns Set of remote file items matching the stand SearchDetails. If it
     * is a valid search but no results match, then an empty set will be
     * returned. If the search is not valid, i.e. there is no query string. then
     * a null set will be returned.
     */
    private Set<RemoteFileItem> standardSearch(SearchDetails searchDetails) {
        String query = searchDetails.getSearchQuery();
        SearchCategory category = searchDetails.getSearchCategory();
        Set<RemoteFileItem> matches = null;
        StringTokenizer st = new StringTokenizer(query);
        while (st.hasMoreElements()) {
            Set<RemoteFileItem> keywordMatches = new HashSet<RemoteFileItem>();
            String keyword = st.nextToken();
            for (Library library : libraries.values()) {
                library.fileNameIndex.lock.readLock().lock();
                try {
                    insertMatchingItemsInto(library.fileNameIndex.getPrefixedBy(keyword).values(),
                            category, keywordMatches, matches);
                } finally {
                    library.fileNameIndex.lock.readLock().unlock();
                }

                for (FilePropertyKey filePropertyKey : FilePropertyKey.getIndexableKeys()) {
                    RemoteFileItemStringTrie propertyStringTrie = library
                            .getFilePropertyIndex(filePropertyKey);
                    if (propertyStringTrie != null) {
                        propertyStringTrie.lock.readLock().lock();
                        try {
                            insertMatchingItemsInto(propertyStringTrie.getPrefixedBy(keyword)
                                    .values(), category, keywordMatches, matches);
                        } finally {
                            propertyStringTrie.lock.readLock().unlock();
                        }
                    }
                }
            }

            // If this is the first keyword, just assign matches to it.
            if (matches == null) {
                matches = keywordMatches;
            } else {
                // Otherwise, we're looking for additional keywords -- retain
                // only matched ones.
                matches.retainAll(keywordMatches);
            }

            // Optimization: If nothing matched this keyword, nothing can be
            // added.
            if (matches.isEmpty()) {
                return Collections.emptySet();
            }
        }
        return matches;
    }

    private void insertMatchingItemsInto(Collection<Collection<RemoteFileItem>> prefixedBy,
            SearchCategory category, Set<RemoteFileItem> storage, Set<RemoteFileItem> allowedItems) {
        for (Collection<RemoteFileItem> remoteFileItems : prefixedBy) {
            for (RemoteFileItem item : remoteFileItems) {
                Category testCategory = item.getCategory();
                boolean allowCategory = category == SearchCategory.ALL
                        || category == SearchCategory.forCategory(testCategory);
                boolean allowItem = allowedItems == null || allowedItems.contains(item);
                if (allowCategory && allowItem) {
                    storage.add(item);
                }
            }
        }
    }

    private static class RemoteFileItemStringTrie extends PatriciaTrie<String, Collection<RemoteFileItem>> {
        private final ReadWriteLock lock;

        public RemoteFileItemStringTrie() {
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
            return super.containsKey(canonicalize((String) k));
        }

        @Override
        public Collection<RemoteFileItem> get(Object k) {
            return super.get(canonicalize((String) k));
        }

        @Override
        public SortedMap<String, Collection<RemoteFileItem>> getPrefixedBy(String key, int offset, int length) {
            return super.getPrefixedBy(canonicalize(key), offset, length);
        }

        @Override
        public SortedMap<String, Collection<RemoteFileItem>> getPrefixedBy(String key, int length) {
            return super.getPrefixedBy(canonicalize(key), length);
        }

        @Override
        public SortedMap<String, Collection<RemoteFileItem>> getPrefixedBy(String key) {
            return super.getPrefixedBy(canonicalize(key));
        }

        @Override
        public SortedMap<String, Collection<RemoteFileItem>> getPrefixedByBits(String key,
                int bitLength) {
            return super.getPrefixedByBits(canonicalize(key), bitLength);
        }

        @Override
        public SortedMap<String, Collection<RemoteFileItem>> headMap(String toKey) {
            return super.headMap(canonicalize(toKey));
        }

        @Override
        public Collection<RemoteFileItem> put(String key, Collection<RemoteFileItem> value) {
            return super.put(canonicalize(key), value);
        }

        @Override
        public Collection<RemoteFileItem> remove(Object k) {
            return super.remove(canonicalize((String) k));
        }

        @Override
        public Entry<String, Collection<RemoteFileItem>> select(String key,
                Cursor<? super String, ? super Collection<RemoteFileItem>> cursor) {
            return super.select(canonicalize(key), cursor);
        }

        @Override
        public Collection<RemoteFileItem> select(String key) {
            return super.select(canonicalize(key));
        }

        @Override
        public SortedMap<String, Collection<RemoteFileItem>> subMap(String fromKey, String toKey) {
            return super.subMap(canonicalize(fromKey), canonicalize(toKey));
        }

        @Override
        public SortedMap<String, Collection<RemoteFileItem>> tailMap(String fromKey) {
            return super.tailMap(canonicalize(fromKey));
        }

        /**
         * Adds the given word to the index as a whole.
         */
        public void addWordToIndex(RemoteFileItem newFile, String word) {
            LOG.debugf("\t {0}", word);
            Collection<RemoteFileItem> filesForWord;
            filesForWord = get(word);
            if (filesForWord == null) {
                filesForWord = new ArrayList<RemoteFileItem>();
                put(word, filesForWord);
            }
            filesForWord.add(newFile);
        }

        /**
         * Takes the given phrase and tokenizes it by spaces. Each individual
         * token gets added to the index.
         */
        public void addPhraseToIndex(RemoteFileItem newFile, String phrase) {
            StringTokenizer st = new StringTokenizer(phrase);
            while (st.hasMoreElements()) {
                String word = st.nextToken();
                addWordToIndex(newFile, word);
            }
        }
    }

    /**
     * Listens to events on a specific presence and updates the library index
     * based on these events.
     */
    private static class LibraryListener implements ListEventListener<RemoteFileItem> {
        private final Library library;

        LibraryListener(Library library) {
            this.library = library;
        }

        public void listChanged(ListEvent<RemoteFileItem> listChanges) {
            // optimization:  if we know the ultimate list is size 0, clear & exit
            if(listChanges.getSourceList().size() == 0) {
                library.clear();
            } else {
                while (listChanges.next()) {
                    switch(listChanges.getType()) {
                    case ListEvent.INSERT:
                        RemoteFileItem newFile = listChanges.getSourceList().get(listChanges.getIndex());
                        index(newFile);                    
                        break;
                    case ListEvent.DELETE:
                    case ListEvent.UPDATE:
                        // TODO: if glazedlists supported retrieving the removed items,
                        //       we could just update the one element -- instead,
                        //       we need to rebuild the whole thing.
                        rebuild(listChanges.getSourceList());
                        return;
                    }
                }
            }
        }
        
        private void rebuild(List<RemoteFileItem> files) {
            library.clear();
            for(RemoteFileItem item : files) {
                index(item);
            }
        }

        /**
         * Indexes the files properties and name.
         */
        private void index(RemoteFileItem newFile) {
            library.indexFileName(newFile);

            for (FilePropertyKey filePropertyKey : FilePropertyKey.getIndexableKeys()) {
                Object property = newFile.getProperty(filePropertyKey);
                if (property != null) {
                    String sentence = property.toString();
                    library.indexProperty(newFile, filePropertyKey, sentence);
                }
            }
        }
    }
}
