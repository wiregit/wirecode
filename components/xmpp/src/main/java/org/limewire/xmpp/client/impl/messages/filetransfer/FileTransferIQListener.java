package org.limewire.xmpp.client.impl.messages.filetransfer;

import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.IQ;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.xmpp.client.service.LibraryProvider;
import org.limewire.xmpp.client.service.LibraryListener;
import org.xmlpull.v1.XmlPullParserException;

public class FileTransferIQListener implements PacketListener {
    private static final Log LOG = LogFactory.getLog(FileTransferIQListener.class);

    private XMPPConnection connection;
    private final LibraryProvider libraryProvider;

    public FileTransferIQListener(XMPPConnection connection, LibraryProvider libraryProvider) {
        this.libraryProvider = libraryProvider;
        this.connection = connection;
    }

    public void processPacket(Packet packet) {
        FileTransferIQ iq = (FileTransferIQ)packet;
        try {
            if(iq.getType().equals(IQ.Type.GET)) {
                handleGet(iq);
            } else if(iq.getType().equals(IQ.Type.RESULT)) {
                handleResult(iq);
            } else if(iq.getType().equals(IQ.Type.SET)) {
                //handleSet(iq);
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

    private void handleResult(FileTransferIQ libraryIQ) {
        
    }

    private void handleGet(FileTransferIQ packet) throws IOException, XmlPullParserException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("handling file transfer get " + packet.getPacketID());
        }
        FileTransferIQ queryResult = new FileTransferIQ(libraryProvider.readFile(packet.getFileMetaData()), packet.getTransferType());
        queryResult.setTo(packet.getFrom());
        queryResult.setFrom(packet.getTo());
        queryResult.setPacketID(packet.getPacketID());
        queryResult.setType(IQ.Type.RESULT);
        connection.sendPacket(queryResult);
    }

    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof FileTransferIQ;
            }
        };
    }
}
