package org.limewire.xmpp.client;

public interface FileTransferProgressListener {
    public void started(File file);
    public void completed(File file);
    public void updated(File file, int percentComplete);
    public void errored(File file);
}
