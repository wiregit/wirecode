package org.limewire.xmpp.client;

import org.limewire.xmpp.client.service.FileTransferProgressListener;
import org.limewire.xmpp.client.service.FileMetaData;

import com.google.inject.Provider;

public class ProgressListener implements FileTransferProgressListener, Provider<FileTransferProgressListener> {
    boolean started;
    boolean completed;
    boolean errored;
    
    public void started(FileMetaData file) {
        started = true;
    }

    public void completed(FileMetaData file) {
        completed = true;
    }

    public void updated(FileMetaData file, int percentComplete) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void errored(FileMetaData file) {
        errored = true;
    }

    public FileTransferProgressListener get() {
        return this;
    }
}
