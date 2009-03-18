package org.limewire.core.impl.library;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.browse.Browse;
import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.io.Address;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.AddressResolutionObserver;
import org.limewire.util.BaseTestCase;
import org.limewire.util.MatchAndCopy;
import org.limewire.xmpp.api.client.LibraryChanged;
import org.limewire.xmpp.api.client.LibraryChangedEvent;
import org.limewire.xmpp.api.client.Presence;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.util.concurrent.Lock;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

public class PresenceLibraryBrowserTest extends BaseTestCase {

    // TODO: What happens when friends are removed? 
    
    public PresenceLibraryBrowserTest(String name) {
        super(name);
    }
    
    /**
     * Goes through the register phase and ensures all listeners are correctly connected
     */
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
        presenceLibraryBrowser.registerToRemoteLibraryManager();
        presenceLibraryBrowser.registerToSocksManager();
        presenceLibraryBrowser.handleEvent(new LibraryChangedEvent(presence, LibraryChanged.LIBRARY_CHANGED));
        
        context.assertIsSatisfied();
    }

    /**
     * Fires events for presence library changes but ones that should not spawn new browses.
     */
    @SuppressWarnings("unchecked")
    public void testBrowseNotInitiated() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        
        final EventList<FriendLibrary> friendLibraryList = context.mock(EventList.class);
        
        final MatchAndCopy<ListEventListener> friendListenerCollector
            = new MatchAndCopy<ListEventListener>(ListEventListener.class);
        
        
        final ListEvent<FriendLibrary> listEventBlank = context.mock(ListEvent.class);
        
        final PresenceLibraryBrowser presenceLibraryBrowser
            = new PresenceLibraryBrowser(null, remoteLibraryManager, null, null);
        
        context.checking(new Expectations() {{
            allowing(remoteLibraryManager).getFriendLibraryList();
            will(returnValue(friendLibraryList));
          
            one(listEventBlank).next();
            will(returnValue(true));
            one(listEventBlank).getType();
            will(returnValue(ListEvent.UPDATE));
            one(listEventBlank).next();
            will(returnValue(true));
            one(listEventBlank).getType();
            will(returnValue(ListEvent.DELETE));
            one(listEventBlank).next();
            will(returnValue(false));
            
            exactly(1).of(friendLibraryList).addListEventListener(with(friendListenerCollector));
        }});
        
        presenceLibraryBrowser.registerToRemoteLibraryManager();
        
        ListEventListener<FriendLibrary> listener = friendListenerCollector.getLastMatch();
        listener.listChanged(listEventBlank);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Fires events that should spawn a normal friend browse.  Full internal integration test.
     *  
     * <p>When the browse is complete makes sure the listener handles removing failed browses correctly.
     */
    @SuppressWarnings("unchecked")
    public void testBrowseIntegration() {
        final Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        final SocketsManager socketsManager = context.mock(SocketsManager.class);
        final BrowseFactory browseFactory = context.mock(BrowseFactory.class);
        
        final MatchAndCopy<ListEventListener> listenerCollector
            = new MatchAndCopy<ListEventListener>(ListEventListener.class);
        final MatchAndCopy<BrowseListener> browseListenerCollector 
            = new MatchAndCopy<BrowseListener>(BrowseListener.class);
        
        final ListEvent<FriendLibrary> listEvent = context.mock(ListEvent.class);
        final EventList<FriendLibrary> friendLibraryEventList = context.mock(EventList.class);
        final FriendLibrary friendLibrary = context.mock(FriendLibrary.class);
        final EventList<PresenceLibrary> presenceLibraryEventList = context.mock(EventList.class);
        
        final ReadWriteLock dummyRWLock = context.mock(ReadWriteLock.class);
        final Lock dummyLock = context.mock(Lock.class);
        
        final ListEvent<PresenceLibrary> addEvent = context.mock(ListEvent.class);       
        final PresenceLibrary presenceLibrary = context.mock(PresenceLibrary.class);
        
        final ListEvent<PresenceLibrary> updateAndRemoveEvent = context.mock(ListEvent.class);
        
        final PresenceLibraryBrowser presenceLibraryBrowser
            = new PresenceLibraryBrowser(browseFactory, remoteLibraryManager, socketsManager, null);
        
        context.checking(new Expectations() {{
            allowing(remoteLibraryManager).getFriendLibraryList();
            will(returnValue(friendLibraryEventList));
          
            one(listEvent).next();
            will(returnValue(true));
            one(listEvent).getType();
            will(returnValue(ListEvent.INSERT));
            one(listEvent).next();
            will(returnValue(false));
            
            allowing(listEvent).getSourceList();
            will(returnValue(friendLibraryEventList));
            allowing(listEvent).getIndex();
            will(returnValue(777));
            allowing(friendLibraryEventList).get(777);
            will(returnValue(friendLibrary));
            
            allowing(friendLibrary).getPresenceLibraryList();
            will(returnValue(presenceLibraryEventList));
            allowing(presenceLibraryEventList).getReadWriteLock();
            will(returnValue(dummyRWLock));
            allowing(presenceLibraryEventList).toArray();
            will(returnValue(new Object[0]));
            
            allowing(dummyRWLock).readLock();
            will(returnValue(dummyLock));
            allowing(dummyLock).lock();
            allowing(dummyLock).unlock();
            
            one(addEvent).next();
            will(returnValue(true));
            one(addEvent).getType();
            will(returnValue(ListEvent.INSERT));
            one(addEvent).next();
            will(returnValue(false));
            one(updateAndRemoveEvent).next();
            will(returnValue(true));
            one(updateAndRemoveEvent).getType();
            will(returnValue(ListEvent.UPDATE));
            one(updateAndRemoveEvent).next();
            will(returnValue(true));
            one(updateAndRemoveEvent).getType();
            will(returnValue(ListEvent.DELETE));
            one(updateAndRemoveEvent).next();
            will(returnValue(false));   
            
            allowing(addEvent).getSourceList();
            will(returnValue(presenceLibraryEventList));
            allowing(presenceLibraryEventList).get(0);
            will(returnValue(presenceLibrary));
            allowing(addEvent).getIndex();
            will(returnValue(0));
            allowing(updateAndRemoveEvent).getSourceList();
            will(returnValue(presenceLibraryEventList));
            allowing(updateAndRemoveEvent).getIndex();
            will(returnValue(0));
            
            // TODO: why two?  
            exactly(2).of(presenceLibrary).setState(LibraryState.LOADING);
            
            FriendPresence friendPresence = context.mock(FriendPresence.class);
            allowing(presenceLibrary).getPresence();
            will(returnValue(friendPresence));
            
            AddressFeature addressFeature = context.mock(AddressFeature.class);
            allowing(friendPresence).getFeature(AddressFeature.ID);
            will(returnValue(addressFeature));
            
            Address address = context.mock(Address.class);
            allowing(addressFeature).getFeature();
            will(returnValue(address));
            allowing(socketsManager).canResolve(address);
            will(returnValue(false));
            allowing(socketsManager).canConnect(address);
            will(returnValue(true));
            
            allowing(friendPresence).getPresenceId();
            Browse browse = context.mock(Browse.class);
            allowing(browseFactory).createBrowse(friendPresence);
            will(returnValue(browse));
            
            Friend friend = context.mock(Friend.class);
            allowing(friendPresence).getFriend();
            will(returnValue(friend));
            allowing(friend).isAnonymous();
            will(returnValue(true));
            
            // Assertions
            exactly(1).of(friendLibraryEventList).addListEventListener(with(listenerCollector));
            exactly(1).of(presenceLibraryEventList).addListEventListener(with(listenerCollector));
            exactly(1).of(browse).start(with(browseListenerCollector));
            exactly(1).of(presenceLibrary).setState(LibraryState.LOADED);
        }});
        
        // Register the initial listener to listen for new remote libraries to manage browsing
        presenceLibraryBrowser.registerToRemoteLibraryManager();
        
        // Force fire a friend being added to the browse list 
        ListEventListener<FriendLibrary> friendListener = listenerCollector.getLastMatch();
        friendListener.listChanged(listEvent);
        
        // Force fire a presence coming available and thus initiate the browse
        assertEquals("A listener was not installed on the presence library",
                2, listenerCollector.getMatches().size());
        ListEventListener<PresenceLibrary> presenceListener = listenerCollector.getLastMatch();
        presenceListener.listChanged(addEvent);
        
        // Complete the browse
        BrowseListener browseListener = browseListenerCollector.getLastMatch();
        browseListener.browseFinished(true);
        
        // Test removal of simulated killed browse
        presenceLibraryBrowser.librariesToBrowse.add(presenceLibrary);
        presenceListener.listChanged(updateAndRemoveEvent);
        assertEmpty(presenceLibraryBrowser.librariesToBrowse);
        
        context.assertIsSatisfied();
    }

    /**
     * Simulates a browse with a resolvable address.
     * 
     * <p>Skips initial integration steps
     */
    public void testBrowseWithGoodResolve() {
        final Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        final SocketsManager socketsManager = context.mock(SocketsManager.class);
        final BrowseFactory browseFactory = context.mock(BrowseFactory.class);
        
        final PresenceLibrary presenceLibrary = context.mock(PresenceLibrary.class);
        
        final Address resolvedAddress = context.mock(Address.class);
        
        final PresenceLibraryBrowser presenceLibraryBrowser
            = new PresenceLibraryBrowser(browseFactory, remoteLibraryManager, socketsManager, null);
        
        final MatchAndCopy<AddressResolutionObserver> observerCollector 
            = new MatchAndCopy<AddressResolutionObserver>(AddressResolutionObserver.class);
        final MatchAndCopy<BrowseListener> browseListenerCollector 
            = new MatchAndCopy<BrowseListener>(BrowseListener.class);
        
        context.checking(new Expectations() {{
            
            allowing(presenceLibrary).setState(with(any(LibraryState.class)));
                        
            Presence presence = context.mock(Presence.class);
            allowing(presenceLibrary).getPresence();
            will(returnValue(presence));
            AddressFeature addressFeature = context.mock(AddressFeature.class);
            allowing(presence).getFeature(AddressFeature.ID);
            will(returnValue(addressFeature));
            Address address = context.mock(Address.class);
            allowing(addressFeature).getFeature();
            will(returnValue(address));
            allowing(socketsManager).canResolve(address);
            will(returnValue(true));
            
            allowing(socketsManager).canConnect(resolvedAddress);
            will(returnValue(true));
            allowing(presence).getPresenceId();
            will(returnValue("12321321 abajeña 12342323523452"));
            Browse browse = context.mock(Browse.class);
            allowing(browseFactory).createBrowse(presence);
            will(returnValue(browse));
            Friend friend = context.mock(Friend.class);
            allowing(presence).getFriend();
            will(returnValue(friend));
            allowing(friend).isAnonymous();
            will(returnValue(true));
            
            // Assertions
            exactly(1).of(socketsManager).resolve(with(same(address)), with(observerCollector));
            exactly(1).of(browse).start(with(browseListenerCollector));
            
        }});
        
        // Kick off the browse
        presenceLibraryBrowser.tryToResolveAndBrowse(presenceLibrary, 0);
        
        // Signal resolution and complete the browse 
        observerCollector.getLastMatch().resolved(resolvedAddress);
        
        // Signal browse failure
        browseListenerCollector.getLastMatch().browseFinished(false);
        
        // Signal browse success
        browseListenerCollector.getLastMatch().browseFinished(true);
        
        context.assertIsSatisfied();
    }
}