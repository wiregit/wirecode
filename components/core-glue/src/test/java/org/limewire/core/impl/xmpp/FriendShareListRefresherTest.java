package org.limewire.core.impl.xmpp;

import java.util.concurrent.ScheduledExecutorService;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.browse.server.BrowseTracker;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.impl.xmpp.FriendShareListRefresher.FinishedLoadingListener;
import org.limewire.core.impl.xmpp.FriendShareListRefresher.LibraryChangedSender;
import org.limewire.listener.ListenerSupport;
import org.limewire.util.BaseTestCase;
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
    
}
