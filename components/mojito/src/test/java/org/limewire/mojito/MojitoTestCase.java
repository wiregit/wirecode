package org.limewire.mojito;

import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.LocalSocketAddressService;
import org.limewire.util.BaseTestCase;

public abstract class MojitoTestCase extends BaseTestCase {
    
    private static final MojitoLocalSocketAddressProvider PROVIDER 
        = new MojitoLocalSocketAddressProvider();
    
    protected MojitoTestCase(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LocalSocketAddressService.setSocketAddressProvider(PROVIDER);
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
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
    }
}
