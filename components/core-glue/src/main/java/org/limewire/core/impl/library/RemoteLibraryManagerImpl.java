package org.limewire.core.impl.library;

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Comparator;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceListener;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.UniqueList;
import ca.odell.glazedlists.ObservableElementList.Connector;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.impl.ReadOnlyList;
import ca.odell.glazedlists.util.concurrent.Lock;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RemoteLibraryManagerImpl implements RemoteLibraryManager, RegisteringEventListener<RosterEvent> {
    private final EventList<FriendLibrary> allFriendLibraries;
    private final EventList<FriendLibrary> readOnlyFriendLibraries;
    private volatile EventList<FriendLibrary> swingFriendLibraries;

    @Inject
    public RemoteLibraryManagerImpl() {
        Connector<FriendLibrary> connector = GlazedLists.beanConnector(FriendLibrary.class);        
        allFriendLibraries = GlazedListsFactory.observableElementList(GlazedListsFactory.threadSafeList(new BasicEventList<FriendLibrary>()), connector);
        readOnlyFriendLibraries = GlazedListsFactory.readOnlyList(allFriendLibraries);
    }

    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }

    public void handleEvent(RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {
            userAdded(event.getSource());
        }
    }
    
    @Override
    public EventList<FriendLibrary> getFriendLibraryList() {
        return readOnlyFriendLibraries;
    }
    
    @Override
    public EventList<FriendLibrary> getSwingFriendLibraryList() {
        assert EventQueue.isDispatchThread();
        if(swingFriendLibraries == null) {
            swingFriendLibraries = GlazedListsFactory.swingThreadProxyEventList(readOnlyFriendLibraries);            
        }
        return swingFriendLibraries;
    }

    private void userAdded(final User user) {
        user.addPresenceListener(new PresenceListener() {            
            public void presenceChanged(final Presence presence) {
                if(presence.getType().equals(Presence.Type.available)) {
                    if(presence instanceof LimePresence) {
                        FriendLibraryImpl friendLibrary = getOrCreateFriendLibrary(user);
                        friendLibrary.getOrCreatePresenceLibrary((LimePresence)presence);
                    }
                } else if(presence.getType().equals(Presence.Type.unavailable)) {
                    if(presence instanceof LimePresence) {
                        FriendLibraryImpl friendLibrary = getFriendLibrary(user);
                        if(friendLibrary != null) {
                            friendLibrary.removePresenceLibrary((LimePresence)presence);
                            // TODO race condition
                            if(friendLibrary.size() == 0) {
                                removeFriendLibrary(friendLibrary);
                            }
                        }
                    }
                }
            }
        });
    }
    
    private FriendLibraryImpl findFriendLibrary(Friend friend) {
        for(FriendLibrary library : allFriendLibraries) {
            if(library.getFriend().getId().equals(friend.getId())) {
                return (FriendLibraryImpl)library;
            }
        }
        return null;
    }

    private FriendLibraryImpl getOrCreateFriendLibrary(Friend friend) {
        Lock lock = allFriendLibraries.getReadWriteLock().writeLock();
        lock.lock();
        try {
            FriendLibraryImpl library = findFriendLibrary(friend);
            if(library == null) {
                library = new FriendLibraryImpl(friend);
                library.commit();
                allFriendLibraries.add(library);
            }
            return library;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean hasFriendLibrary(Friend friend) {
        return getFriendLibrary(friend) != null;
    }
    
    private FriendLibraryImpl getFriendLibrary(Friend friend) {
        Lock lock = allFriendLibraries.getReadWriteLock().readLock();
        lock.lock();
        try {
            return findFriendLibrary(friend);
        } finally {
            lock.unlock();
        }
    }

    private void removeFriendLibrary(FriendLibraryImpl friendLibrary) {
        Lock lock = allFriendLibraries.getReadWriteLock().writeLock();
        lock.lock();
        try {
            allFriendLibraries.remove(friendLibrary);
            friendLibrary.dispose();
        } finally {
            lock.unlock();
        }
    }

    private static class FriendLibraryImpl implements FriendLibrary {
        private final Friend friend;

        private final ObservableElementList<PresenceLibrary> allPresenceLibraries;
        private final ReadOnlyList<PresenceLibrary> readOnlyPresenceLibraries;
        
        private final CompositeList<RemoteFileItem> compositeList;
        private final UniqueList<RemoteFileItem> threadSafeUniqueList;
        private volatile TransformedList<RemoteFileItem, RemoteFileItem> swingList;
        
        private final PropertyChangeSupport changeSupport;
        private volatile LibraryState state;

        public FriendLibraryImpl(Friend friend) {
            this.friend = friend;
            compositeList = new CompositeList<RemoteFileItem>();
            threadSafeUniqueList = GlazedListsFactory.uniqueList(GlazedListsFactory.threadSafeList(compositeList),
                    new Comparator<RemoteFileItem>() {
                @Override
                public int compare(RemoteFileItem o1, RemoteFileItem o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            
            changeSupport = new PropertyChangeSupport(this);
            
            Connector<PresenceLibrary> connector = GlazedLists.beanConnector(PresenceLibrary.class);            
            allPresenceLibraries = GlazedListsFactory.observableElementList(GlazedListsFactory.threadSafeList(new BasicEventList<PresenceLibrary>()), connector);
            readOnlyPresenceLibraries = GlazedListsFactory.readOnlyList(allPresenceLibraries);
            
            allPresenceLibraries.addListEventListener(new ListEventListener<PresenceLibrary>() {
                @Override
                public void listChanged(ListEvent<PresenceLibrary> listChanges) {
                    LibraryState oldState = state;
                    state = calculateState();
                    changeSupport.firePropertyChange("state", oldState, state);
                }
            });
        }
        
        @Override
        public EventList<PresenceLibrary> getPresenceLibraryList() {
            return readOnlyPresenceLibraries;
        }
        
        @Override
        public LibraryState getState() {
            return state;
        }
        
        private LibraryState calculateState() {
            Lock lock = allPresenceLibraries.getReadWriteLock().readLock();
            lock.lock();
            try {
                boolean oneCompleted = false;
                for(PresenceLibrary library : allPresenceLibraries) {
                    switch (library.getState()) {
                    case LOADING:
                        return LibraryState.LOADING;
                    case LOADED:
                        oneCompleted = true;
                        break;

                    }
                }
                if(oneCompleted) {
                    return LibraryState.LOADED;
                } else {
                    return LibraryState.FAILED_TO_LOAD;
                }
            } finally {
                lock.unlock();
            }
        }

        public Friend getFriend() {
            return friend;
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
        
        private PresenceLibraryImpl findPresenceLibrary(LimePresence presence) {
            for(PresenceLibrary library : allPresenceLibraries) {
                if(library.getPresence().getJID().equals(presence.getJID())) {
                    return (PresenceLibraryImpl)library;
                }
            }
            return null;
        }

        private PresenceLibraryImpl getOrCreatePresenceLibrary(LimePresence presence) {
            PresenceLibraryImpl library;
            boolean created = false;
            
            Lock lock = allPresenceLibraries.getReadWriteLock().writeLock();
            lock.lock();
            try {
                library = findPresenceLibrary(presence);
                if(library == null) {
                    created = true;
                    library = new PresenceLibraryImpl(presence, createMemberList());
                    allPresenceLibraries.add(library);
                }
            } finally {
                lock.unlock();
            }
            
            if(created) {
                library.commit();
                addMemberList(library);
            }
            
            return library;
        }

        private void removePresenceLibrary(LimePresence presence) {
            PresenceLibraryImpl removed = null;
            
            Lock lock = allPresenceLibraries.getReadWriteLock().writeLock();
            lock.lock();
            try {
                PresenceLibraryImpl presenceLibrary = findPresenceLibrary(presence);
                if(presenceLibrary != null) {
                    removed = presenceLibrary;
                    allPresenceLibraries.remove(presenceLibrary);
                    presenceLibrary.dispose();
                }
            } finally {
                lock.unlock();
            }
            
            if(removed != null) {
                removeMemberList(removed);
            }
        }

        private void removeMemberList(PresenceLibrary presenceLibrary) {
            compositeList.getReadWriteLock().writeLock().lock();
            try {
                compositeList.removeMemberList(presenceLibrary.getModel());
            } finally {
                compositeList.getReadWriteLock().writeLock().unlock();
            }
        }

        private EventList<RemoteFileItem> createMemberList() {
            return compositeList.createMemberList();
        }

        private void addMemberList(PresenceLibrary presenceLibrary) {
            compositeList.getReadWriteLock().writeLock().lock();
            try {
                compositeList.addMemberList(presenceLibrary.getModel());
            } finally {
                compositeList.getReadWriteLock().writeLock().unlock();
            }
        }

        @Override
        public EventList<RemoteFileItem> getModel() {
            return threadSafeUniqueList;
        }

        @Override
        public EventList<RemoteFileItem> getSwingModel() {
            assert EventQueue.isDispatchThread();
            if(swingList == null) {
                swingList =  GlazedListsFactory.swingThreadProxyEventList(threadSafeUniqueList);
            }
            return swingList;
        }

        @Override
        public int size() {
            return threadSafeUniqueList.size();
        }


        public void addFile(RemoteFileItem file) {
            throw new UnsupportedOperationException();
        }

        public void removeFile(RemoteFileItem file) {
            throw new UnsupportedOperationException();
        }

        void commit() {
            // Add things here after we guarantee we want to use this list.
        }

        void dispose() {
            if(swingList != null) {
                swingList.dispose();
            }
            compositeList.dispose();
            threadSafeUniqueList.dispose();
            readOnlyPresenceLibraries.dispose();
            allPresenceLibraries.dispose();
        }
        
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            changeSupport.addPropertyChangeListener(listener);
        }
        
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            changeSupport.removePropertyChangeListener(listener);
        }
        
        @Override
        public String toString() {
            return StringUtils.toString(this);
        }
        
        //TODO: add new accessors appropriate for creating FileItems based on
        //      lookups. May also need to subclass CoreFileItem appropriate for
        //      friend library info.
    }

    private static class PresenceLibraryImpl implements PresenceLibrary {
        protected final TransformedList<RemoteFileItem, RemoteFileItem> eventList;
        protected volatile TransformedList<RemoteFileItem, RemoteFileItem> swingEventList;
        private final LimePresence limePresence;
        private volatile LibraryState state = LibraryState.LOADING;
        
        private final PropertyChangeSupport changeSupport;

        PresenceLibraryImpl(LimePresence limePresence, EventList<RemoteFileItem> list) {
            this.limePresence = limePresence;
            eventList = GlazedListsFactory.threadSafeList(list);
            changeSupport = new PropertyChangeSupport(this);
        }

        public LimePresence getPresence() {
            return limePresence;
        }

        @Override
        public EventList<RemoteFileItem> getModel() {
            return eventList;
        }

        @Override
        public EventList<RemoteFileItem> getSwingModel() {
            assert EventQueue.isDispatchThread();
            if(swingEventList == null) {
                swingEventList =  GlazedListsFactory.swingThreadProxyEventList(eventList);
            }
            return swingEventList;
        }

        void dispose() {
            if(swingEventList != null) {
                swingEventList.dispose();
            }
            eventList.dispose();
        }

        public void addFile(RemoteFileItem file) {
            eventList.add(file);
        }

        public void removeFile(RemoteFileItem file) {
            eventList.remove(file);
        }

        @Override
        public int size() {
            return eventList.size();
        }

        void commit() {
            // Add things here after we guarantee we want to use this list.
        }

        public void clear() {
        }
        
        @Override
        public LibraryState getState() {
            return state;
        }
        
        @Override
        public void setState(LibraryState newState) {
            LibraryState oldState = state;
            this.state = newState;
            changeSupport.firePropertyChange("state", oldState, newState);
        }
        
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            changeSupport.addPropertyChangeListener(listener);
        }
        
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            changeSupport.removePropertyChangeListener(listener);
        }
    }
}
