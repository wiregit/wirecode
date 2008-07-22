package org.limewire.xmpp.client.impl;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.file.FileLocator;
import org.limewire.net.address.Address;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQ;
import org.limewire.xmpp.client.impl.messages.library.LibraryIQ;
import org.limewire.xmpp.client.impl.messages.library.LibraryIQListener;
import org.limewire.xmpp.client.impl.messages.address.AddressIQ;
import org.limewire.xmpp.client.service.FileMetaData;
import org.limewire.xmpp.client.service.FileTransferMetaData;
import org.limewire.xmpp.client.service.FileTransferProgressListener;
import org.limewire.xmpp.client.service.HostMetaData;
import org.limewire.xmpp.client.service.LibraryListener;
import org.limewire.xmpp.client.service.LimePresence;
import org.xmlpull.v1.XmlPullParserException;

public class LimePresenceImpl extends PresenceImpl implements LimePresence {

    private static final Log LOG = LogFactory.getLog(LimePresenceImpl.class);
    
    private LibraryListener libraryListener;
    protected LibraryIQListener IQListener;
    private final FileLocator fileLocator;
    private Address address;

    LimePresenceImpl(org.jivesoftware.smack.packet.Presence presence, XMPPConnection connection, LibraryIQListener libraryIQListener, FileLocator fileLocator) {
        super(presence, connection);
        this.IQListener = libraryIQListener;
        this.fileLocator = fileLocator;
    }
    
    public Address getAddress() {
        return address;
    }
    
    void sendGetLibrary() {
        if(LOG.isInfoEnabled()) {
            LOG.info("getting library from " + getJID() + "...");
        }
        final LibraryIQ libraryIQ = new LibraryIQ();
        libraryIQ.setType(IQ.Type.GET);
        libraryIQ.setTo(getJID());
        libraryIQ.setPacketID(IQ.nextID());
        IQListener.addLibraryListener(libraryIQ, libraryListener);
        //final PacketCollector collector = connection.createPacketCollector(
        //    new PacketIDFilter(libraryIQ.getPacketID()));         
        //Thread responseThread = new Thread(new Runnable() {
        //    public void run() {
                connection.sendPacket(libraryIQ);
                //LibraryIQ response = (LibraryIQ) collector.nextResult();
                //collector.cancel();
                //response.parseFiles(libraryListener);
        //    }
        //});
        //responseThread.setDaemon(true);
        //responseThread.start();
    }
    
    void sendGetAddress() {
        if(LOG.isInfoEnabled()) {
            LOG.info("getting address from " + getJID() + "...");
        }
        final AddressIQ addressIQ = new AddressIQ();
        addressIQ.setType(IQ.Type.GET);
        addressIQ.setTo(getJID());
        addressIQ.setPacketID(IQ.nextID());
        final PacketCollector collector = connection.createPacketCollector(
            new PacketIDFilter(addressIQ.getPacketID()));         
        connection.sendPacket(addressIQ);
        final AddressIQ response = (AddressIQ) collector.nextResult();
        address = response.getAddress();
        collector.cancel();
    }
    
    public void setLibraryListener(LibraryListener libraryListener) {
        this.libraryListener = libraryListener;
        // TODO fire exiting library
    }

    public FileTransferMetaData requestFile(FileMetaData file, FileTransferProgressListener progressListener) throws IOException, XmlPullParserException {
        if(LOG.isInfoEnabled()) {
            LOG.info("requesting file " + file.toString() + " from " + getJID());
        }
       
        final FileTransferIQ transferIQ = new FileTransferIQ(file, FileTransferIQ.TransferType.REQUEST);
        transferIQ.setType(IQ.Type.GET);
        transferIQ.setTo(getJID());
        transferIQ.setPacketID(IQ.nextID());
        final PacketCollector collector = connection.createPacketCollector(
            new PacketIDFilter(transferIQ.getPacketID()));         
        connection.sendPacket(transferIQ);
        final FileTransferIQ response = (FileTransferIQ) collector.nextResult();
        collector.cancel();
        return new FileTransferMetaData() {
            public FileMetaData getFileMetaData() {
                return response.getFileMetaData();
            }

            public HostMetaData getHostMetaData() {
                return response.getHostMetaData();
            }
        };
    }

    public void sendFile(FileTransferMetaData file, FileTransferProgressListener progressListener) {
        if(LOG.isInfoEnabled()) {
            LOG.info("sending file " + file.toString() + " to " + getJID());
        }
        final FileTransferIQ transferIQ = new FileTransferIQ(file, FileTransferIQ.TransferType.OFFER);
        transferIQ.setType(IQ.Type.GET);
        transferIQ.setTo(getJID());
        transferIQ.setPacketID(IQ.nextID());
        connection.sendPacket(transferIQ);
    }
    
    
}
