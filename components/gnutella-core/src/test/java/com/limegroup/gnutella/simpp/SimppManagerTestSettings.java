package com.limegroup.gnutella.simpp;

import com.limegroup.gnutella.settings.LimeProps;
import com.limegroup.gnutella.settings.IntSetting;

class SimppManagerTestSettings extends LimeProps {

    public static final int MAX_SETTING = 20;
    public static final int MIN_SETTING = 3;
    public static final int DEFAULT_SETTING = 4;
    /**
     * A test SIMPP setting.
     */
    public static final IntSetting TEST_UPLOAD_SETTING = 
        FACTORY.createSettableIntSetting("TEST_UPLOAD_SETTING", DEFAULT_SETTING, 
                                         "test_upload", MIN_SETTING, MAX_SETTING);
    
}
