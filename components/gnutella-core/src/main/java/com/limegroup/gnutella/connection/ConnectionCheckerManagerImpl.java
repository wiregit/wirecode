package com.limegroup.gnutella.connection;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ThreadExecutor;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.UploadServices;
import com.limegroup.gnutella.util.SocketsManager;

@Singleton
public class ConnectionCheckerManagerImpl implements ConnectionCheckerManager {
    
    private static final Log LOG = LogFactory.getLog(ConnectionCheckerManagerImpl.class);

    private final AtomicInteger numWorkarounds = new AtomicInteger();
    private ConnectionChecker currentChecker;

    private final ConnectionServices connectionServices;
    private final Provider<ConnectionManager> connectionManager;
    private final UploadServices uploadServices;
    private final Provider<DownloadManager> downloadManager;
    private final Provider<UDPService> udpService;
    private final Provider<HostCatcher> hostCatcher;
    private final SocketsManager socketsManager;
    
    @Inject
    public ConnectionCheckerManagerImpl(ConnectionServices connectionServices,
            Provider<ConnectionManager> connectionManager,
            UploadServices uploadServices,
            Provider<DownloadManager> downloadManager,
            Provider<UDPService> udpService, Provider<HostCatcher> hostCatcher,
            SocketsManager socketsManager) {
        this.connectionServices = connectionServices;
        this.connectionManager = connectionManager;
        this.uploadServices = uploadServices;
        this.downloadManager = downloadManager;
        this.udpService = udpService;
        this.hostCatcher = hostCatcher;
        this.socketsManager = socketsManager;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.ConnectionCheckerManager#checkForLiveConnection()
     */
    public ConnectionChecker checkForLiveConnection() {
        LOG.trace("checking for live connection");
    
        ConnectionChecker checker;
        boolean startThread = false;
        synchronized(this) {
            if (currentChecker == null) {
                startThread = true;
                currentChecker = new ConnectionChecker(numWorkarounds,
                        connectionServices, uploadServices, connectionManager,
                        downloadManager, hostCatcher, udpService,
                        socketsManager);
            }
            checker = currentChecker;
        }
        
        // Only create a new thread if one isn't alive.
        if(startThread) {
            final Runnable runner = checker;
            LOG.debug("Starting a new connection-checker thread");
            ThreadExecutor.startThread(new Runnable() {
                public void run() {
                    try {
                        runner.run();
                    } finally {
                        synchronized(this) {
                            currentChecker = null;
                        }
                    }
                }
            }, "check for live connection");
        }
        
        return checker;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.ConnectionCheckerManager#getNumWorkarounds()
     */
    public int getNumWorkarounds() {
        return numWorkarounds.get();
    }



}
