package com.limegroup.gnutella.settings;

import java.util.*;
import java.io.*;
import com.limegroup.gnutella.simpp.*;
import com.limegroup.gnutella.*;


public class SimppSettingsManager {
    
    private Properties _simppProps;

    private HashMap _defaults;
    
    private boolean _isDefault;
    
    private static SimppSettingsManager INSTANCE;
    
    private SimppSettingsManager() {
        String simppSettings = SimppManager.instance().getPropsString();
        if(simppSettings == null || simppSettings.equals(""))
            throw new IllegalArgumentException("SimppManager not ready");
        updateSimppSettings(simppSettings, false);
        _isDefault = true;//we have not yet shifted from the defaults
    }

    public static SimppSettingsManager instance() {
        if(INSTANCE == null)
            INSTANCE = new SimppSettingsManager();
        return INSTANCE;
    }
    
    public void updateSimppSettings(String simppSettings, boolean activate) {
        byte[] settings = null;
        try {
            settings = simppSettings.getBytes("UTF-8");
        } catch (UnsupportedEncodingException uex) {
            ErrorService.error(uex);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(settings);
        _simppProps = new Properties();
        try {
            _simppProps.load(bais);
        } catch(IOException iox) {
            ErrorService.error(iox);//huh? IOEx with a BAIS frm String?
        }
        if(activate)
            activateSimppSettings();
    }

    public void activateSimppSettings() {
        //TODO: save the old setting in the defaults map, and set the value in
        //the properties to the value in _simppProperties 

        //TODO: Make sure each setting has the setting so that it's not written
        //out to the file
        _isDefault = false;
    }
    
    public void revertToDefaults() {
        //TODO1: is this necessary?
        //replace the properties in defaults map in the settings handler, and
        //remove the other ones which are in simppProperties.
        _isDefault = true;
    }

}
