package org.limewire.http.entity;

import org.limewire.nio.observer.Shutdownable;

public interface FileTransferMonitor extends Shutdownable {

    void addAmountUploaded(int written);

    void shutdown();
    
    void start();

    void stop();

}
