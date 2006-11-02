package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

import com.limegroup.gnutella.io.Shutdownable;
import com.limegroup.gnutella.statistics.DownloadStat;

/**
 * A ConnectObserver for starting the download via a push connect.
 */
class PushConnector extends HTTPConnectObserver {

	private final DownloadWorker worker;
	private final ManagedDownloader manager;
	private boolean forgetOnFailure;
    private boolean directConnectOnFailure;
    private PushDetails pushDetails;
    
    /**
     * Additional Shutdownable to notify if we are shutdown
     */
    private AtomicReference<Shutdownable> toCancel = 
    	new AtomicReference<Shutdownable>(null);
    
    /**
     * Creates a new PushConnector.  If forgetOnFailure is true,
     * this will call _manager.forgetRFD(_rfd) if the push fails.
     * If directConnectOnFailure is true, this will attempt a direct
     * connection if the push fails.
     * Upon success, this will always start the download.
     * 
     * @param forgetOnFailure
     * @param directConnectOnFailure
     * @param worker TODO
     */
    PushConnector(DownloadWorker worker, ManagedDownloader manager, boolean forgetOnFailure, boolean directConnectOnFailure) {
        this.worker = worker;
        this.manager = manager;
		this.forgetOnFailure = forgetOnFailure;
        this.directConnectOnFailure = directConnectOnFailure;
    }
    
    Shutdownable updateCancellable(Shutdownable newCancel) {
    	return toCancel.getAndSet(newCancel);
    }

    /**
     * Notification that the push succeeded.  Starts the download if the connection still exists.
     */
    public void handleConnect(Socket socket) {
        //LOG.debug(_rfd + " -- Handling connect from PushConnector");
        HTTPDownloader dl = worker.createDownloader(socket);
        try {
           dl.connectTCP(0);
           DownloadStat.CONNECT_PUSH_SUCCESS.incrementStat();
        } catch(IOException iox) {
            //LOG.debug(_rfd + " -- IOX after starting connected from PushConnector.");
            DownloadStat.PUSH_FAILURE_LOST.incrementStat();
            failed();
            return;
        }
        
        worker.startDownload(dl);
    }

    /** Notification that the push failed. */
    public void shutdown() {
    	Shutdownable delegate = toCancel.get();
    	if (delegate != null)
    		delegate.shutdown();
        //LOG.debug(_rfd + " -- Handling shutdown from PushConnector");            
        DownloadStat.PUSH_FAILURE_NO_RESPONSE.incrementStat();
        failed();
    }
    
    /** Sets the details that will be used to unregister the push observer. */
    void setPushDetails(PushDetails details) {
        this.pushDetails = details;
    }
    
    /**
     * Possibly tells the manager to forget this RFD, cleans up various things,
     * and tells the manager to forget this worker.
     */
    private void failed() {            
        manager.unregisterPushObserver(pushDetails, false);
        
        if(!directConnectOnFailure) {
            if(forgetOnFailure) {
                manager.forgetRFD(worker.getRFD());
            }
            this.worker.finishConnect();
            this.worker.finishWorker();
        } else {
            this.worker.connectDirectly(this.worker.new DirectConnector(false));
        }
    }
}