package org.limewire.mojito;

import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.LocalSocketAddressService;
import org.limewire.mojito.settings.MojitoProps;
import org.limewire.util.BaseTestCase;

public abstract class MojitoTestCase extends BaseTestCase {
    
    private static final MojitoLocalSocketAddressProvider PROVIDER 
        = new MojitoLocalSocketAddressProvider();
    
    protected MojitoTestCase(String name) {
        super(name);
    }
    
    /**
     * Called statically before any settings.
     */
    public static void beforeAllTestsSetUp() throws Throwable {
        MojitoProps.instance().revertToDefault();
        LocalSocketAddressService.setSocketAddressProvider(PROVIDER);
    }
    
    public void preSetUp() throws Exception {
        super.preSetUp();
        
        MojitoProps.instance().revertToDefault();
        LocalSocketAddressService.setSocketAddressProvider(PROVIDER);
    }
    
    @Override
    public void postTearDown() {
        super.postTearDown();
        setLocalIsPrivate(true);
    }
    
    public void setLocalIsPrivate(boolean isLocalPrivate) {
        PROVIDER.isLocalPrivate = isLocalPrivate;
    }
    
    private static class MojitoLocalSocketAddressProvider implements LocalSocketAddressProvider {
        
        private volatile boolean isLocalPrivate = true;
        
        public byte[] getLocalAddress() { 
            return null; 
        }
        
        public int getLocalPort() { 
            return -1; 
        }
        
        public boolean isLocalAddressPrivate() {
            return isLocalPrivate; 
        }
        
        public boolean isTLSCapable() {
            return false;
        }
    }
}
