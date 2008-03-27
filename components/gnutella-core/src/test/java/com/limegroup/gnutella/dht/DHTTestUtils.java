package com.limegroup.gnutella.dht;

import org.limewire.io.LocalSocketAddressService;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.PingPongSettings;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;

public class DHTTestUtils {

    public static void setSettings(int port) {
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
                new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
                new String[] {"127.*.*.*", "18.239.0.*"});
                
        ConnectionSettings.PORT.setValue(port);
        BaseTestCase.assertEquals("unexpected port", port, ConnectionSettings.PORT.getValue());
                
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        PingPongSettings.PINGS_ACTIVE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        
        // DHT Settings
        DHTSettings.PERSIST_ACTIVE_DHT_ROUTETABLE.setValue(false);
        DHTSettings.PERSIST_DHT_DATABASE.setValue(false);
        DHTSettings.ENABLE_PUSH_PROXY_QUERIES.setValue(true);
        ContextSettings.SHUTDOWN_MESSAGES_MULTIPLIER.setValue(0);
        
         // We're working on the loopback. Everything should be done
        // in less than 500ms
        NetworkSettings.DEFAULT_TIMEOUT.setValue(500);
        
        // Nothing should take longer than 1.5 seconds. If we start seeing
        // LockTimeoutExceptions on the loopback then check this Setting!
        ContextSettings.WAIT_ON_LOCK.setValue(1500);
        
    }

    public static void setLocalIsPrivate(boolean localIsPrivate) {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(localIsPrivate);
        NetworkSettings.FILTER_CLASS_C.setValue(localIsPrivate);
        NetworkSettings.LOCAL_IS_PRIVATE.setValue(false);
        LocalSocketAddressProviderStub localSocketAddressProviderStub = new LocalSocketAddressProviderStub();
        localSocketAddressProviderStub.setLocalAddressPrivate(localIsPrivate);
        LocalSocketAddressService.setSocketAddressProvider(localSocketAddressProviderStub);
    }

}
