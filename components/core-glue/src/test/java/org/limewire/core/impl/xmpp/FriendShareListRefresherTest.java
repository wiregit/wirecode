package org.limewire.core.impl.xmpp;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.browse.server.BrowseTracker;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifier;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifierFeature;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.impl.xmpp.FriendShareListRefresher.FinishedLoadingListener;
import org.limewire.core.impl.xmpp.FriendShareListRefresher.LibraryChangedSender;
import org.limewire.listener.ListenerSupport;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPService;

import ca.odell.glazedlists.EventList;

import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.ManagedFileList;
import com.limegroup.gnutella.library.ManagedListStatusEvent;

public class FriendShareListRefresherTest extends BaseTestCase {

    public FriendShareListRefresherTest(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public void testRegister() {
        Mockery context = new Mockery();

        final FileManager fileManager = context.mock(FileManager.class);
        final ManagedFileList managedFileList = context.mock(ManagedFileList.class);
        
        final ListenerSupport<FriendShareListEvent> listenerSupport
            = context.mock(ListenerSupport.class);
      
        final FriendShareListRefresher friendShareListRefresher = new FriendShareListRefresher(null, null, null);
        
        context.checking(new Expectations() {
            {
                exactly(1).of(listenerSupport).addListener(with(same(friendShareListRefresher)));
                
                allowing(fileManager).getManagedFileList();
                will(returnValue(managedFileList));
                
                exactly(1).of(managedFileList).addManagedListStatusListener(with(any(FinishedLoadingListener.class)));
                
            }});
        
        friendShareListRefresher.register(fileManager);
        friendShareListRefresher.register(listenerSupport);
        
        context.assertIsSatisfied();
    }

    /**
     * Confirm completion listeners are added and removed with respective friend share list events.
     */
    @SuppressWarnings("unchecked")
    public void testHandleEvent() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final FriendShareListEvent event1 = context.mock(FriendShareListEvent.class);
        final FriendShareListEvent event2 = context.mock(FriendShareListEvent.class);
        final Friend friend = context.mock(Friend.class);
        final LocalFileList localFileList = context.mock(LocalFileList.class);
        final EventList<LocalFileItem> eventList = context.mock(EventList.class);;
        
        final FriendShareListRefresher friendShareListRefresher = new FriendShareListRefresher(null, null, null);
        
        final Sequence resultSequence = context.sequence("resultSequence");
        
        context.checking(new Expectations() {
            {
                // These should be checked
                atLeast(1).of(event1).getType();
                will(returnValue(FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED));
                
                atLeast(1).of(event2).getType();
                will(returnValue(FriendShareListEvent.Type.FRIEND_SHARE_LIST_REMOVED));
                                
                atLeast(1).of(event1).getFriend();
                will(returnValue(friend));
                
                atLeast(1).of(event2).getFriend();
                will(returnValue(friend));
                
                allowing(friend).getId();
                will(returnValue("sadsadsa"));
                
                atLeast(1).of(event1).getFileList();
                will(returnValue(localFileList));
                
                atLeast(1).of(event2).getFileList();
                will(returnValue(localFileList));
                
                atLeast(1).of(localFileList).getModel();
                will(returnValue(eventList));
                
                // Assertions
                exactly(1).of(eventList).addListEventListener(with(any(LibraryChangedSender.class)));
                inSequence(resultSequence);
                
                exactly(1).of(eventList).removeListEventListener(with(any(LibraryChangedSender.class)));
                inSequence(resultSequence);
            }});
        
        
        friendShareListRefresher.handleEvent(event1);
        friendShareListRefresher.handleEvent(event2);
        
        context.assertIsSatisfied();

    }
    
    /**
     * Fire handleEvent() of the FinishedLoadingListener with SAVE and LOAD_FINISHING.
     * 
     * Confirm that nothing happens
     */
    public void testFinishedLoadingListenerOtherEvents() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final BrowseTracker tracker = context.mock(BrowseTracker.class);
        final XMPPService xmppService = context.mock(XMPPService.class);
        final ScheduledExecutorService scheduledExecutorService = context.mock(ScheduledExecutorService.class);
        final ManagedListStatusEvent event = context.mock(ManagedListStatusEvent.class);
        
