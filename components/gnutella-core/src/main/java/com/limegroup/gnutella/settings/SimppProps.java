package com.limegroup.gnutella.settings;

import java.util.*;
import java.io.*;

public class SimppProps {
    
    private static SimppProps INSTANCE;
    
    /**
     * Maps simpp key to the string form of  the Setting it represents.
     */
    private Map /*String -> String */ _keyToSettingClass;
    
    private SimppProps() {
        _keyToSettingClass = new HashMap();
        loadDefaults();
    }
    
    public static synchronized SimppProps instance() {
        if(INSTANCE == null)
            INSTANCE = new SimppProps();
        return INSTANCE;
    }
    
    public String getClassNameForKey(String key) {
        return (String)_keyToSettingClass.get(key);

    }
    
    //////////////////////////private helpers/////////////////////
    /**
     * The default properties for mapping simpp-key to Settings class that the
     * simpp-key corresponds to.
     * <p>
     * IMPORTANT: If you ever add a simpp-enabled setting you must add an entry
     * here, to make sure we are able to identify the setting from a key, so all
     * the settings that are simpp-enabled must be listed here with their keys.
     */
    private void loadDefaults() {
        _keyToSettingClass.put("test_upload",
                               "UploadSettings.TEST_UPLOAD_SETTING");
        _keyToSettingClass.put("soft_max", "UploadSettings.SOFT_MAX_UPLOADS");
    }

}
