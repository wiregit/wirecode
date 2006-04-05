package com.limegroup.gnutella.downloader;

import java.io.IOException;

public interface DownloadStateObserver {

    public void handleStateFinished(ConnectionStatus status);
    public void handleStateException(IOException iox);
    public void handleStateShutdown();
    
}
