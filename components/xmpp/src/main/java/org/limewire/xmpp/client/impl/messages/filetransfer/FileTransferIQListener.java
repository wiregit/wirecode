package org.limewire.xmpp.client.impl.messages.filetransfer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.limewire.xmpp.client.service.FileOfferHandler;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class FileTransferIQListener implements PacketListener {
    private static final Log LOG = LogFactory.getLog(FileTransferIQListener.class);

    private final FileOfferHandler fileOfferHandler;

    public FileTransferIQListener(FileOfferHandler fileOfferHandler) {
        this.fileOfferHandler = fileOfferHandler;
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
        // TODO async?
        fileOfferHandler.fileOfferred(packet.getFileMetaData());
        // TODO send acceptance or rejection;
        // TODO only needed for user feedback
    }

    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof FileTransferIQ;
            }
        };
    }
}
