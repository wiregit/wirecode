package com.limegroup.gnutella.settings;

import java.util.*;
import java.io.*;

public class SimppProps {
    
    private static SimppProps INSTANCE;
    
    private Properties _properties;
    
    private SimppProps() {
        //Note: We could have stored the properties in a file, but then we would
        //have had to reconcile the settings that are in the file, and those
        //that are in the loadDefaults. It's better to not have a file, and
        //always use loadDefaults.
        _properties = new Properties();
        loadDefaults();
    }
    
    public static synchronized SimppProps instance() {
        if(INSTANCE == null)
            INSTANCE = new SimppProps();
        return INSTANCE;
    }
    
    public String getClassNameForKey(String key) {
        return (String)_properties.get(key);

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
        _properties.put("test_upload","UploadSettings.TEST_UPLOAD_SETTING");
        _properties.put("soft_max", "UploadSettings.SOFT_MAX_UPLOADS");
    }

}
