package com.limegroup.gnutella.settings;

import java.util.*;
import java.io.*;
import com.limegroup.gnutella.simpp.*;
import com.limegroup.gnutella.*;

import org.apache.commons.logging.*;

public class SimppSettingsManager {
    
    /**
     * The properties we crete from the string we get via simpp message
     */
    private Properties _simppProps;

    /**
     * A cache of the values we had for the settings before the simpp settings
     * were applied to them
     */
    private final HashMap /* Setting -> String*/ _defaults;
    
    /**
     * true if we have not applied the simpp settings, or have since reverted to
     * them, false otherwise
     */
    private boolean _isDefault;
     
    /**
     *  The instance
     */
    private static SimppSettingsManager INSTANCE;

    private static final Log LOG=LogFactory.getLog(SimppSettingsManager.class);
    
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
        LOG.debug("activating new settings");
        if(!_isDefault) //we are already activated...
            return;
        synchronized(_simppProps) {
            Set set = _simppProps.entrySet();
            for(Iterator iter = set.iterator(); iter.hasNext() ; ) {
                Map.Entry currEntry = (Map.Entry)iter.next();
                String settingKey = (String)currEntry.getKey();
                Setting simppSetting = getSimppSettingForKey(settingKey);
                //If this setting is null, it means that the SettingsFactory has
                //not loaded this setting yet. We need to force it's hand. 
                if(simppSetting == null) // load it
                    simppSetting = loadSetting(settingKey);
                if(simppSetting == null) {
                    //Something is amiss, either simpp message is malformed or
                    //the SimppProps has not got the key/value marked correctly
                    //Ignore this setting and keep going
                    continue;
                }
                //get the setting we want based on the name of the setting
                String simppValue = (String)currEntry.getValue();
                if(LOG.isDebugEnabled()) {
                    LOG.debug("setting:"+simppSetting);
                    LOG.debug("simpp value:"+simppValue);
                }

                if(!simppSetting.isSimppEnabled())
                    continue;
                //get the default/current value and cache it                
                String defaultValue = (String)simppSetting.getValueAsString();
                if(LOG.isDebugEnabled())
                    LOG.debug("current value:"+defaultValue);
                _defaults.put(simppSetting, defaultValue);
                //we never want to write this setting out
                simppSetting.setAlwaysSave(false);
                //set the setting to the value that simpp says
                simppSetting.loadValue(simppValue);
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
                Setting currSetting = (Setting)iter.next();
                String defaultValue = (String)_defaults.get(currSetting);
                currSetting.loadValue(defaultValue);
            }            
        } //end of synchronized 
        _isDefault = true;
    }


    /////////////////////////////private helpers////////////////////////////

    private Setting getSimppSettingForKey(String simppKey) {
        LimeProps limeProps = LimeProps.instance();
        return limeProps.getSimppSetting(simppKey);
    }

    /**
     * @param rawSetting has the form SETTING_NAME{setting_key} = setting_value
     */
    private Setting loadSetting(String simppKey) {
        LOG.debug("loadSetting called");
        String fullname = SimppProps.instance().getClassNameForKey(simppKey);
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

}
