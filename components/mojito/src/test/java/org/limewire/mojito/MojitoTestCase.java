package org.limewire.mojito;

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
    }
    
    @Override
    public void postTearDown() {
        super.postTearDown();
        setLocalIsPrivate(true);
    }
    
    public void setLocalIsPrivate(boolean isLocalPrivate) {
        NetworkSettings.LOCAL_IS_PRIVATE.setValue(isLocalPrivate);
        NetworkSettings.FILTER_CLASS_C.setValue(isLocalPrivate);
    }
}
