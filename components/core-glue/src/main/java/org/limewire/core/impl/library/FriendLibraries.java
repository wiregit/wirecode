package org.limewire.core.impl.library;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.limewire.collection.MultiIterator;
import org.limewire.collection.StringTrie;
import org.limewire.core.api.library.BuddyLibraryEvent;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FriendLibraries {
    private final Map<String, StringTrie<ConcurrentLinkedQueue<RemoteFileItem>>> libraries;
    private static final Log LOG = LogFactory.getLog(FriendLibraries.class);

    FriendLibraries() {
        this.libraries = new ConcurrentHashMap<String, StringTrie<ConcurrentLinkedQueue<RemoteFileItem>>>();
    }
    
    @Inject void register(ListenerSupport<BuddyLibraryEvent> buddyLibrarySupport) {
        buddyLibrarySupport.addListener(new EventListener<BuddyLibraryEvent>() {
            @Override
            public void handleEvent(BuddyLibraryEvent event) {
                switch(event.getType()) {
                case BUDDY_ADDED:
                    StringTrie<ConcurrentLinkedQueue<RemoteFileItem>> trie = new StringTrie<ConcurrentLinkedQueue<RemoteFileItem>>(true);
                    LOG.debugf("adding friend library " + event.getId() + " to index");
                    libraries.put(event.getId(), trie);
                    // TODO race condition?
                    event.getFileList().getModel().addListEventListener(new LibraryListener(trie));
                    break;
                case BUDDY_REMOVED:
                    LOG.debugf("removing friend library " + event.getId() + " from index");
                    libraries.remove(event.getId());
                }
            }
        });
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

    public RemoteFileItem lookup(String s) {
        Iterator<RemoteFileItem> it = iterator(s);
        if (!it.hasNext())
            return null;
        return it.next();
    }

    @SuppressWarnings("unchecked")
    public Iterator<RemoteFileItem> iterator() {
        List<Iterator<RemoteFileItem>> iterators = new ArrayList<Iterator<RemoteFileItem>>();
        for(StringTrie<ConcurrentLinkedQueue<RemoteFileItem>> library : libraries.values()) {
            iterators.add(iterator(library.getIterator()));
        }
        return new MultiIterator<RemoteFileItem>(iterators.toArray(new Iterator[iterators.size()]));
    }

    @SuppressWarnings("unchecked")
    private Iterator<RemoteFileItem> iterator(final Iterator<ConcurrentLinkedQueue<RemoteFileItem>> iterator) {
        List<Iterator<RemoteFileItem>> iterators = new ArrayList<Iterator<RemoteFileItem>>();        
        while(iterator.hasNext()) {
            iterators.add(iterator(iterator.next()));
        }
        return new MultiIterator<RemoteFileItem>(iterators.toArray(new Iterator[iterators.size()]));
    }

    private Iterator<RemoteFileItem> iterator(ConcurrentLinkedQueue<RemoteFileItem> remoteFileItems) {
        return remoteFileItems.iterator();
    }

    @SuppressWarnings("unchecked")
    public Iterator<RemoteFileItem> iterator(String s) {
        List<Iterator<RemoteFileItem>> iterators = new ArrayList<Iterator<RemoteFileItem>>();
        for(StringTrie<ConcurrentLinkedQueue<RemoteFileItem>> library : libraries.values()) {
            iterators.add(iterator(library.getPrefixedBy(s)));
        }
        return new MultiIterator<RemoteFileItem>(iterators.toArray(new Iterator[iterators.size()]));
    }
}

