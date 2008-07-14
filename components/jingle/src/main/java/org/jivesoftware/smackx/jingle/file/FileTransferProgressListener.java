package org.jivesoftware.smackx.jingle.file;

import org.jivesoftware.smackx.packet.StreamInitiation;

public interface FileTransferProgressListener {
    public void started(StreamInitiation.File file);
    public void completed(StreamInitiation.File file);
    public void updated(StreamInitiation.File file, int percentComplete);
    public void errored(StreamInitiation.File file);
}
