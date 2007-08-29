package org.limewire.http.entity;

import java.io.IOException;

import org.apache.http.protocol.HttpContext;
import org.limewire.http.HttpIOReactor;
import org.limewire.http.HttpIOSession;

public class BasicFileTransferMonitor implements FileTransferMonitor {

    private final HttpIOSession ioSession;

    public BasicFileTransferMonitor(HttpContext context) {
        if (context != null) {
            this.ioSession = (HttpIOSession) context.getAttribute(HttpIOReactor.IO_SESSION_KEY);
        } else {
            this.ioSession = null;
        }
    }
    
    public void addAmountUploaded(int written) {
    }

    public void start() {
    }

    public void failed(IOException e) {
        shutdown();
    }

    public void timeout() {
        shutdown();
    }

    private void shutdown() {
        if (this.ioSession != null) {
            this.ioSession.shutdown();
        }
    }    

}