        final FriendShareListRefresher friendShareListRefresher = new FriendShareListRefresher(tracker,
                xmppService, scheduledExecutorService);

        final FinishedLoadingListener finishedLoadingListener = friendShareListRefresher.new FinishedLoadingListener();
        
        context.checking(new Expectations() {
            {   exactly(1).of(event).getType();
                will(returnValue(ManagedListStatusEvent.Type.SAVE));
                exactly(1).of(event).getType();
                will(returnValue(ManagedListStatusEvent.Type.LOAD_FINISHING));
            }});
        
        finishedLoadingListener.handleEvent(event);
        finishedLoadingListener.handleEvent(event);
    }
    
    
    /**
     * Fire handleEvent() of the FinishedLoadingListener
     * 
     * Ensure that refreshes are sent to all friend and presences
     */
    @SuppressWarnings("unchecked")
    public void testFinishedLoadingListenerLoadComplete() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final BrowseTracker tracker = context.mock(BrowseTracker.class);
        final XMPPService xmppService = context.mock(XMPPService.class);
        final ScheduledExecutorService scheduledExecutorService = context.mock(ScheduledExecutorService.class);
        final ManagedListStatusEvent event = context.mock(ManagedListStatusEvent.class);
        
        final XMPPConnection xmppConnection = context.mock(XMPPConnection.class);
        
        final LinkedList<User> users = new LinkedList<User>();
        users.add(context.mock(User.class));
        users.add(context.mock(User.class));
        
        final Map<String, FriendPresence> presences1 = new HashMap<String, FriendPresence>();
        final Map<String, FriendPresence> presences2 = new HashMap<String, FriendPresence>();
                
        presences1.put("a", context.mock(FriendPresence.class));
        presences1.put("b", context.mock(FriendPresence.class));
        presences2.put("a", context.mock(FriendPresence.class));
        
        final Feature<LibraryChangedNotifier> featureA = context.mock(Feature.class);
        final Feature<LibraryChangedNotifier> featureB = context.mock(Feature.class);
        
        final LibraryChangedNotifier notifierA = context.mock(LibraryChangedNotifier.class);
        final LibraryChangedNotifier notifierB = context.mock(LibraryChangedNotifier.class); 
        
        final FriendShareListRefresher friendShareListRefresher = new FriendShareListRefresher(tracker,
                xmppService, scheduledExecutorService);

        final FinishedLoadingListener finishedLoadingListener = friendShareListRefresher.new FinishedLoadingListener();
        
        final String id1 = "1234";
        final String id2 = "5678";
        
        context.checking(new Expectations() {
            {   // These should be checked
                atLeast(1).of(event).getType();
                will(returnValue(ManagedListStatusEvent.Type.LOAD_COMPLETE));

                // Non critical actions
                allowing(xmppService).getActiveConnection();
                will(returnValue(xmppConnection));
                allowing(xmppConnection).getUsers();
                will(returnValue(users));
                allowing(users.get(0)).getId();
                will(returnValue(id1));
                allowing(users.get(1)).getId();
                will(returnValue(id2));
                allowing(users.get(0)).getFriendPresences();
                will(returnValue(presences1));
                allowing(users.get(1)).getFriendPresences();
                will(returnValue(presences2));
                allowing(presences1.get("a")).getFeature(LibraryChangedNotifierFeature.ID);
                will(returnValue(featureA));
                allowing(presences1.get("b")).getFeature(LibraryChangedNotifierFeature.ID);
                will(returnValue(featureB));
                allowing(presences2.get("a")).getFeature(LibraryChangedNotifierFeature.ID);
                will(returnValue(featureA));
                allowing(featureA).getFeature();
                will(returnValue(notifierA));
                allowing(featureB).getFeature();
                will(returnValue(notifierB));
                
                // Assertions
                exactly(1).of(tracker).sentRefresh(id1);
                exactly(1).of(tracker).sentRefresh(id2);
                exactly(2).of(notifierA).sendLibraryRefresh();
                exactly(1).of(notifierB).sendLibraryRefresh();
                
            }});
        
        finishedLoadingListener.handleEvent(event);
    }
    
}
