package com.limegroup.gnutella.settings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.simpp.SimppManager;

public class SimppSettingsManager {

    private static final Log LOG = LogFactory.getLog(SimppSettingsManager.class);
    
    /**
     * The properties we crete from the string we get via simpp message
     */
    private Properties _simppProps;

    /**
     * A cache of the values we had for the settings before the simpp settings
     * were applied to them.  
     * <p>
     * Note: This is a utility added to allow LimeWire to revert to non-simpp
     * settings, ie. the user pref settings. 
     * Note 2: See note in revertToUserPrefs method
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
    private boolean _usingUserPrefs;
     
    /**
     *  The instance
     */
    private static SimppSettingsManager INSTANCE;

    //constructor
    private SimppSettingsManager() {
        _usingUserPrefs = true; //we are using defualt settings by default
        String simppSettings = SimppManager.instance().getPropsString();
        if(simppSettings == null)
            throw new IllegalArgumentException("SimppManager unexpected state");
        _userPrefs = new HashMap();
        _remainderSimppSettings = new HashMap();
        updateSimppSettings(simppSettings);
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
    public void updateSimppSettings(String simppSettings) {
        byte[] settings = null;
        try {            
            settings = simppSettings.getBytes("UTF-8");
        } catch (UnsupportedEncodingException uex) {
            ErrorService.error(uex);
            return;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(settings);
        _simppProps = new Properties();
        try {
            _simppProps.load(bais);
        } catch(IOException iox) {
            LOG.error("IOX reading simpp properties", iox);
            return;
        }
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
                //not loaded this setting yet. Let's cache the value in a
                //hashmap which will be referenced everytime a setting is
                //created
                if(simppSetting == null) {//remember it for later
                    _remainderSimppSettings.put(settingKey, simppValue);
                    continue;
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
                //set the setting to the value that simpp says
                simppSetting.setValue(simppValue);
            }
        }//end of synchronized block
        _usingUserPrefs = false;
    }
    
    /**
     * Call this method if you want to restore the values of the settings the
     * activateSimppSettings method set. 
     * 
     * Note: As of now, nothing will cause this method to be called, we could
     * save a little memory by not having this method, and not having the
     * _userPrefs map around, but it may be useful...who knows where this code
     * will go...
     */
    public void revertToUserPrefs() {
        if(_usingUserPrefs) //we are already at default values
            return;
        synchronized(_simppProps) {
            Set set = _simppProps.keySet();
            for(Iterator iter = set.iterator(); iter.hasNext() ; ) {
                Setting currSetting = (Setting)iter.next();
                String userSetValue = (String)_userPrefs.get(currSetting);
                currSetting.loadValue(userSetValue);
            }            
        } //end of synchronized 
        _usingUserPrefs = true;
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
