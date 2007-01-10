package org.limewire.mojito;

import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.LocalSocketAddressService;
import org.limewire.util.BaseTestCase;

public abstract class MojitoTestCase extends BaseTestCase {
    
    static {
        LocalSocketAddressService.setSocketAddressProvider(new LocalSocketAddressProvider() {
            public byte[] getLocalAddress() {
                return null;
            }

            public int getLocalPort() {
                return -1;
            }

            public boolean isLocalAddressPrivate() {
                return false;
            }
        });
    }

    protected MojitoTestCase(String name) {
        super(name);
    }
    
    
    

}
