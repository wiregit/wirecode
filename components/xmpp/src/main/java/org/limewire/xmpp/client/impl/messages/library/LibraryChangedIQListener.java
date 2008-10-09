package org.limewire.xmpp.client.impl.messages.library;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.limewire.listener.EventListener;
import org.limewire.xmpp.api.client.LibraryChanged;
import org.limewire.xmpp.api.client.LibraryChangedEvent;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.client.impl.UserImpl;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQ;
import org.xmlpull.v1.XmlPullParserException;

public class LibraryChangedIQListener implements PacketListener {
    private static final Log LOG = LogFactory.getLog(LibraryChangedIQListener.class);

    private final EventListener<LibraryChangedEvent> libChangedListeners;
    private final Map<String, UserImpl> users;

    public LibraryChangedIQListener(EventListener<LibraryChangedEvent> libChangedListeners,
                                    Map<String, UserImpl> users) {
        this.libChangedListeners = libChangedListeners;
        this.users = users;
    }

    public void processPacket(Packet packet) {
        FileTransferIQ iq = (FileTransferIQ)packet;
        try {
            if(iq.getType().equals(IQ.Type.GET)) {
                //handleGet(iq);
            } else if(iq.getType().equals(IQ.Type.RESULT)) {
                //handleResult(iq);
            } else if(iq.getType().equals(IQ.Type.SET)) {
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

    private void handleSet(FileTransferIQ packet) throws IOException, XmlPullParserException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("handling library changed set " + packet.getPacketID());
        }
        String userID = StringUtils.parseBareAddress(packet.getFrom());
        UserImpl user;
        synchronized (users) {
            user = users.get(userID);
        }
        if (user != null) {
            Presence presence = user.getPresences().get(packet.getFrom());
            if(presence != null && presence instanceof LimePresence) {
                libChangedListeners.handleEvent(new LibraryChangedEvent((LimePresence)presence, LibraryChanged.LIBRARY_CHANGED));
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
