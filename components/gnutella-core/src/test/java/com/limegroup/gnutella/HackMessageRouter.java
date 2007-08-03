package com.limegroup.gnutella;

public class HackMessageRouter extends StandardMessageRouter {

    public HackMessageRouter() {
        super(ProviderHacks.getNetworkManager(), ProviderHacks
                .getQueryRequestFactory(), ProviderHacks
                .getQueryHandlerFactory(),
                ProviderHacks.getOnDemandUnicaster(), ProviderHacks
                        .getHeadPongFactory(), ProviderHacks
                        .getPingReplyFactory());
    }

}
