package com.limegroup.gnutella.uploader;

import java.io.File;

public interface FileTransfer {

    long getUploadBegin();

    long getUploadEnd();

    File getFile();

    void activateThrottle();

    void addAmountUploaded(int written);

    void stop();

}
