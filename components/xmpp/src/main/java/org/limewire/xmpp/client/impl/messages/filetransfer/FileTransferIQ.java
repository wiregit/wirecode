package org.limewire.xmpp.client.impl.messages.filetransfer;

import java.io.IOException;
import java.util.Arrays;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.limewire.xmpp.client.impl.messages.FileMetaDataImpl;
import org.limewire.xmpp.client.impl.messages.HostMetaDataImpl;
import org.limewire.xmpp.client.service.FileMetaData;
import org.limewire.xmpp.client.service.FileTransferMetaData;
import org.limewire.xmpp.client.service.HostMetaData;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class FileTransferIQ extends IQ {

    public enum TransferType {OFFER, REQUEST}

    private FileMetaData fileMetaData;
    private HostMetaData hostMetaData;
    private TransferType transferType;
    
    public FileTransferIQ(XmlPullParser parser) throws IOException, XmlPullParserException {
        do {
            int eventType = parser.getEventType();
            if(eventType == XmlPullParser.START_TAG) {
                if(parser.getName().equals("file-transfer")) {
                    transferType = TransferType.valueOf(parser.getAttributeValue(null, "type"));
                } else if(parser.getName().equals("file")) {
                    fileMetaData = new FileMetaDataImpl(parser);
                } else if(parser.getName().equals("host")) {
                    hostMetaData = new HostMetaDataImpl(parser);
                }
            } else if(eventType == XmlPullParser.END_TAG) {
                if(parser.getName().equals("file-transfer")) {
                    return;
                }
            }
        } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
    }
    
    public FileTransferIQ(FileTransferMetaData fileTransferMetaData, TransferType transferType) {
        this(fileTransferMetaData.getFileMetaData(), fileTransferMetaData.getHostMetaData(), transferType);
    }
    
    public FileTransferIQ(FileMetaData fileMetaData, HostMetaData hostMetaData, TransferType transferType) {
        this.fileMetaData = fileMetaData;
        this.hostMetaData = hostMetaData;
        this.transferType = transferType;
    }
    
    public FileTransferIQ(FileMetaData fileMetaData, TransferType transferType) {
        this(fileMetaData, null, transferType);
    }

    public FileMetaData getFileMetaData() {
        return fileMetaData;
    }
    
    public HostMetaData getHostMetaData() {
        return hostMetaData;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public String getChildElementXML() {
        String fileTransfer = "<file-transfer xmlns='jabber:iq:lw-file-transfer' type='" + transferType.toString() + "'>";
        if(fileMetaData != null) {
            fileTransfer += fileMetaData.toXML();
        }
        if(hostMetaData != null) {
            fileTransfer += hostMetaData.toXML();
        }
        fileTransfer += "</file-transfer>";
        return fileTransfer;
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
