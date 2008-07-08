package org.limewire.xmpp.client.impl.messages.filetransfer;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.xmpp.client.service.FileMetaData;

import java.io.IOException;

public class FileTransferIQ extends IQ {

    enum TransferType {OFFER, REQUEST}
    
    private static final Log LOG = LogFactory.getLog(FileTransferIQ.class);

    private FileMetaData fileMetaData;
    private TransferType transferType;
    
    public FileTransferIQ(XmlPullParser parser) {
        try {
            do {
                int eventType = parser.getEventType();
                if(eventType == XmlPullParser.START_TAG) {
                    if(parser.getName().equals("file-transfer")) {
                        transferType = TransferType.valueOf(parser.getAttributeValue(null, "type"));
                    }
                    else if(parser.getName().equals("file")) {
                        fileMetaData = new FileMetaDataImpl(parser);
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
                    if(parser.getName().equals("file-transfer")) {
                        return;
                    }
                }
            } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);   // TODO throw?
        } catch (XmlPullParserException e) {
            LOG.error(e.getMessage(), e);   // TODO throw?
        }
    }

    public FileMetaData getFileMetaData() {
        return fileMetaData;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public String getChildElementXML() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
    
    public static IQProvider getIQProvider() {
        return new FileTransferIQProvider();
    }

    private static class FileTransferIQProvider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {                     
            return new FileTransferIQ(parser);
        }
    }
}
