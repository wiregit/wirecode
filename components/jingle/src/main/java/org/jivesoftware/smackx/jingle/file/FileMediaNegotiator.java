package org.jivesoftware.smackx.jingle.file;

import java.io.File;
import java.util.Date;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.media.MediaNegotiator;
import org.jivesoftware.smackx.packet.Content;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.StreamInitiation;
import org.jivesoftware.smackx.packet.file.FileDescription;

public class FileMediaNegotiator extends MediaNegotiator {   

    private static final Logger LOG = Logger.getLogger(FileMediaNegotiator.class);

    private FileDescription.FileContainer file;
    private boolean userAccepted;
    private UserAcceptor userAcceptor;

    public FileMediaNegotiator(JingleSession js, FileDescription.FileContainer file, UserAcceptor userAcceptor) {
        super(js);
        this.userAcceptor = userAcceptor;
        this.file = file;
        userAccepted = false;
        inviting = new InvitingImpl(this);
        accepting = new AcceptingImpl(this);
        active = new ActiveImpl(this);
    }
    
    public class InvitingImpl extends Inviting {

        public InvitingImpl(MediaNegotiator neg) {
            super(neg);
        }

        public Jingle eventInvite() {
            // SEND SESSION-INITIATE
            FileDescription description = new FileDescription();            
            description.setFileContainer(file);
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
            return userAcceptor.userAccepts(((FileDescription)jin.getContent().getDescriptions().get(0)).getFileContainer());
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
            description.setFileContainer(file);
            return description;
        }
    }

    public void addDescriptionToSessionInitiate(Jingle jingle) {
        FileDescription description = new FileDescription();
        description.setFileContainer(file);
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
        description.setFileContainer(file);
        content.addDescription(description);
    }

    public FileDescription.FileContainer getFile() {
        return file;
    }
}
