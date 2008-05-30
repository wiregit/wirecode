package org.limewire.xmpp.client;

import java.util.ArrayList;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.OutgoingJingleSession;
import org.jivesoftware.smackx.jingle.nat.ICETransportManager;

public class LibraryListener implements PacketListener {
    private XMPPConnection connection;
    private RemoteFile [] files;

    public LibraryListener(XMPPConnection connection, RemoteFile [] files) {
        this.files = files;
        this.connection = connection;
    }
    
    public void setFiles(RemoteFile [] files) {
        this.files = files;
    }

    public void setConnection(XMPPConnection connection) {
        this.connection = connection;
    }

    public void processPacket(Packet packet) {
        Library iq = (Library)packet;
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
    }

    private void handleResult(Library library) {
        for(RemoteFile file : library.getAllSharedFileDescriptors()){
            System.out.println("file " + file.getName());
            
        }
    }

    private void handleGet(Library packet) {
        IQ queryResult = new Library(files);
        queryResult.setTo(packet.getFrom());
        queryResult.setFrom(packet.getTo());
        queryResult.setPacketID(packet.getPacketID());
        queryResult.setType(IQ.Type.RESULT);
        connection.sendPacket(queryResult);
    }

    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof Library;
            }
        };
    }
    
    private void jingleOUT(String to) throws InterruptedException {
        JingleManager manager = new JingleManager(connection);

        try {
//            MultiMediaManager mediaManager = new MultiMediaManager();
//            mediaManager.addMediaManager(new JmfMediaManager());
//            mediaManager.addMediaManager(new ScreenShareMediaManager());
//            mediaManager.addMediaManager(new SpeexMediaManager());
//
//            manager.setMediaManager(mediaManager);
            
            OutgoingJingleSession out = manager.createOutgoingJingleSession(to, null);

            out.start();

            while (out.getJingleMediaSession() == null) {
                Thread.sleep(500);
            }

            //out.terminate();
        } catch (XMPPException e) {
            e.printStackTrace();
        }
    }
}
