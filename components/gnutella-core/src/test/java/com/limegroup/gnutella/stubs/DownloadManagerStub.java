package com.limegroup.gnutella.stubs;

import java.io.File;
import java.net.Socket;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;


public class DownloadManagerStub extends DownloadManager {
    public void initialize(ActivityCallback callback, MessageRouter router,
                           FileManager fileManager) {
        super.initialize(callback, router, fileManager);
        postGuiInit();
    }

    public synchronized int downloadsInProgress() { return 0; }
    public synchronized boolean readSnapshot(File file) { return false; }
    public synchronized boolean writeSnapshot() { return true; }

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
    //public void remove(ManagedDownloader downloader, boolean success) { }
    public boolean sendQuery(ManagedDownloader requerier, QueryRequest query) { 

		return !GUID.isLimeRequeryGUID(query.getGUID());
    }
    public void sendPush(RemoteFileDesc file) {}
    public synchronized void measureBandwidth() { }
	public synchronized float getMeasuredBandwidth() { return 0.f; }
    public void internalError(Throwable e) { }
    public IncompleteFileManager getIncompleteFileManager() { return null; }
}
