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
        updateSimppSettings(simppSettings, true);
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
        synchronized(_simppProps) {
            Set set = _simppProps.entrySet();
            for(Iterator iter = set.iterator(); iter.hasNext() ; ) {
                Map.Entry currEntry = (Map.Entry)iter.next();
                String simppSetting = (String)currEntry.getKey();
                Object simppValue = (Object)currValue.getValue();
                //get the setting we want based on the name of the setting
                Setting currSetting = findSettingByName(simppSetting);
                //get the default/current value and cache it                
                String defaultValue = (String)currSetting.getValueAsString();
                _defaults.put(simppSetting, defaultValue);
                //we never want to write this setting out
                currSetting.setAlwaysSave(false);
                //set the setting to the value that simpp says
                setSettingsByType(currSetting, simppValue);
            }
        }//end of synchronized block
        _isDefault = false;
    }
    
    public void revertToDefaults() {
        synchronized(_simppProps) {
            Set set = _simppProps.keySet();
            for(Iterator iter = set.iterator(); iter.hasNext() ; ) {
                Object currEntry = iter.next();
                String defaultValue = _defaults.get(currEntry);
                Setting currSetting = findSettingByName();
                setSettingsByType(currSetting, defaultValue);
            }            
        } //end of synchronized 
        _isDefault = true;
    }


    /////////////////////////////private helpers////////////////////////////

    private Setting findSettingByName(String settingName) {
        //TODO1: get this setting
    }

    private void setSettingByType(Setting toSet, Object value) {
        //TODO: Find out what kind of setting it is an set the value
        //accordingly
    }

}
