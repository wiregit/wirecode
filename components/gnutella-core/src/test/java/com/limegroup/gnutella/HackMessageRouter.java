package com.limegroup.gnutella;

import org.limewire.concurrent.SimpleProvider;

public class HackMessageRouter extends StandardMessageRouter {

    public HackMessageRouter() {
        super(ProviderHacks.getNetworkManager(), ProviderHacks
                .getQueryRequestFactory(), ProviderHacks
                .getQueryHandlerFactory(),
                ProviderHacks.getOnDemandUnicaster(), ProviderHacks
                        .getHeadPongFactory(), ProviderHacks
                        .getPingReplyFactory(), ProviderHacks
                        .getConnectionManager(), ProviderHacks
                        .getForMeReplyHandler(), ProviderHacks
                        .getQueryUnicaster(), ProviderHacks.getFileManager(),
                ProviderHacks.getContentManager(), ProviderHacks
                        .getDHTManager(), ProviderHacks.getUploadManager(),
                ProviderHacks.getDownloadManager(), ProviderHacks
                        .getUdpService(), ProviderHacks
                        .getSearchResultHandler(), ProviderHacks
                        .getSocketsManager(), ProviderHacks.getHostCatcher(),
                ProviderHacks.getQueryReplyFactory(), ProviderHacks
                        .getStaticMessages(), SimpleProvider.of(ProviderHacks
                        .getMessageDispatcher()), ProviderHacks
                        .getMulticastService(), ProviderHacks
                        .getQueryDispatcher(), SimpleProvider.of(ProviderHacks
                        .getActivityCallback()), ProviderHacks
                        .getConnectionServices(), ProviderHacks
                        .getApplicationServices(), ProviderHacks
                        .getBackgroundExecutor(), SimpleProvider
                        .of(ProviderHacks.getPongCacher()), SimpleProvider
                        .of(ProviderHacks.getSimppManager()), SimpleProvider
                        .of(ProviderHacks.getUpdateHandler()),

                ProviderHacks.getStatistics()

        );
    }

}