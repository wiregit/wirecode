package org.limewire.core.impl.library;

import java.awt.EventQueue;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.FriendLibraryEvent;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.PresenceLibraryEvent;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceListener;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;

@Singleton
public class RemoteLibraryManagerImpl implements RemoteLibraryManager, RegisteringEventListener<RosterEvent> {
    private final ConcurrentHashMap<String, FriendLibraryImpl> friendLibraryFileLists;
    private final EventListener<FriendLibraryEvent> friendLibraryEventListener;

    @Inject
    public RemoteLibraryManagerImpl(EventListener<FriendLibraryEvent> friendLibraryEventListener) {
        this.friendLibraryFileLists = new ConcurrentHashMap<String, FriendLibraryImpl>();
        this.friendLibraryEventListener = friendLibraryEventListener;
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
                        FriendLibraryImpl friendLibrary = getOrCreateFriendLibrary(user);
                        friendLibrary.removePresenceLibrary((LimePresence)presence);
                        // TODO race condition
                        if(friendLibrary.size() == 0) {
                            removeFriendLibrary(friendLibrary.getFriend());
                        }
                    }
                }
            }
        });
    }

    private FriendLibraryImpl getOrCreateFriendLibrary(Friend friend) {
        FriendLibraryImpl newList = new FriendLibraryImpl(friend);
        FriendLibraryImpl existing = friendLibraryFileLists.putIfAbsent(friend.getId(), newList);

        if(existing == null) {
            newList.commit();
            friendLibraryEventListener.handleEvent(new FriendLibraryEvent(FriendLibraryEvent.Type.LIBRARY_ADDED, newList));
            return newList;
        } else {
            newList.dispose();
            return existing;
        }
    }

    @Override
    public boolean hasFriendLibrary(Friend friend) {
        return friendLibraryFileLists.get(friend.getId()) != null;
    }

    private void removeFriendLibrary(Friend friend) {
        FriendLibraryImpl list = friendLibraryFileLists.remove(friend.getId());
        if(list != null) {
            friendLibraryEventListener.handleEvent(new FriendLibraryEvent(FriendLibraryEvent.Type.LIBRARY_REMOVED, list));
            list.dispose();
        }
    }

    private class FriendLibraryImpl implements FriendLibrary {

        private final CompositeList<RemoteFileItem> compositeList;
        private final EventList<RemoteFileItem> threadSafeUniqueList;
        private volatile TransformedList<RemoteFileItem, RemoteFileItem> swingList;
        private final EventListenerList<PresenceLibraryEvent> listeners;
        private final Friend friend;
        private final ConcurrentHashMap<String, PresenceLibraryImpl> presenceLibraryFileLists;

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
            listeners = new EventListenerList<PresenceLibraryEvent>();
            presenceLibraryFileLists = new ConcurrentHashMap<String, PresenceLibraryImpl>();
        }

        public void addListener(EventListener<PresenceLibraryEvent> presenceLibraryEventEventListener) {
            listeners.addListener(presenceLibraryEventEventListener);
        }

        public boolean removeListener(EventListener<PresenceLibraryEvent> presenceLibraryEventEventListener) {
            return listeners.removeListener(presenceLibraryEventEventListener);
        }

        public Friend getFriend() {
            return friend;
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        public void removePresenceLibrary(LimePresence limePresence) {
            PresenceLibraryImpl list = presenceLibraryFileLists.remove(limePresence.getJID());
            if(list != null) {
                removeMemberList(list);
                list.dispose();
            }
        }

        public void removeMemberList(PresenceLibrary presenceLibrary) {
            compositeList.getReadWriteLock().writeLock().lock();
            try {
                compositeList.removeMemberList(presenceLibrary.getModel());
            } finally {
                compositeList.getReadWriteLock().writeLock().unlock();
            }
            listeners.broadcast(new PresenceLibraryEvent(presenceLibrary, PresenceLibraryEvent.Type.LIBRARY_REMOVED));
        }

        public EventList<RemoteFileItem> createMemberList() {
            return compositeList.createMemberList();
        }

        private void getOrCreatePresenceLibrary(LimePresence limePresence) {
            PresenceLibraryImpl newList = new PresenceLibraryImpl(limePresence, createMemberList());
            PresenceLibraryImpl existing = presenceLibraryFileLists.putIfAbsent(limePresence.getJID(), newList);

            if(existing == null) {
                newList.commit();
                addMemberList(newList);
            } else {
                newList.dispose();
            }
        }

        public void addMemberList(PresenceLibrary presenceLibrary) {
            compositeList.getReadWriteLock().writeLock().lock();
            try {
                compositeList.addMemberList(presenceLibrary.getModel());
            } finally {
                compositeList.getReadWriteLock().writeLock().unlock();
            }
            listeners.broadcast(new PresenceLibraryEvent(presenceLibrary, PresenceLibraryEvent.Type.LIBRARY_ADDED));
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
        }
        //TODO: add new accessors appropriate for creating FileItems based on
        //      lookups. May also need to subclass CoreFileItem appropriate for
        //      friend library info.
    }

    private class PresenceLibraryImpl implements PresenceLibrary {
        protected final TransformedList<RemoteFileItem, RemoteFileItem> eventList;
        protected volatile TransformedList<RemoteFileItem, RemoteFileItem> swingEventList;
        private final LimePresence limePresence;

        PresenceLibraryImpl(LimePresence limePresence, EventList<RemoteFileItem> list) {
            this.limePresence = limePresence;
            eventList = GlazedListsFactory.threadSafeList(list);
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
    }
}
