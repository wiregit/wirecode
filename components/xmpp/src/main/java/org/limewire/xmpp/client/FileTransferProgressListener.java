package org.limewire.xmpp.client;

/**
 * Used to track progress in a jingle file exchange
 */
public interface FileTransferProgressListener {
    public void started(FileMetaData file);
    public void completed(FileMetaData file);
    public void updated(FileMetaData file, int percentComplete);
    public void errored(FileMetaData file);
}
