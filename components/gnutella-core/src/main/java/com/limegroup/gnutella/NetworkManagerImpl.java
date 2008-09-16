package com.limegroup.gnutella;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.Properties;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.LimeProps;
import org.limewire.core.settings.SearchSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.HolePunchAddress;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.ssl.SSLEngineTest;
import org.limewire.nio.ssl.SSLUtils;
import org.limewire.rudp.RUDPUtils;
import org.limewire.service.ErrorService;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.HeaderUpdateVendorMessage;
import com.limegroup.gnutella.net.address.gnutella.PushProxyHolePunchAddress;
import com.limegroup.gnutella.net.address.gnutella.PushProxyHolePunchAddressImpl;
import com.limegroup.gnutella.net.address.gnutella.PushProxyMediatorAddress;
import com.limegroup.gnutella.statistics.OutOfBandStatistics;

@Singleton
public class NetworkManagerImpl implements NetworkManager {
    
    private final Provider<UDPService> udpService;
    private final Provider<Acceptor> acceptor;
    private final Provider<DHTManager> dhtManager;
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<ActivityCallback> activityCallback;
    private final OutOfBandStatistics outOfBandStatistics;
    private final NetworkInstanceUtils networkInstanceUtils;
    private final Provider<CapabilitiesVMFactory> capabilitiesVMFactory;
    private final SettingListener fwtListener = new FWTChangeListener();
    private final Provider<ByteBufferCache> bbCache;
    
    private final Object addressLock = new Object();
    private volatile Connectable directAddress;
    private volatile PushProxyMediatorAddress mediatedAddress;
    private volatile HolePunchAddress holePunchAddress;
    
    
    /** True if TLS is supported for this session. */
    private volatile boolean tlsSupported = true;
    
    /** The Throwable that was the reason TLS failed. */
    @InspectablePrimitive("reason tls failed")
    @SuppressWarnings({"unused", "FieldCanBeLocal", "UnusedDeclaration"})
    private volatile String tlsDisabledReason;
    
    private final EventListenerList<AddressEvent> listeners =
        new EventListenerList<AddressEvent>(getClass());
    
