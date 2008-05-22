package com.limegroup.gnutella;

import java.io.IOException;
import java.util.Properties;

import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.rudp.RUDPUtils;
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
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SearchSettings;
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
    
    private final EventListenerList<NetworkManagerEvent> listeners =
        new EventListenerList<NetworkManagerEvent>();
    
    @Inject
    public NetworkManagerImpl(Provider<UDPService> udpService,
            Provider<Acceptor> acceptor,
            Provider<DHTManager> dhtManager,
            Provider<ConnectionManager> connectionManager,
            Provider<ActivityCallback> activityCallback,
            OutOfBandStatistics outOfBandStatistics,
            NetworkInstanceUtils networkInstanceUtils,
            Provider<CapabilitiesVMFactory> capabilitiesVMFactory) {
        this.udpService = udpService;
        this.acceptor = acceptor;
        this.dhtManager = dhtManager;
        this.connectionManager = connectionManager;
        this.activityCallback = activityCallback;
        this.outOfBandStatistics = outOfBandStatistics;
        this.networkInstanceUtils = networkInstanceUtils;
        this.capabilitiesVMFactory = capabilitiesVMFactory;
    }
    

    public void start() {
        ConnectionSettings.LAST_FWT_STATE.addSettingListener(fwtListener);
    }


    public void stop() {
        ConnectionSettings.LAST_FWT_STATE.removeSettingListener(fwtListener);
    }
    
    public void initialize() {
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
        
        fireEvent(new NetworkManagerEvent(this, EventType.ADDRESS_CHANGE));
        
        return true;
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
    
    private class FWTChangeListener implements SettingListener {
        public void settingChanged(SettingEvent evt) {
            if (evt.getEventType() == SettingEvent.EventType.VALUE_CHANGED)
                updateCapabilities();
        }
    }

    public void addListener(EventListener<NetworkManagerEvent> listener) {
        listeners.addListener(listener);
    }

    public boolean removeListener(EventListener<NetworkManagerEvent> listener) {
        return listeners.removeListener(listener);
    }
    
    private void fireEvent(NetworkManagerEvent event) {
        listeners.broadcast(event);
    }
}
