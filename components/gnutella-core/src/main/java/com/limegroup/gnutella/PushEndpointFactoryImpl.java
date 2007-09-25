package com.limegroup.gnutella;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.StringTokenizer;

import org.limewire.collection.BitNumbers;
import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IPPortCombo;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkUtils;
import org.limewire.rudp.UDPConnection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.messages.BadPacketException;

@Singleton
public class PushEndpointFactoryImpl implements PushEndpointFactory {

    private final NetworkManager networkManager;
    private final Provider<UDPService> udpService;
    private final Provider<ConnectionManager> connectionManager;
    private final ApplicationServices applicationServices;
    private final Provider<PushEndpointCache> pushEndpointCache;

    private final Provider<PushEndpoint> selfProvider;
    
    @Inject
    public PushEndpointFactoryImpl(NetworkManager networkManager,
            Provider<UDPService> udpService,
            Provider<ConnectionManager> connectionManager,
            ApplicationServices applicationServices,
            Provider<PushEndpointCache> pushEndpointCache) {
        this.networkManager = networkManager;
        this.udpService = udpService;
        this.connectionManager = connectionManager;
        this.applicationServices = applicationServices;
        this.pushEndpointCache = pushEndpointCache;
        this.selfProvider = new AbstractLazySingletonProvider<PushEndpoint>() {
            @Override
            protected PushEndpoint createObject() {
                return new SelfEndpoint(
                        PushEndpointFactoryImpl.this.networkManager,
                        PushEndpointFactoryImpl.this.applicationServices,
                        PushEndpointFactoryImpl.this.connectionManager,
                        PushEndpointFactoryImpl.this.udpService);
            }
            
        };
    }       
    
    public PushEndpoint createPushEndpoint(byte[] guid) {
        return createPushEndpoint(guid, IpPort.EMPTY_SET);
    }
    
    public PushEndpoint createPushEndpoint(byte[] guid, Set<? extends IpPort> proxies) {
        return createPushEndpoint(guid, proxies, PushEndpoint.PLAIN, 0);
    }
    
    public PushEndpoint createPushEndpoint(byte[] guid, Set<? extends IpPort> proxies, byte features, int version) {
        return createPushEndpoint(guid, proxies, features, version, null);
    }

    public PushEndpoint createPushEndpoint(byte[] guid, Set<? extends IpPort> proxies, byte features, int version, IpPort addr) {
        return new PushEndpoint(guid, proxies, features, version, addr, pushEndpointCache.get());
    }

    public PushEndpoint createPushEndpoint(String httpString) throws IOException {
        byte[] guid;
        
        if (httpString.length() < 32 ||
                httpString.indexOf(";") > 32)
            throw new IOException("http string does not contain valid guid");
        
        //the first token is the guid
        String guidS=httpString.substring(0,32);
        httpString = httpString.substring(32);
        
        try {
            guid = GUID.fromHexString(guidS);
        } catch(IllegalArgumentException iae) {
            throw new IOException(iae.getMessage());
        }
        
        StringTokenizer tok = new StringTokenizer(httpString,";");        
        Set<IpPort> proxies = new IpPortSet();        
        int fwtVersion =0;        
        IpPort addr = null;
        BitNumbers tlsProxies = null;
        
        while(tok.hasMoreTokens()) {
            String current = tok.nextToken().trim();
            
            // see if this token is the fwt header
            // if this token fails to parse we abort since we must know
            // if the PE supports fwt or not. 
            if (current.startsWith(HTTPConstants.FW_TRANSFER)) {
                fwtVersion = (int) HTTPUtils.parseFeatureToken(current);
                continue;
            }
            
            // don't parse it in the middle of parsing proxies.
            if (proxies.size() == 0 && current.startsWith(PushEndpoint.PPTLS_HTTP)) {
                String value = HTTPUtils.parseValue(current);
                if(value != null) {
                    try {
                        tlsProxies = new BitNumbers(value);
                    } catch(IllegalArgumentException invalid) {
                        throw (IOException)new IOException().initCause(invalid);
                    }
                }
                continue;
            }

            // Only look for more proxies if we didn't reach our limit
            if(proxies.size() < PushEndpoint.MAX_PROXIES) {
                boolean tlsCapable = tlsProxies != null && tlsProxies.isSet(proxies.size());
                // if its not the header, try to parse it as a push proxy
                try {
                    proxies.add(NetworkUtils.parseIpPort(current, tlsCapable));
                    continue;
                } catch(IOException ohWell) {
                    tlsProxies = null; // stop adding TLS, since our index may be off
                }
            }
            
            // if its not a push proxy, try to parse it as a port:ip
            // only the first occurence of port:ip is parsed
            if (addr==null) {
                try {
                    addr = parsePortIp(current);
                }catch(IOException notBad) {}
            }
            
        }
        
        return createPushEndpoint(guid, proxies, (byte)(proxies.size() | fwtVersion << 3), fwtVersion, addr);
    }    

