package org.limewire.core.impl.library;

import java.util.Iterator;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.library.RemoteFileItem;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FriendLibraryAutoCompleter implements AutoCompleteDictionary {
    private final FriendLibraries friendLibrarySearcher;

    @Inject
    FriendLibraryAutoCompleter(FriendLibraries friendLibrarySearcher) {
        this.friendLibrarySearcher = friendLibrarySearcher;
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
        final Iterator<RemoteFileItem> files = friendLibrarySearcher.iterator();
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

    public Iterator<String> iterator(String s) {
        final Iterator<RemoteFileItem> files = friendLibrarySearcher.iterator(s);
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

