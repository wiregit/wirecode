package org.limewire.xmpp.client;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.OutgoingJingleSession;
import org.jivesoftware.smackx.jingle.file.FileContentHandler;

public class LimeUser extends User{
    private Library library;

    public LimeUser(String jid, String name, Presence presence, XMPPConnection connection) {
        super(jid, name, presence, connection);
    }
    
    public Library getLibrary() {
        return library;
    }
    
    void setLibrary(Library library) {
        this.library = library;
    }
    
    public void sendFile(java.io.File file) {
        JingleManager manager = new JingleManager(connection);

        try {
            FileContentHandler fileContentHandler = new FileContentHandler(file, true);
            OutgoingJingleSession out = manager.createOutgoingJingleSession(id, fileContentHandler);

            out.start();

            while (out.getJingleMediaSession() == null) {
                Thread.sleep(500);
            }

            //out.terminate();
        } catch (XMPPException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
