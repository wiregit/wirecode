package org.limewire.xmpp.client.impl.messages.library;

import java.io.IOException;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.client.LibraryChanged;
import org.limewire.core.api.friend.client.LibraryChangedEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.core.api.friend.client.FriendConnection;
import org.xmlpull.v1.XmlPullParserException;

public class LibraryChangedIQListener implements PacketListener {
    
    private static final Log LOG = LogFactory.getLog(LibraryChangedIQListener.class);

    private final EventBroadcaster<LibraryChangedEvent> libChangedBroadcaster;
    private final FriendConnection connection;

    public LibraryChangedIQListener(EventBroadcaster<LibraryChangedEvent> libChangedListeners,
                                    FriendConnection connection) {
        this.libChangedBroadcaster = libChangedListeners;
        this.connection = connection;
    }

    public void processPacket(Packet packet) {
        LibraryChangedIQ iq = (LibraryChangedIQ)packet;
        try {
            if(iq.getType().equals(IQ.Type.GET)) {
                //handleGet(iq);
            } else if(iq.getType().equals(IQ.Type.RESULT)) {
                //handleResult(iq);
            } else if(iq.getType().equals(IQ.Type.SET)) {
                LOG.debugf("received iq {0}", packet);
                handleSet(iq);
            } else if(iq.getType().equals(IQ.Type.ERROR)) {
                //handleError(iq);
            } else {
                //sendError(packet);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            //sendError(packet);
        } catch (XmlPullParserException e) {
            LOG.error(e.getMessage(), e);
            //sendError(packet);
        }
    }

    private void handleSet(LibraryChangedIQ packet) throws IOException, XmlPullParserException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("handling library changed set " + packet.getPacketID());
        }
        Friend user = connection.getUser(StringUtils.parseBareAddress(packet.getFrom()));
        if (user != null) {
            FriendPresence presence = user.getFriendPresences().get(packet.getFrom());
            if(presence != null) {
                libChangedBroadcaster.broadcast(new LibraryChangedEvent(presence, LibraryChanged.LIBRARY_CHANGED));
            }
        }
    }

    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof LibraryChangedIQ;
            }
        };
    }
}
