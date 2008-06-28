package org.limewire.xmpp.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.packet.StreamInitiation;

/**
 * An adapter between the xmpp component class <code>FileTransferProgressListener</code> and the
 * smack class <code>FileTransferProgressListener</code>
 */
class ProgressListenerAdapter implements org.jivesoftware.smackx.jingle.file.FileTransferProgressListener {

    private final Log LOG = LogFactory.getLog(ProgressListenerAdapter.class);

    private final org.limewire.xmpp.client.FileTransferProgressListener progressListener;
    private final JingleSession session;

    public ProgressListenerAdapter(org.limewire.xmpp.client.FileTransferProgressListener progressListener, JingleSession session) {
        this.progressListener = progressListener;
        this.session = session;
    }

    public void started(StreamInitiation.File file) {
        progressListener.started(new FileMetaDataAdapter(file));
    }

    public void completed(StreamInitiation.File file) {
        progressListener.completed(new FileMetaDataAdapter(file));
        try {
            session.terminate();
        } catch (org.jivesoftware.smack.XMPPException e) {
            LOG.debug(e.getMessage(), e);
        }
    }

    public void updated(StreamInitiation.File file, int percentComplete) {
        progressListener.updated(new FileMetaDataAdapter(file), percentComplete);
    }

    public void errored(StreamInitiation.File file) {
        progressListener.errored(new FileMetaDataAdapter(file));
        try {
            session.terminate();
        } catch (org.jivesoftware.smack.XMPPException e) {
            LOG.debug(e.getMessage(), e);
        }
    }
}
