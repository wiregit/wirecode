package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkUtils;
import org.limewire.rudp.UDPConnection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class PushEndpointFactoryImpl implements PushEndpointFactory {
    
    private final NetworkManager networkManager;
    private final Provider<PushEndpoint> selfProvider;
    
    @Inject
    public PushEndpointFactoryImpl(NetworkManager networkManager) {
        this.networkManager = networkManager;
        this.selfProvider = new AbstractLazySingletonProvider<PushEndpoint>() {
            @Override
            protected PushEndpoint createObject() {
                return new SelfEndpoint(PushEndpointFactoryImpl.this.networkManager);
            }
            
        };
    }   
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.PushEndpointFactory#createForSelf()
     */
    public PushEndpoint createForSelf() {
        return selfProvider.get(); 
    }
    
    
    private static class SelfEndpoint extends PushEndpoint {
        private final NetworkManager networkManager;

        private SelfEndpoint(NetworkManager networkManager) {
            super(RouterService.getMyGUID(), IpPort.EMPTY_SET,
                    PushEndpoint.PLAIN, UDPConnection.VERSION);
            this.networkManager = networkManager;
        }

        /**
         * delegate the call to connection manager
         */
        @Override
        public Set<? extends IpPort> getProxies() {
            return ProviderHacks.getConnectionManager().getPushProxies();
        }

        /**
         * we always have the same features
         */
        @Override
        public byte getFeatures() {
            return 0;
        }

        /**
         * we support the same FWT version if we support FWT at all
         */
        @Override
        public int supportsFWTVersion() {
            return networkManager.canDoFWT() ? UDPConnection.VERSION : 0;
        }

        /**
         * Our address is our external address if it is valid and external.
         * Otherwise we return the BOGUS_IP 
         */
        @Override
        public String getAddress() {
            byte[] addr = networkManager.getExternalAddress();

            if (NetworkUtils.isValidAddress(addr)
                    && !NetworkUtils.isPrivateAddress(addr))
                return NetworkUtils.ip2string(addr);

            return RemoteFileDesc.BOGUS_IP;
        }

        /**
         * @return our external address.  First converts it to string since
         * 1.3 jvms does not support getting it from byte[].
         */
        @Override
        public InetAddress getInetAddress() {
            try {
                return InetAddress.getByName(getAddress());
            } catch (UnknownHostException bad) {
                return null;
            }
        }

        /**
         * Our port is our external port
         */
        @Override
        public int getPort() {
            if (networkManager.canDoFWT()
                    && !networkManager.acceptedIncomingConnection())
                return ProviderHacks.getUdpService().getStableUDPPort();
            return networkManager.getPort();
        }

        @Override
        protected IpPort getValidExternalAddress() {
            try {
                String addr = getAddress();
                int port = getPort();
                if (addr.equals(RemoteFileDesc.BOGUS_IP)
                        || !NetworkUtils.isValidPort(port))
                    return null;
                return new IpPortImpl(addr, getPort());

            } catch (UnknownHostException bad) {
                return null;
            }
        }
        
        @Override
        public boolean isLocal() {
            return true;
        }
    }

}
