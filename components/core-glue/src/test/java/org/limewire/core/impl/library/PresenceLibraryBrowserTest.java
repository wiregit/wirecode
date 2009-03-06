package org.limewire.core.impl.library;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.SocketsManager;
import org.limewire.util.BaseTestCase;
import org.limewire.util.MatchAndCopy;
import org.limewire.xmpp.api.client.LibraryChanged;
import org.limewire.xmpp.api.client.LibraryChangedEvent;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEventListener;

public class PresenceLibraryBrowserTest extends BaseTestCase {

    public PresenceLibraryBrowserTest(String name) {
        super(name);
    }
    
    @SuppressWarnings("unchecked")
    public void testListeners() {
        Mockery context = new Mockery();
        
        final ListenerSupport<LibraryChangedEvent> listenerSupport = context.mock(ListenerSupport.class);
        
        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        final SocketsManager socketsManager = context.mock(SocketsManager.class);
        
        final FriendPresence presence = context.mock(FriendPresence.class);
        final EventList<FriendLibrary> friendLibraryList = context.mock(EventList.class);
        
        final MatchAndCopy<EventListener> listenerCollector = new MatchAndCopy<EventListener>(EventListener.class);
        
        final MatchAndCopy<ListEventListener> friendListenerCollector
            = new MatchAndCopy<ListEventListener>(ListEventListener.class);
        
        final PresenceLibraryBrowser presenceLibraryBrowser
            = new PresenceLibraryBrowser(null, remoteLibraryManager, socketsManager, null);
        
        context.checking(new Expectations() {{
            allowing(remoteLibraryManager).getFriendLibraryList();
            will(returnValue(friendLibraryList));
            
            exactly(1).of(listenerSupport).addListener(presenceLibraryBrowser);
            exactly(1).of(remoteLibraryManager).removePresenceLibrary(presence);
            exactly(1).of(remoteLibraryManager).addPresenceLibrary(presence);
            exactly(1).of(socketsManager).addListener(with(listenerCollector));
            exactly(1).of(friendLibraryList).addListEventListener(with(friendListenerCollector));
        }});
        
        presenceLibraryBrowser.register(listenerSupport);
        presenceLibraryBrowser.handleEvent(new LibraryChangedEvent(presence, LibraryChanged.LIBRARY_CHANGED));
        
        context.assertIsSatisfied();
    }

}
