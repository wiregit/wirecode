package org.limewire.core.impl.library;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.collection.MultiIterator;
import org.limewire.collection.StringTrie;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

@Singleton
public class FriendLibraryAutoCompleter implements AutoCompleteDictionary, RegisteringEventListener<RosterEvent> {
    private final LibraryManager libraryManager;
    private final Map<String, StringTrie<ConcurrentLinkedQueue<RemoteFileItem>>> libraries;
    private static final Log LOG = LogFactory.getLog(FriendLibraryAutoCompleter.class);

    @Inject
    FriendLibraryAutoCompleter(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
        this.libraries = new ConcurrentHashMap<String, StringTrie<ConcurrentLinkedQueue<RemoteFileItem>>>();
    }

    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }

    public void handleEvent(RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {
            String name = event.getSource().getName();
            if(name == null) {
                name = event.getSource().getId();
            }
            synchronized (libraryManager) {
                if(!libraryManager.containsBuddyLibrary(name)) {
                    libraryManager.addBuddyLibrary(name);
                }
            }
            StringTrie<ConcurrentLinkedQueue<RemoteFileItem>> library = new StringTrie<ConcurrentLinkedQueue<RemoteFileItem>>(true);
            LOG.debugf("adding friend library " + name + " to index");
            libraries.put(name, library);
            // TODO race condition?
            libraryManager.getBuddyLibrary(name).getModel().addListEventListener(new LibraryListener(library));
        } else if(event.getType().equals(User.EventType.USER_REMOVED)) {
            String name = event.getSource().getName();
            if(name == null) {
                event.getSource().getId();
            }
            LOG.debugf("removing friend library " + name + " from index");
            libraries.remove(name);
        }
    }

    class LibraryListener implements ListEventListener<RemoteFileItem> {

        final StringTrie<ConcurrentLinkedQueue<RemoteFileItem>> library;

        LibraryListener(StringTrie<ConcurrentLinkedQueue<RemoteFileItem>> library) {
            this.library = library;
        }

        public void listChanged(ListEvent<RemoteFileItem> listChanges) {
            while(listChanges.next()) {
                if(listChanges.getType() == ListEvent.INSERT) {
                    RemoteFileItem newFile = listChanges.getSourceList().get(listChanges.getIndex());
                    LOG.debugf("adding file {0}, indexing under:", newFile.getName());
                    StringTokenizer st = new StringTokenizer(newFile.getName());
                    while(st.hasMoreElements()) {
                        String word = st.nextToken();
                        LOG.debugf("\t {0}", word);
                        ConcurrentLinkedQueue<RemoteFileItem> filesForWord;
                        synchronized (library) {
                            filesForWord = library.get(word);
                            if(filesForWord == null) {
                                filesForWord = new ConcurrentLinkedQueue<RemoteFileItem>();
                                library.add(word, filesForWord);
                            }
                        }
                        filesForWord.add(newFile);
                    }
                } else if(listChanges.getType() == ListEvent.DELETE) {
                    RemoteFileItem newFile = listChanges.getSourceList().get(listChanges.getIndex());
                    LOG.debugf("removing file {0} from index", newFile.getName());
                    StringTokenizer st = new StringTokenizer(newFile.getName());
                    while(st.hasMoreElements()) {
                        String word = st.nextToken();
                       ConcurrentLinkedQueue<RemoteFileItem> filesForWord;
                        synchronized (library) {
                            filesForWord = library.get(word);
                            if(filesForWord == null) {
                                filesForWord = new ConcurrentLinkedQueue<RemoteFileItem>();
                                library.add(word, filesForWord);
                            }
                        }
                        filesForWord.remove(newFile);
                    }
                }
            }
        }
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
        List<Iterator<String>> iterators = new ArrayList<Iterator<String>>();
        for(StringTrie<ConcurrentLinkedQueue<RemoteFileItem>> library : libraries.values()) {
            iterators.add(iterator(library.getIterator()));
        }
        return new MultiIterator(iterators.toArray(new Iterator[]{}));
    }

    private Iterator<String> iterator(final Iterator<ConcurrentLinkedQueue<RemoteFileItem>> iterator) {
        List<Iterator<String>> iterators = new ArrayList<Iterator<String>>();        
        while(iterator.hasNext()) {
            iterators.add(iterator(iterator.next()));
        }
        return new MultiIterator(iterators.toArray(new Iterator[]{}));
    }

    private Iterator<String> iterator(ConcurrentLinkedQueue<RemoteFileItem> remoteFileItems) {
        final Iterator<RemoteFileItem> files = remoteFileItems.iterator();
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
        List<Iterator<String>> iterators = new ArrayList<Iterator<String>>();
        for(StringTrie<ConcurrentLinkedQueue<RemoteFileItem>> library : libraries.values()) {
            iterators.add(iterator(library.getPrefixedBy(s)));
        }
        return new MultiIterator(iterators.toArray(new Iterator[]{}));
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }
}

