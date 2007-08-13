package com.limegroup.gnutella;

import org.limewire.concurrent.Providers;

import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;

/** 
 * A stubbed-out ManagedConnection that does nothing.  Useful for testing, since
 * ManagedConnection has no public-access constructors.  ManagedConnectionStub
 * is in this package instead of com.limegroup.gnutella.stubs because it
 * requires package-access to ManagedConnection.
 */
public class ManagedConnectionStub extends ManagedConnection {
    public ManagedConnectionStub() {
        super("1.2.3.4", 6346, ConnectType.PLAIN, new ConnectionManagerStub(),
                ProviderHacks.getNetworkManager(), ProviderHacks
                        .getQueryRequestFactory(), ProviderHacks
                        .getHeadersFactory(), ProviderHacks
                        .getHandshakeResponderFactory(), ProviderHacks
                        .getQueryReplyFactory(), ProviderHacks
                        .getMessageDispatcher(), ProviderHacks
                        .getNetworkUpdateSanityChecker(), ProviderHacks
                        .getUdpService(), ProviderHacks.getMessageRouter(),
                ProviderHacks.getSearchResultHandler(), ProviderHacks.getCapabilitiesVMFactory(),
                ProviderHacks.getSocketsManager(), ProviderHacks.getAcceptor(), ProviderHacks.getMessagesSupportedVendorMessage(),
                Providers.of(ProviderHacks.getSimppManager()), Providers.of(ProviderHacks.getUpdateHandler()),
                Providers.of(ProviderHacks.getConnectionServices()));
    }

    public void initialize() {
    }
}
