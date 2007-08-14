package com.limegroup.gnutella;

import org.limewire.concurrent.Providers;

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
                        .getStaticMessages(), Providers.of(ProviderHacks
                        .getMessageDispatcher()), ProviderHacks
                        .getMulticastService(), ProviderHacks
                        .getQueryDispatcher(), Providers.of(ProviderHacks
                        .getActivityCallback()), ProviderHacks
                        .getConnectionServices(), ProviderHacks
                        .getApplicationServices(), ProviderHacks
                        .getBackgroundExecutor(), Providers
                        .of(ProviderHacks.getPongCacher()), Providers
                        .of(ProviderHacks.getSimppManager()), Providers
                        .of(ProviderHacks.getUpdateHandler()), ProviderHacks.getGuidMapManager(),

                ProviderHacks.getStatistics()

        );
    }

}