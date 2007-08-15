package com.limegroup.gnutella.downloader;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.settings.DownloadSettings;

@Singleton
public class SourceRankerFactory {
    
    private final NetworkManager networkManager;
    private final Provider<UDPPinger> udpPingerFactory;
    private final Provider<MessageRouter> messageRouter;
    
    @Inject
    public SourceRankerFactory(NetworkManager networkManager,
                               Provider<UDPPinger> udpPingerFactory, 
                               Provider<MessageRouter> messageRouter) {
        this.networkManager = networkManager;
        this.udpPingerFactory = udpPingerFactory;
        this.messageRouter = messageRouter;
    }

    /**
     * @return a ranker appropriate for our system's capabilities.
     */
    public SourceRanker getAppropriateRanker() {
        if (networkManager.canReceiveSolicited() && 
                DownloadSettings.USE_HEADPINGS.getValue())
            return new PingRanker(networkManager, udpPingerFactory.get(), messageRouter.get());
        else 
            return new LegacyRanker();
    }

    /**
     * @param original the current ranker that we use
     * @return the ranker that should be used.  If different than the current one,
     * the current one is stopped.
     */
    public SourceRanker getAppropriateRanker(SourceRanker original) {
        if(original == null)
            return getAppropriateRanker();
        
        SourceRanker better;
        if (networkManager.canReceiveSolicited() && 
                DownloadSettings.USE_HEADPINGS.getValue()) {
            if (original instanceof PingRanker)
                return original;
            better = new PingRanker(networkManager, udpPingerFactory.get(), messageRouter.get());
        }else {
            if (original instanceof LegacyRanker)
                return original;
            better = new LegacyRanker();
        }
        
        better.setMeshHandler(original.getMeshHandler());
        better.addToPool(original.getShareableHosts());
        original.stop();
        return better;
    }
}
