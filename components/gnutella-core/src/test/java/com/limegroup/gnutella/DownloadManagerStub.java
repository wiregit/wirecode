package com.limegroup.gnutella;

import java.io.File;
import java.net.Socket;


import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.MultiShutdownable;


public class DownloadManagerStub extends DownloadManager {
	private final PushDownloadManager pdManager = new PushDownloadManagerStub();
    @Override
    public void initialize(DownloadCallback callback, MessageRouter router,
                           FileManager fileManager) {
        super.initialize(callback, router, fileManager);
        postGuiInit();
    }

    @Override
    public synchronized int downloadsInProgress() { return 0; }
    @Override
    public synchronized boolean readSnapshot(File file) { return false; }
    @Override
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
    
    @Override
    public void handleQueryReply(QueryReply qr) { }
    //public void remove(ManagedDownloader downloader, boolean success) { }
    @Override
    public boolean sendQuery(ManagedDownloader requerier, QueryRequest query) { 

		return !GUID.isLimeRequeryGUID(query.getGUID());
    }
    @Override
    public synchronized void measureBandwidth() { }
    @Override
	public synchronized float getMeasuredBandwidth() { return 0.f; }
    @Override
    public IncompleteFileManager getIncompleteFileManager() { return null; }
    
    public PushDownloadManager getPushManager() {return pdManager;}
    
    private static class PushDownloadManagerStub extends PushDownloadManager {
    	PushDownloadManagerStub() {
    		super(null, null, null, null, null);
    	}
		@Override
		public void acceptConnection(String word, Socket socket) {}

		@Override
		public void initialize(ConnectionDispatcher dispatcher) {}

		@Override
		public void sendPush(RemoteFileDesc file, MultiShutdownable observer) {}

		@Override
		public void sendPush(RemoteFileDesc file) {}
    	
    }
}
