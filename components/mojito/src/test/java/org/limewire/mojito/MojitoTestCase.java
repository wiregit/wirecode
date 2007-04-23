package org.limewire.mojito;

import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.LocalSocketAddressService;
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
