package org.limewire.core.impl.library;

import java.util.Iterator;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.search.SearchCategory;

public class FriendLibraryAutoCompleter implements AutoCompleteDictionary {
    private final FriendLibraries friendLibraries;
    private final SearchCategory category;

    public FriendLibraryAutoCompleter(FriendLibraries friendLibraries, SearchCategory category) {
        this.friendLibraries = friendLibraries;
        this.category = category;
    }
    
    public void addEntry(String s) {
        throw new UnsupportedOperationException();
    }

    public boolean removeEntry(String s) {
        throw new UnsupportedOperationException();
    }

    public String lookup(String s) {
        Iterator<String> it = iterator(s);
        if (!it.hasNext())
            return null;
        return it.next();
    }

    public Iterator<String> iterator() {
        return iterator(null);
    }

    public Iterator<String> iterator(String s) {
        final Iterator<RemoteFileItem> files = friendLibraries.iterator(s, category, false);
        return new Iterator<String>() {
            public boolean hasNext() {
                return files.hasNext();
            }

            public String next() {
                return files.next().getName();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }
}

