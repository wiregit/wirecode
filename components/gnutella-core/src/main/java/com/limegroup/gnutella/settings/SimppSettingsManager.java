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
        _isDefault = true; //we are using defualt settings by default
        String simppSettings = SimppManager.instance().getPropsString();        
        if(simppSettings == null || simppSettings.equals(""))
            throw new IllegalArgumentException("SimppManager not ready");
        _defaults = new HashMap();
        updateSimppSettings(simppSettings, true);
    }

    //instance 
    public static synchronized SimppSettingsManager instance() {
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
            ErrorService.error(iox);//huh? IOEx with a BAIS from String?
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
        if(!_isDefault) //we are already activated...
            return;
        synchronized(_simppProps) {
            Set set = _simppProps.entrySet();
            for(Iterator iter = set.iterator(); iter.hasNext() ; ) {
                Map.Entry currEntry = (Map.Entry)iter.next();
                String rawSimppSettingStr = (String)currEntry.getKey();
                String simppSetting = getSettingString(rawSimppSettingStr);
                if(simppSetting == null) {//bad case, simpp message inconsistent
                    continue;//ignore this setting and move on
                    //TODO: Option 2 is to ignore this whole simpp message
                    //return; //TODO: Error service here?
                }
                String simppValue = (String)currEntry.getValue();
                System.out.println("Sumeet:setting:"+simppSetting);
                System.out.println("Sumeet:simpp value:"+simppValue);
                //get the setting we want based on the name of the setting
                Setting currSetting = findSettingByName(simppSetting);
                if(currSetting == null) {
                    //perhaps setting not touched with lazy loading, try load
                    //the setting
                    currSetting = loadSetting(rawSimppSettingStr);
                    if(currSetting == null) //Still null?
                        continue;//Perhaps this setting has been removed frm LW
                }
                if(!currSetting.isSimppEnabled())
                    continue;
                //get the default/current value and cache it                
                String defaultValue = (String)currSetting.getValueAsString();
                System.out.println("current value:"+defaultValue);
                _defaults.put(simppSetting, defaultValue);
                //we never want to write this setting out
                currSetting.setAlwaysSave(false);
                //set the setting to the value that simpp says
                currSetting.loadValue(simppValue);
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
                currSetting.loadValue(defaultValue);
            }            
        } //end of synchronized 
        _isDefault = true;
    }


    /////////////////////////////private helpers////////////////////////////

    private Setting findSettingByName(String settingName) {
        LimeProps limeProps = LimeProps.instance();
        return limeProps.getSetting(settingName);
    }

    private String getSettingString(String rawSetting) {
        //The raw string has the format settingStr{Classname.fieldname}
        int index = rawSetting.indexOf("{");
        if(index < 0) //we have a real serious problem
            return null;
        return rawSetting.substring(0,index);
    }
    

    /**
     * @param rawSetting has the form SETTING_NAME{setting_key} = setting_value
     */
    private Setting loadSetting(String rawSetting) {
        System.out.println("Sumeet: loading setting");
        String fullname = getSettingsClass(rawSetting);
        if(fullname == null) //simpp messasge badly formatted
            return null;
        int dot = fullname.indexOf(".");
        if(dot < 0) //simpp setting badly formatted
            return null;
        try {
            String classname = fullname.substring(0, dot);
            String fieldname = fullname.substring(dot+1);
            Class settingsClass = Class.forName(classname);
            Object obj = settingsClass.getField(fieldname).get(settingsClass);
            Setting ret = (Setting)obj;
            return ret;
        } catch (ClassNotFoundException cnfx) {
            return null;
        } catch (NoSuchFieldException nsfx) {
            return null;
        } catch(IllegalAccessException iax) {
            return null;
        }
    }

   private String getSettingsClass(String rawSetting) {
        int i = rawSetting.indexOf("{");
        int j = rawSetting.indexOf("}");
        if(i < 0 || j < 0) //we have a problem, the simpp message has bad format
            return null;
        String settingKey = rawSetting.substring(i+1, j);
        //the class from the settingkey from the simpp-settings properties
        return SimppProps.instance().getClassNameForKey(settingKey); 
    }

}