    public PushEndpoint createFromBytes(DataInputStream dais) throws BadPacketException, IOException {
        byte [] guid =new byte[16];
        Set<IpPort> proxies = new IpPortSet(); 
        IpPort addr = null;
        
        byte header = (byte)(dais.read() & 0xFF);
        
        // get the number of push proxies
        byte number = (byte)(header & PushEndpoint.SIZE_MASK); 
        byte features = (byte)(header & PushEndpoint.FEATURES_MASK);
        byte version = (byte)((header & PushEndpoint.FWT_VERSION_MASK) >> 3);
        
        dais.readFully(guid);
        
        if (version > 0) {
            byte [] host = new byte[6];
            dais.readFully(host);
            try {
                addr = IPPortCombo.getCombo(host);
            } catch(InvalidDataException ide) {
                throw new BadPacketException(ide);
            }
            if (addr.getAddress().equals(RemoteFileDesc.BOGUS_IP)) {
                addr = null;
                version = 0;
            }
        }
        
        // If the features mentioned this has pptls bytes, read that.
        BitNumbers bn = null;
        if((features & PushEndpoint.PPTLS_BINARY) != 0) {
            byte[] tlsIndexes = new byte[1];
            dais.readFully(tlsIndexes);
            bn = new BitNumbers(tlsIndexes);
        }   
        
        byte [] tmp = new byte[6];
        for (int i = 0; i < number; i++) {
            dais.readFully(tmp);
            try {
                IpPort ipp = IPPortCombo.getCombo(tmp);
                if(bn != null && bn.isSet(i))
                    ipp = new ConnectableImpl(ipp, true);
                proxies.add(ipp);
            } catch(InvalidDataException ide) {
                throw new BadPacketException(ide);
            }
        }
        
        /** this adds the read set to the existing proxies */
        PushEndpoint pe = createPushEndpoint(guid, proxies, features, version, addr);
        pe.updateProxies(true);
        return pe;
    }    
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.PushEndpointFactory#createForSelf()
     */
    public PushEndpoint createForSelf() {
        return selfProvider.get(); 
    }    
    
    /** 
     * @param http a string representing a port and an ip
     * @return an object implementing IpPort 
     * @throws IOException parsing failed.
     */
    // NOTE: this is parse PORT IP, not parseIpPort
    private IpPort parsePortIp(String http) throws IOException{
        int separator = http.indexOf(":");
    	
    	//see if this is a valid ip:port address; 
    	if (separator == -1 || separator!= http.lastIndexOf(":") ||
    			separator == http.length())
    		throw new IOException();
    	
    	String portS = http.substring(0,separator);
    	int port =0;
    	
    	try {
    		port = Integer.parseInt(portS);
    		if(!NetworkUtils.isValidPort(port))
    		    throw new IOException();
    	}catch(NumberFormatException failed) {
    	    throw new IOException(failed.getMessage());
    	}
    	
    	String host = http.substring(separator+1);
    	
    	if (!NetworkUtils.isValidAddress(host) || NetworkUtils.isPrivateAddress(host))
    	    throw new IOException();
    	
    	return new IpPortImpl(host,port);
    }

    private static class SelfEndpoint extends PushEndpoint {
        private final NetworkManager networkManager;
        private final Provider<ConnectionManager> connectionManager;
        private final Provider<UDPService> udpService;
        
        private SelfEndpoint(NetworkManager networkManager,
                ApplicationServices applicationServices,
                Provider<ConnectionManager> connectionManager,
                Provider<UDPService> udpService) {
            super(applicationServices.getMyGUID(), IpPort.EMPTY_SET,
                    PushEndpoint.PLAIN, UDPConnection.VERSION, null, null);
            this.networkManager = networkManager;
            this.connectionManager = connectionManager;
            this.udpService = udpService;
        }

        /**
         * delegate the call to connection manager
         */
        @Override
        public Set<? extends IpPort> getProxies() {
            return connectionManager.get().getPushProxies();
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
                return udpService.get().getStableUDPPort();
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
