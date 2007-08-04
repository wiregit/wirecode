package com.limegroup.gnutella;


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
                ProviderHacks.getStatistics()

        );
    }

}