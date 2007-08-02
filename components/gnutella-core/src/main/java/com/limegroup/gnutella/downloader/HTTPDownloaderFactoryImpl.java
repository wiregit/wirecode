package com.limegroup.gnutella.downloader;

import java.net.Socket;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.RemoteFileDesc;

public class HTTPDownloaderFactoryImpl implements HTTPDownloaderFactory {

    private final NetworkManager networkManager;

    public HTTPDownloaderFactoryImpl(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.HTTPDownloaderFactory#create(java.net.Socket, com.limegroup.gnutella.RemoteFileDesc, com.limegroup.gnutella.downloader.VerifyingFile, boolean)
     */
    public HTTPDownloader create(Socket socket, RemoteFileDesc rfd,
            VerifyingFile incompleteFile, boolean inNetwork) {
        return new HTTPDownloader(socket, rfd, incompleteFile, inNetwork,
                true, networkManager);
    }
}
