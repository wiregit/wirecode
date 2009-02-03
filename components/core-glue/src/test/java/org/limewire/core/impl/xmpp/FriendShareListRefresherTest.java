package org.limewire.core.impl.xmpp;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.browse.server.BrowseTracker;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.impl.xmpp.FriendShareListRefresher.FinishedLoadingListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.api.client.XMPPService;

import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.ManagedFileList;

public class FriendShareListRefresherTest extends BaseTestCase {

    public FriendShareListRefresherTest(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public void testRegister() {
        Mockery context = new Mockery();

        final BrowseTracker tracker = context.mock(BrowseTracker.class);
        final XMPPService xmppService = context.mock(XMPPService.class);
        final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
        final FileManager fileManager = context.mock(FileManager.class);
        final ManagedFileList managedFileList = context.mock(ManagedFileList.class);
        
        final ListenerSupport<FriendShareListEvent> listenerSupport
            = context.mock(ListenerSupport.class);
      
        final FriendShareListRefresher friendShareListRefresher = new FriendShareListRefresher(tracker,
                xmppService, scheduledExecutorService);
        
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

 
}
