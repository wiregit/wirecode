package org.limewire.xmpp.client.impl.messages.library;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.jivesoftware.smack.packet.IQ.Type;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.LibraryChanged;
import org.limewire.friend.api.LibraryChangedEvent;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.api.feature.LibraryChangedNotifier;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.client.impl.XMPPFriendConnectionImpl;

public class LibraryChangedIQListenerTest extends BaseTestCase {

    private Mockery context;
    private FeatureTransport.Handler<LibraryChangedNotifier> handler;
    private XMPPFriendConnectionImpl connection;
    private LibraryChangedIQListener libraryChangedIQListener;

    public LibraryChangedIQListenerTest(String name) {
        super(name);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        handler = context.mock(FeatureTransport.Handler.class);
        connection = context.mock(XMPPFriendConnectionImpl.class);
        libraryChangedIQListener = new LibraryChangedIQListener(handler, connection); 
    }
    
    public void testProcessFiresLibraryChangedEvent() {
        LibraryChangedIQ libraryChangedIQ = new LibraryChangedIQ();
        libraryChangedIQ.setType(Type.SET);
        libraryChangedIQ.setFrom("me@you.com/ldkfjd");
        
        final Friend user = context.mock(Friend.class);
        final FriendPresence friendPresence = context.mock(FriendPresence.class);

        final AtomicReference<LibraryChangedEvent> event = new AtomicReference<LibraryChangedEvent>();
        context.checking(new Expectations() {{
            one(connection).getFriend("me@you.com");
            will(returnValue(user));
            one(user).getPresences();
            will(returnValue(Collections.singletonMap("me@you.com/ldkfjd", friendPresence)));
            one(handler).featureReceived("me@you.com/ldkfjd", with(any(LibraryChangedNotifier.class)));
            will(new AssignParameterAction<LibraryChangedEvent>(event, 0));
        }});
        
        libraryChangedIQListener.processPacket(libraryChangedIQ);
        
        assertSame(friendPresence, event.get().getData());
        assertEquals(LibraryChanged.LIBRARY_CHANGED, event.get().getType());
        
        context.assertIsSatisfied();
    }
}
