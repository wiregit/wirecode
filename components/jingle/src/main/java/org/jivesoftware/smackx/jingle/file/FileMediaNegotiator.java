package org.jivesoftware.smackx.jingle.file;

import java.io.File;
import java.util.Date;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.media.MediaNegotiator;
import org.jivesoftware.smackx.packet.Content;
import org.jivesoftware.smackx.packet.file.FileDescription;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.StreamInitiation;

public class FileMediaNegotiator extends MediaNegotiator {

    private StreamInitiation.File file;
    private boolean sending;

    public FileMediaNegotiator(JingleSession js, File file, boolean sending) {
        super(js);
        this.file = getFile(file);
        this.sending = sending;
        inviting = new InvitingImpl(this);
    }

    private StreamInitiation.File getFile(File file) {
        StreamInitiation.File siFile = new StreamInitiation.File(file.getName(), file.length());
        siFile.setDate(new Date(file.lastModified()));
        siFile.setDesc(null); // TODO
        siFile.setHash(null); // TODO
        siFile.setRanged(false); // TODO add range support to StreamInitiation.File
        return siFile;
    }
    
    public class InvitingImpl extends Inviting {

        public InvitingImpl(MediaNegotiator neg) {
            super(neg);
        }

        /**
         * Create an initial Jingle packet, with the list of payload types that
         * we support. The list is in order of preference.
         */
        public Jingle eventInvite() {
            FileDescription description = new FileDescription();
            FileDescription.FileContainer container;
            if(sending) {
                container = new FileDescription.Offer(file);
            } else {
                container = new FileDescription.Request(file);
            }
            description.setFileContainer(container);
        
            Jingle jingle = new Jingle(new Content(description));
            jingle.setAction(Jingle.Action.DESCRIPTIONINFO);
            jingle.setType(IQ.Type.SET);
            return jingle;
        }
    }

    public void addDescriptionToSessionInitiate(Jingle jingle) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addDescriptionToContentAccept(Jingle jin, Jingle jout) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isEstablished() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addAcceptedDescription(Content content) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public StreamInitiation.File getFile() {
        return file;
    }

    public boolean isSending() {
        return sending;
    }
}
