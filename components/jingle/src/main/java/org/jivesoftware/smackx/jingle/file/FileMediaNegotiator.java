package org.jivesoftware.smackx.jingle.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.media.MediaNegotiator;
import org.jivesoftware.smackx.packet.Content;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.JingleError;
import org.jivesoftware.smackx.packet.StreamInitiation;
import org.jivesoftware.smackx.packet.file.FileDescription;
import org.apache.log4j.Logger;

public class FileMediaNegotiator extends MediaNegotiator {   

    private static final Logger LOG = Logger.getLogger(FileMediaNegotiator.class);

    private StreamInitiation.File file;
    private boolean sending;
    private boolean userAccepted;

    public FileMediaNegotiator(JingleSession js, File file, boolean sending) {
        super(js);
        this.file = getFile(file);
        this.sending = sending;
        userAccepted = false;
        inviting = new InvitingImpl(this);
        accepting = new AcceptingImpl(this);
        active = new ActiveImpl(this);
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

        public Jingle eventInvite() {
            // SEND SESSION-INITIATE
            FileDescription description = new FileDescription();
            FileDescription.FileContainer container;
            if(sending) {
                container = new FileDescription.Offer(file);
            } else {
                container = new FileDescription.Request(file);
            }
            description.setFileContainer(container);
            userAccepted = true;
            return new Jingle(new Content(description));
        }
    }
    
    public class AcceptingImpl extends Accepting {

        public AcceptingImpl(MediaNegotiator neg) {
            super(neg);
        }

        public Jingle eventInitiate(Jingle jin) {
            // RECEIVE SESSION_INITIATE
            // TODO make async
            if(userAccepts(jin)) {
                userAccepted = true;
                setState(active);
            } else {
                try {
                    session.terminate("no thx");
                } catch (XMPPException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        private boolean userAccepts(Jingle jin) {
            // TODO ask user if they want to accept the offer/request
            return true;
        }
    }
    
    public class ActiveImpl extends Active {

        public ActiveImpl(MediaNegotiator neg) {
            super(neg);
        }

        /**
         * We have an agreement.
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventEnter()
         */
        public void eventEnter() {
            triggerMediaEstablished(getFileDescription());
            super.eventEnter();
        }

        /**
         * We are breaking the contract...
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventExit()
         */
        public void eventExit() {
            triggerMediaClosed(getFileDescription());
            super.eventExit();
        }
        
        // TODO is this necessary?
        public void eventError(IQ iq) throws XMPPException {
            triggerMediaClosed(getFileDescription());
            super.eventError(iq);
        }

        private FileDescription getFileDescription() {
            FileDescription description = new FileDescription();
            FileDescription.FileContainer container;
            if(sending) {
                // TODO this is incorrect - need to take into account if we
                // TODO are initiator or receiver
                container = new FileDescription.Offer(file);
            } else {
                container = new FileDescription.Request(file);
            }
            description.setFileContainer(container);
            return description;
        }
    }

    public void addDescriptionToSessionInitiate(Jingle jingle) {
        FileDescription description = new FileDescription();
        FileDescription.FileContainer container;
        if(sending) {
            container = new FileDescription.Offer(file);
        } else {
            container = new FileDescription.Request(file);
        }
        description.setFileContainer(container);
        jingle.getContent().addDescription(description);
    }

    public void addDescriptionToContentAccept(Jingle jin, Jingle jout) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isEstablished() {
        return userAccepted;
    }

    public void addAcceptedDescription(Content content) {
        FileDescription description = new FileDescription();
        FileDescription.FileContainer container;
        if(sending) {
            // TODO this is wrong
            container = new FileDescription.Offer(file);
        } else {
            container = new FileDescription.Request(file);
        }
        description.setFileContainer(container);
        content.addDescription(description);
    }

    public StreamInitiation.File getFile() {
        return file;
    }

    public boolean isSending() {
        return sending;
    }
}
