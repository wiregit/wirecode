package com.limegroup.gnutella.downloader;

import java.net.Socket;

import com.limegroup.gnutella.RemoteFileDesc;

public interface HTTPDownloaderFactory {

    /**
     * Constructs an HTTPDownloader with the given socket. If the socket was a
     * PUSH socket, the GIV must have already been read off it.
     * 
     * You must call initializeTCP prior to connectHTTP.
     * 
     * @param socket the socket to download from.
     * @param rfd complete information for the file to download, including host
     *        address and port
     * @param incompleteFile the temp file to use while downloading, which need
     *        not exist.
     * @param inNetwork true if this is for an in-network downloader.
     */
    public HTTPDownloader create(Socket socket, RemoteFileDesc rfd,
            VerifyingFile incompleteFile, boolean inNetwork);

}