package org.limewire.mojito;

import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.LocalSocketAddressService;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.settings.MojitoProps;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.util.BaseTestCase;

public abstract class MojitoTestCase extends BaseTestCase {
    
    protected MojitoTestCase(String name) {
        super(name);
    }
    
    /**
     * Called statically before any settings.
     */
    public static void beforeAllTestsSetUp() throws Throwable {
        MojitoProps.instance().revertToDefault();
    }
    
    public void preSetUp() throws Exception {
        super.preSetUp();
        
        MojitoProps.instance().revertToDefault();
        
        // DHT Settings
        ContextSettings.SHUTDOWN_MESSAGES_MULTIPLIER.setValue(0);
        
        NetworkSettings.FILTER_CLASS_C.setValue(false);
        NetworkSettings.LOCAL_IS_PRIVATE.setValue(false);
        
        // We're working on the loopback. Everything should be done
        // in less than 500ms
        NetworkSettings.DEFAULT_TIMEOUT.setValue(500);
        
        // Nothing should take longer than 1.5 seconds. If we start seeing
        // LockTimeoutExceptions on the loopback then check this Setting!
        ContextSettings.WAIT_ON_LOCK.setValue(1500);
        
        LocalSocketAddressService.setSocketAddressProvider(new LocalSocketAddressProvider() {
            public byte[] getLocalAddress() {
                throw new UnsupportedOperationException("Mojito does not use this method and if it does implement it!");
            }

            public int getLocalPort() {
                throw new UnsupportedOperationException("Mojito does not use this method and if it does implement it!");
            }

            public boolean isLocalAddressPrivate() {
                return NetworkSettings.LOCAL_IS_PRIVATE.getValue();
            }

            public boolean isTLSCapable() {
                throw new UnsupportedOperationException("Mojito does not use this method and if it does implement it!");
            }
        });
    }
    
    @Override
    public void postTearDown() {
        super.postTearDown();
        setLocalIsPrivate(true);
    }
    
    public void setLocalIsPrivate(boolean localIsPrivate) {
        NetworkSettings.LOCAL_IS_PRIVATE.setValue(localIsPrivate);
        NetworkSettings.FILTER_CLASS_C.setValue(localIsPrivate);
    }
}
