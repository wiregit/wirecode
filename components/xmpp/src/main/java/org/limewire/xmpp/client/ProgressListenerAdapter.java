package org.limewire.xmpp.client;

import org.jivesoftware.smackx.packet.StreamInitiation;

/**
 * An adapter between the xmpp component class <code>FileTransferProgressListener</code> and the
 * smack class <code>FileTransferProgressListener</code>
 */
class ProgressListenerAdapter implements org.jivesoftware.smackx.jingle.file.FileTransferProgressListener {
    private final org.limewire.xmpp.client.FileTransferProgressListener progressListener;

    public ProgressListenerAdapter(org.limewire.xmpp.client.FileTransferProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public void started(StreamInitiation.File file) {
        progressListener.started(new FileMetaDataAdapter(file));
    }

    public void completed(StreamInitiation.File file) {
        progressListener.completed(new FileMetaDataAdapter(file));
    }

    public void updated(StreamInitiation.File file, int percentComplete) {
        progressListener.updated(new FileMetaDataAdapter(file), percentComplete);
    }

    public void errored(StreamInitiation.File file) {
        progressListener.errored(new FileMetaDataAdapter(file));
    }
}
