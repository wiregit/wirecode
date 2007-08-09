package com.limegroup.bittorrent.handshaking;

import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.util.SocketsManager;

@Singleton
public class BTConnectionFetcherFactory {
    
    private final ScheduledExecutorService scheduledExecutorService;
    private final ApplicationServices applicationServices;
    private final SocketsManager socketsManager;
        
	@Inject
	public BTConnectionFetcherFactory(
            @Named("nioExecutor") ScheduledExecutorService scheduledExecutorService,
            ApplicationServices applicationServices,
            SocketsManager socketsManager) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.applicationServices = applicationServices;
        this.socketsManager = socketsManager;
    }


    public BTConnectionFetcher getBTConnectionFetcher(ManagedTorrent torrent) {
		return new BTConnectionFetcher(torrent, scheduledExecutorService, applicationServices, socketsManager);
	}
}
