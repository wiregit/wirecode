package com.limegroup.gnutella.stubs;

import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.util.URLDecoder;


public class DownloadManagerStub extends DownloadManager {
    public void initialize(
        ActivityCallback callback, MessageRouter router,
        Acceptor acceptor, FileManager fileManager) { }
    public void postGuiInit(RouterService backend) { }
    public synchronized int downloadsInProgress() { return 0; }
    public synchronized boolean readSnapshot(File file) { return false; }

    /*
    public synchronized Downloader download(RemoteFileDesc[] files,
                                            boolean overwrite) 
            throws FileExistsException, AlreadyDownloadingException, 
				   java.io.FileNotFoundException { 
        throw new AlreadyDownloadingException(); 
    }
    public synchronized Downloader download(
    public synchronized Downloader download(File incompleteFile)
    public synchronized Downloader download(String query,
    */

    public String conflicts(RemoteFileDesc[] files, ManagedDownloader dloader) { 
        return null; 
    }
    public void handleQueryReply(QueryReply qr) { }
    public void acceptDownload(Socket socket) { }
    public void waitForSlot(ManagedDownloader downloader) { }
    public void yieldSlot(ManagedDownloader downloader) { }
    public void remove(ManagedDownloader downloader, boolean success) { }
    public boolean sendQuery(ManagedDownloader requerier, QueryRequest query) { 
        return false; 
    }
    public boolean sendPush(RemoteFileDesc file) { return false; }
    public synchronized void measureBandwidth() { }
	public synchronized float getMeasuredBandwidth() { return 0.f; }
    public void internalError(Throwable e) { }
    public IncompleteFileManager getIncompleteFileManager() { return null; }
}