    @Inject
    public NetworkManagerImpl(Provider<UDPService> udpService,
            Provider<Acceptor> acceptor,
            Provider<DHTManager> dhtManager,
            Provider<ConnectionManager> connectionManager,
            Provider<ActivityCallback> activityCallback,
            OutOfBandStatistics outOfBandStatistics,
            NetworkInstanceUtils networkInstanceUtils,
            Provider<CapabilitiesVMFactory> capabilitiesVMFactory,
            Provider<ByteBufferCache> bbCache) {
        this.udpService = udpService;
        this.acceptor = acceptor;
        this.dhtManager = dhtManager;
        this.connectionManager = connectionManager;
        this.activityCallback = activityCallback;
        this.outOfBandStatistics = outOfBandStatistics;
        this.networkInstanceUtils = networkInstanceUtils;
        this.capabilitiesVMFactory = capabilitiesVMFactory;
        this.bbCache = bbCache;
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    

    public void start() {
        ConnectionSettings.CANNOT_DO_FWT.addSettingListener(fwtListener);
        if(isIncomingTLSEnabled() || isOutgoingTLSEnabled()) {
            SSLEngineTest sslTester = new SSLEngineTest(SSLUtils.getTLSContext(), SSLUtils.getTLSCipherSuites(), bbCache.get());
            if(!sslTester.go()) {
                Throwable t = sslTester.getLastFailureCause();
                setTLSNotSupported(t);
                if(!SSLSettings.IGNORE_SSL_EXCEPTIONS.getValue() && !sslTester.isIgnorable(t))
                    ErrorService.error(t);
            }
        }
    }


    public void stop() {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(acceptedIncomingConnection());
        ConnectionSettings.CANNOT_DO_FWT.removeSettingListener(fwtListener);
    }
    
    public void initialize() {
    }
    
    public String getServiceName() {
        return I18nMarker.marktr("Network Management");
    }


    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#isIpPortValid()
     */
    public boolean isIpPortValid() {
        return (NetworkUtils.isValidAddress(getAddress()) &&
                NetworkUtils.isValidPort(getPort()));
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#getUDPConnectBackGUID()
     */
    public GUID getUDPConnectBackGUID() {
        return udpService.get().getConnectBackGUID();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#isOOBCapable()
     */
    public boolean isOOBCapable() {
        return isGUESSCapable() && outOfBandStatistics.isSuccessRateGood()&&
               !networkInstanceUtils.isPrivate() &&
               SearchSettings.OOB_ENABLED.getValue() &&
               acceptor.get().isAddressExternal() && isIpPortValid();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#isGUESSCapable()
     */
    public boolean isGUESSCapable() {
    	return udpService.get().isGUESSCapable();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#getNonForcedPort()
     */
    public int getNonForcedPort() {
        return acceptor.get().getPort(false);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#getPort()
     */    
    public int getPort() {
    	return acceptor.get().getPort(true);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#getNonForcedAddress()
     */
    public byte[] getNonForcedAddress() {
        return acceptor.get().getAddress(false);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#getAddress()
     */
    public byte[] getAddress() {
    	return acceptor.get().getAddress(true);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#getExternalAddress()
     */
    public byte[] getExternalAddress() {
        return acceptor.get().getExternalAddress();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#incomingStatusChanged()
     */
    public boolean incomingStatusChanged() {
        updateCapabilities();
        activityCallback.get().handleAddressStateChanged();
        // Only continue if the current address/port is valid & not private.
        byte addr[] = getAddress();
        int port = getPort();
        if(!NetworkUtils.isValidAddress(addr))
            return false;
        if(networkInstanceUtils.isPrivateAddress(addr))
            return false;            
        if(!NetworkUtils.isValidPort(port))
            return false;
            
        return true;
    }

    private void updateCapabilities() {
        capabilitiesVMFactory.get().updateCapabilities();
        if (connectionManager.get().isShieldedLeaf()) 
            connectionManager.get().sendUpdatedCapabilities();
        synchronized (addressLock) {
            maybeFireNewHolePunchAddress();
        }
    }

    private boolean maybeFireNewHolePunchAddress() {
        if(isPushProxyHolePunchCapable()) {
            PushProxyHolePunchAddress newHolePunchAddress = getPushProxyHolePunchAddress();
            if(holePunchAddress == null || !holePunchAddress.equals(newHolePunchAddress)) {
                fireHolePunchAddressEvent(newHolePunchAddress);
                return true;
            }
        } else {
            // TODO newMediatedConnectionAddress(mediatedAddress); ??
        }
        return false;
    }

    private boolean isPushProxyHolePunchCapable() {
        return supportsFWTVersion() > 0 && mediatedAddress != null;
    }

    private void fireHolePunchAddressEvent(PushProxyHolePunchAddress holePunchAddress) {
        this.holePunchAddress = holePunchAddress;
        fireEvent(new AddressEvent(holePunchAddress, Address.EventType.ADDRESS_CHANGED));
    }

    private PushProxyHolePunchAddress getPushProxyHolePunchAddress() {
        try {
            Connectable directAddress = new ConnectableImpl(NetworkUtils.ip2string(getExternalAddress()),
                    getStableUDPPort(), isIncomingTLSEnabled()); // TODO is that the right port method?
            return new PushProxyHolePunchAddressImpl(supportsFWTVersion(), directAddress, mediatedAddress);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean addressChanged() {
        activityCallback.get().handleAddressStateChanged();        
        
        // Only continue if the current address/port is valid & not private.
        byte addr[] = getAddress();
        int port = getPort();
        if(!NetworkUtils.isValidAddress(addr))
            return false;
        if(networkInstanceUtils.isPrivateAddress(addr))
            return false;            
        if(!NetworkUtils.isValidPort(port))
            return false;
    
        
        // reset the last connect back time so the next time the TCP/UDP
        // validators run they try to connect back.
        acceptor.get().resetLastConnectBackTime();
        
        // Notify the DHT
        dhtManager.get().addressChanged();
        
    	Properties props = new Properties();
    	props.put(HeaderNames.LISTEN_IP,NetworkUtils.ip2string(addr)+":"+port);
    	HeaderUpdateVendorMessage huvm = new HeaderUpdateVendorMessage(props);
    	
        for(RoutedConnection c : connectionManager.get().getInitializedConnections()) {
    		if (c.getConnectionCapabilities().remoteHostSupportsHeaderUpdate() >= HeaderUpdateVendorMessage.VERSION)
    			c.send(huvm);
    	}
    	
        for(RoutedConnection c : connectionManager.get().getInitializedClientConnections()) {
    		if (c.getConnectionCapabilities().remoteHostSupportsHeaderUpdate() >= HeaderUpdateVendorMessage.VERSION)
    			c.send(huvm);
    	}

        // TODO
        //fireEvent(new AddressEvent(null, Address.EventType.ADDRESS_CHANGED));
        
        return true;
    }

    public void externalAddressChanged() {
        synchronized (addressLock) {
            maybeFireNewDirectConnectionAddress();
        }
    }

    private void maybeFireNewDirectConnectionAddress() {
        if(isDirectConnectionCapable()) {
            Connectable newDirectAddress = getDirectConnectionAddress();
            if(directAddress == null || !directAddress.equals(newDirectAddress)) {
                fireDirectConnectionAddressEvent(newDirectAddress); 
            }
        } else {
            fireNullAddressEvent();
        }
    }

    private boolean isDirectConnectionCapable() {
        // TODO is that the right port method?
        return NetworkUtils.isValidAddress(getExternalAddress()) 
                && acceptedIncomingConnection() && NetworkUtils.isValidPort(getNonForcedPort());
    }

    public void portChanged() {
        synchronized (addressLock) {
            maybeFireNewDirectConnectionAddress();
        }
    }

    public void acceptedIncomingConnectionChanged() {
        synchronized (addressLock) {
            maybeFireNewDirectConnectionAddress();
        }
    }

    private Connectable getDirectConnectionAddress() {
        try {
            return new ConnectableImpl(NetworkUtils.ip2string(getExternalAddress()),
                    getNonForcedPort(), isIncomingTLSEnabled()); // TODO is that the right port method?
        } catch (UnknownHostException e) {
            // TODO does this warrant ErrorService?
            ErrorService.error(e);
            return null;
        }                                  
    }
    
    private void fireDirectConnectionAddressEvent(Connectable address) {
        directAddress = address;
        fireEvent(new AddressEvent(address, Address.EventType.ADDRESS_CHANGED));                                 
    }

    public void newMediatedConnectionAddress(PushProxyMediatorAddress newMediatorAddress) { 
        synchronized (addressLock) {
            if(supportsFWTVersion() > 0) {
                mediatedAddress = newMediatorAddress;
                PushProxyHolePunchAddress newHolePunchAddress = getPushProxyHolePunchAddress();
                if(holePunchAddress == null || !holePunchAddress.equals(newHolePunchAddress)) {
                    fireHolePunchAddressEvent(newHolePunchAddress);
                }
            } else if(mediatedAddress == null || !mediatedAddress.equals(newMediatorAddress)) {
                fireMediatedConenctionAddressEvent(newMediatorAddress);
            }           
        }
    }
    
    private void fireMediatedConenctionAddressEvent(PushProxyMediatorAddress address) {
        mediatedAddress = address;
        fireEvent(new AddressEvent(address, Address.EventType.ADDRESS_CHANGED));                                 
    }

    private void fireNullAddressEvent() {
        // TODO NullAddress
        //fireEvent(new AddressEvent(null, Address.EventType.ADDRESS_CHANGED));
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.NetworkManager#acceptedIncomingConnection()
    */
    public boolean acceptedIncomingConnection() {
    	return acceptor.get().acceptedIncoming();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#setListeningPort(int)
     */
    public void setListeningPort(int port) throws IOException {
        acceptor.get().setListeningPort(port);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#canReceiveUnsolicited()
     */
    public boolean canReceiveUnsolicited() {
    	return udpService.get().canReceiveUnsolicited();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#canReceiveSolicited()
     */
    public boolean canReceiveSolicited() {
    	return udpService.get().canReceiveSolicited();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#canDoFWT()
     */
    public boolean canDoFWT() {
        return udpService.get().canDoFWT();
    }
    
    public int getStableUDPPort() {
        return udpService.get().getStableUDPPort();
    }

    public GUID getSolicitedGUID() {
        return udpService.get().getSolicitedGUID();
    }

    public int supportsFWTVersion() {
        return udpService.get().canDoFWT() ? RUDPUtils.VERSION : 0;
    }
    
    public boolean isPrivateAddress(byte[] addr) {
        return networkInstanceUtils.isPrivateAddress(addr);
    }
    
    /** Disables TLS for this session. */
    private void setTLSNotSupported(Throwable reason) {
        tlsSupported = false;
        if(reason != null) {
            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            reason.printStackTrace(pw);
            pw.flush();
            tlsDisabledReason = writer.getBuffer().toString();
        } else {
            tlsDisabledReason = null;
        }
    }
    
    /** Returns true if TLS is disabled for this session. */
    public boolean isTLSSupported() {
        return tlsSupported;
    }
    
    /** Whether or not incoming TLS is allowed. */
    public boolean isIncomingTLSEnabled() {
        return tlsSupported && SSLSettings.TLS_INCOMING.getValue();
    }

    public void setIncomingTLSEnabled(boolean enabled) {
        SSLSettings.TLS_INCOMING.setValue(enabled);
    }

    /** Whether or not outgoing TLS is allowed. */
    public boolean isOutgoingTLSEnabled() {
        return tlsSupported && SSLSettings.TLS_OUTGOING.getValue();
    }

    public void setOutgoingTLSEnabled(boolean enabled) {
        SSLSettings.TLS_OUTGOING.setValue(enabled);
    }

    private class FWTChangeListener implements SettingListener {
        public void settingChanged(SettingEvent evt) {
            if (evt.getEventType() == SettingEvent.EventType.VALUE_CHANGED)
                updateCapabilities();
        }
    }

    public void addListener(EventListener<AddressEvent> listener) {
        listeners.addListener(listener);
    }

    public boolean removeListener(EventListener<AddressEvent> listener) {
        return listeners.removeListener(listener);
    }
    
    private void fireEvent(AddressEvent event) {
        listeners.broadcast(event);
    }
    
    private static class SSLSettings extends LimeProps {
    
        private SSLSettings() {}
        
        /** Whether or not we want to accept incoming TLS connections. */
        public static final BooleanSetting TLS_INCOMING =
            FACTORY.createBooleanSetting("TLS_INCOMING", true);
        
        /** Whether or not we want to make outgoing connections with TLS. */
        public static final BooleanSetting TLS_OUTGOING =
            FACTORY.createBooleanSetting("TLS_OUTGOING", true);
        
        /** False if we want to report exceptions in TLS handling. */
        public static final BooleanSetting IGNORE_SSL_EXCEPTIONS =
            FACTORY.createRemoteBooleanSetting("IGNORE_SSL_EXCEPTIONS", true, "TLS.ignoreException");
    
    }
}
