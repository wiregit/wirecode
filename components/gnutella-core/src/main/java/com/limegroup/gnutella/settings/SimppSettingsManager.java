package com.limegroup.gnutella.settings;

import java.util.*;
import java.io.*;
import com.limegroup.gnutella.simpp.*;
import com.limegroup.gnutella.*;


public class SimppSettingsManager {
    
    /**
     * The properties we crete from the string we get via simpp message
     */
    private Properties _simppProps;

    /**
     * A cache of the values we had for the settings before the simpp settings
     * were applied to them
     */
    private final HashMap /* String -> String*/ _defaults;
    
    /**
     * true if we have not applied the simpp settings, or have since reverted to
     * them, false otherwise
     */
    private boolean _isDefault;
    
    /**
     *  The instance
     */
    private static SimppSettingsManager INSTANCE;
    
    //constructor
    private SimppSettingsManager() {
        String simppSettings = SimppManager.instance().getPropsString();        
        if(simppSettings == null || simppSettings.equals(""))
            throw new IllegalArgumentException("SimppManager not ready");
        _defaults = new HashMap();
        updateSimppSettings(simppSettings, true);
    }

    //instance 
    public static SimppSettingsManager instance() {
        if(INSTANCE == null)
            INSTANCE = new SimppSettingsManager();
        return INSTANCE;
    }
    
    /**
     * Call this method with the verified simppSettings which are used to
     * replace other settings if they exist in the system.
     */
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

    /**
     * Call this method if you want to activate the settings to the ones in
     * this.simppProps
     */
    public void activateSimppSettings() {
        System.out.println("activating new settings");
        synchronized(_simppProps) {
            Set set = _simppProps.entrySet();
            for(Iterator iter = set.iterator(); iter.hasNext() ; ) {
                Map.Entry currEntry = (Map.Entry)iter.next();
                String simppSetting = (String)currEntry.getKey();
                String simppValue = (String)currEntry.getValue();
                System.out.println("setting:"+simppSetting);
                System.out.println("simpp value:"+simppValue);
                //get the setting we want based on the name of the setting
                Setting currSetting = findSettingByName(simppSetting);
                if(currSetting == null) //create a new Setting
                    currSetting = makeSettingPerType(simppSetting, simppValue);
                //get the default/current value and cache it                
                String defaultValue = (String)currSetting.getValueAsString();
                System.out.println("current value:"+defaultValue);
                _defaults.put(simppSetting, defaultValue);
                //we never want to write this setting out
                currSetting.setAlwaysSave(false);
                //set the setting to the value that simpp says
                setSettingByType(currSetting, simppValue);
            }
        }//end of synchronized block
        _isDefault = false;
    }
    
    /**
     * Call this method if you want to restore the values of the settings the
     * activateSimppSettings method set
     */
    public void revertToDefaults() {
        synchronized(_simppProps) {
            Set set = _simppProps.keySet();
            for(Iterator iter = set.iterator(); iter.hasNext() ; ) {
                String currEntry = (String)iter.next();
                String defaultValue = (String)_defaults.get(currEntry);
                Setting currSetting = findSettingByName(currEntry);
                setSettingByType(currSetting, defaultValue);
            }            
        } //end of synchronized 
        _isDefault = true;
    }


    /////////////////////////////private helpers////////////////////////////

    private Setting findSettingByName(String settingName) {
        LimeProps limeProps = LimeProps.instance();
        return limeProps.getSetting(settingName);
    }

    private void setSettingByType(Setting toSet, String value) {
        //TODO: Find out what kind of setting it is an set the value
        //accordingly
    }

    private Setting makeSettingPerType(String settingKey, String settingValue) {
        //TODO1: Implement this method
        return null;
    }

}
