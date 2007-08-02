package com.limegroup.gnutella.downloader;

import java.net.Socket;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.RemoteFileDesc;

public class SocketlessHTTPDownloaderFactory implements HTTPDownloaderFactory {
    
    private final NetworkManager networkManager;
    public SocketlessHTTPDownloaderFactory(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public HTTPDownloader create(Socket socket, RemoteFileDesc rfd,
            VerifyingFile incompleteFile, boolean inNetwork) {
        return new HTTPDownloader(socket, rfd, incompleteFile, inNetwork, false, networkManager);
    }

}
