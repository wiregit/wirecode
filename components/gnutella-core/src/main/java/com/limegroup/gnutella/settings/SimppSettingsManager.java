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
    private final HashMap /* Setting -> String*/ _userPrefs;

    /**
     * A mapping of simppKeys to simppValues which have not been initialized
     * yet. Newly created settings must check with this map to see if they
     * should load defualt value or the simpp value
     */
    private final HashMap /* String -> String */ _remainderSimppSettings;
    
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
        _userPrefs = new HashMap();
        _remainderSimppSettings = new HashMap();
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
        synchronized(_simppProps) {
            Set set = _simppProps.entrySet();
            for(Iterator iter = set.iterator(); iter.hasNext() ; ) {
                Map.Entry currEntry = (Map.Entry)iter.next();
                String settingKey = (String)currEntry.getKey();
                Setting simppSetting = getSimppSettingForKey(settingKey);
                String simppValue = (String)currEntry.getValue();
                //If this setting is null, it means that the SettingsFactory has
                //not loaded this setting yet. We need to force it's hand. 
                if(simppSetting == null) {//remember it for later
                    _remainderSimppSettings.put(settingKey, simppValue);
                }
                if(LOG.isDebugEnabled()) {
                    LOG.debug("setting:"+simppSetting);
                    LOG.debug("simpp value:"+simppValue);
                }
                if(!simppSetting.isSimppEnabled())
                    continue;
                //get the default/current value and cache it                
                String userSetValue = (String)simppSetting.getValueAsString();
                if(LOG.isDebugEnabled())
                    LOG.debug("current value:"+userSetValue);
                _userPrefs.put(simppSetting, userSetValue);
                //we never want to write this setting out
                simppSetting.setAlwaysSave(false);
                //set the setting to the value that simpp says
                simppSetting.setValue(simppValue);
            }
        }//end of synchronized block
        _isDefault = false;
    }
    
    /**
     * Call this method if you want to restore the values of the settings the
     * activateSimppSettings method set
     */
    public void revertToUserPrefs() {
        if(_isDefault) //we are already at default values
            return;
        synchronized(_simppProps) {
            Set set = _simppProps.keySet();
            for(Iterator iter = set.iterator(); iter.hasNext() ; ) {
                Setting currSetting = (Setting)iter.next();
                String userSetValue = (String)_userPrefs.get(currSetting);
                currSetting.loadValue(userSetValue);
            }            
        } //end of synchronized 
        _isDefault = true;
    }

    /**
     * @return the simpp value for a simppkey from the map that remembers simpp
     * settings which have not been loaded yet. Removes the entry from the
     * mapping since it is no longer needed, now that the setting has been
     * created.
     */
    String getRemanentSimppValue(String simppKey) {
        synchronized(_simppProps) {
            return (String)_remainderSimppSettings.remove(simppKey);
        }
    }

    /** 
     * Appends the setings and userPref to the map holding the cached user
     * preferecnces
     */
    void cacheUserPref(Setting setting, String userPref) {
        synchronized(_simppProps) {
            _userPrefs.put(setting, userPref);
        }
    }
    

    /////////////////////////////private helpers////////////////////////////

    private Setting getSimppSettingForKey(String simppKey) {
        LimeProps limeProps = LimeProps.instance();
        return limeProps.getSimppSetting(simppKey);
    }

}
