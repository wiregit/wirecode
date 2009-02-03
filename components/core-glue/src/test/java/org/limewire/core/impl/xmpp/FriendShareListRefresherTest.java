package org.limewire.core.impl.xmpp;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.jmock.Mockery;
import org.limewire.core.api.browse.server.BrowseTracker;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.api.client.XMPPService;

import com.limegroup.gnutella.library.FileManager;

public class FriendShareListRefresherTest extends BaseTestCase {

    public FriendShareListRefresherTest(String name) {
        super(name);
    }

    public void testBasics() {
        Mockery context = new Mockery();

        final BrowseTracker tracker = context.mock(BrowseTracker.class);
        final XMPPService xmppService = context.mock(XMPPService.class);
        final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);

        final FileManager fileManager = context.mock(FileManager.class);
        final TestListenerSupport<FriendShareListEvent> testListenerSupport = new TestListenerSupport<FriendShareListEvent>();
//        FriendShareListRefresher friendShareListRefresher = new FriendShareListRefresher(tracker,
//                xmppService, scheduledExecutorService);
//        friendShareListRefresher.register(fileManager);
//        friendShareListRefresher.register(testListenerSupport);
    }

    private class TestListenerSupport<T> implements ListenerSupport<T> {
        private final CopyOnWriteArrayList<EventListener<T>> listenerList;

        public TestListenerSupport() {
            listenerList = new CopyOnWriteArrayList<EventListener<T>>();
        }

        @Override
        public void addListener(EventListener<T> listener) {
            listenerList.add(listener);
        }

        @Override
        public boolean removeListener(EventListener<T> listener) {
            return listenerList.remove(listener);
        }

        public void fireEvent(T event) {
            for (EventListener<T> listener : listenerList) {
                listener.handleEvent(event);
            }
        }
    }

}
