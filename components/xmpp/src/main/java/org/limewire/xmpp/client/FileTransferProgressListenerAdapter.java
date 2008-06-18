package org.limewire.xmpp.client;

import org.jivesoftware.smackx.packet.StreamInitiation;

public class FileTransferProgressListenerAdapter implements org.jivesoftware.smackx.jingle.file.FileTransferProgressListener {
        private final org.limewire.xmpp.client.FileTransferProgressListener progressListener;

        public FileTransferProgressListenerAdapter(org.limewire.xmpp.client.FileTransferProgressListener progressListener) {
            this.progressListener = progressListener;
        }

        public void started(StreamInitiation.File file) {
            progressListener.started(new File(file.getName(), file.getHash()));
        }

        public void completed(StreamInitiation.File file) {
            progressListener.completed(new File(file.getName(), file.getHash()));
        }

        public void updated(StreamInitiation.File file, int percentComplete) {
            progressListener.updated(new File(file.getName(), file.getHash()), percentComplete);
        }

        public void errored(StreamInitiation.File file) {
            progressListener.errored(new File(file.getName(), file.getHash()));
        }
    }
