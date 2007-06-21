package org.limewire.http.entity;

public interface FileTransferMonitor {

    void start();

    void addAmountUploaded(int written);

    void stop();

}
