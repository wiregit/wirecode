package org.limewire.http.entity;

public interface FileTransfer {

    void start();

    void addAmountUploaded(int written);

    void stop();

}
