package org.limewire.core.impl.xmpp;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.impl.xmpp.FriendShareListRefresher.FinishedLoadingListener;
import org.limewire.core.impl.xmpp.FriendShareListRefresher.LibraryChangedSender;
import org.limewire.listener.ListenerSupport;
import org.limewire.util.BaseTestCase;

import ca.odell.glazedlists.EventList;

import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.ManagedFileList;

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

    @SuppressWarnings("unchecked")
    public void testHandleEvent() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final FriendShareListEvent event = context.mock(FriendShareListEvent.class);
        final Friend friend = context.mock(Friend.class);
        final LocalFileList localFileList = context.mock(LocalFileList.class);
        final EventList<LocalFileItem> eventList = context.mock(EventList.class);;
        
        final FriendShareListRefresher friendShareListRefresher = new FriendShareListRefresher(null, null, null);
        
        context.checking(new Expectations() {
            {
                allowing(event).getType();
                will(returnValue(FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED));
                
                atLeast(1).of(event).getFriend();
                will(returnValue(friend));
                
                allowing(friend).getId();
                will(returnValue("sadsadsa"));
                
                atLeast(1).of(event).getFileList();
                will(returnValue(localFileList));
                
                atLeast(1).of(localFileList).getModel();
                will(returnValue(eventList));
                
                exactly(1).of(eventList).addListEventListener(with(any(LibraryChangedSender.class)));
            }});
        
        
        friendShareListRefresher.handleEvent(event); 
        
        context.assertIsSatisfied();

    }
    
}
