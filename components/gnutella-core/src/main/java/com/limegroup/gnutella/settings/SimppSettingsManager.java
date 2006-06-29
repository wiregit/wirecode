package com.limegroup.gnutella.settings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
    private final Map<Setting, String> _userPrefs;

    /**
     * A mapping of simppKeys to simppValues which have not been initialized
     * yet. Newly created settings must check with this map to see if they
     * should load defualt value or the simpp value
     */
    private final Map<String, String> _remainderSimppSettings;
     
    /**
     *  The instance
     */
    private static SimppSettingsManager INSTANCE;

    //constructor
    private SimppSettingsManager() {
        String simppSettings = SimppManager.instance().getPropsString();
        if(simppSettings == null)
            throw new IllegalStateException("SimppManager unexpected state");
        _userPrefs = new HashMap<Setting, String>();
        _remainderSimppSettings = new HashMap<String, String>();
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
            for(Map.Entry<Object, Object> entry : _simppProps.entrySet()) {
                String settingKey = (String)entry.getKey();
                Setting simppSetting = getSimppSettingForKey(settingKey);
                String simppValue = (String)entry.getValue();
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
                String userSetValue = simppSetting.getValueAsString();
                if(LOG.isDebugEnabled())
                    LOG.debug("current value:"+userSetValue);
                _userPrefs.put(simppSetting, userSetValue);
                //set the setting to the value that simpp says
                simppSetting.setValue(simppValue);
            }
        }
    }
    
    /**
     * @return the simpp value for a simppkey from the map that remembers simpp
     * settings which have not been loaded yet. Removes the entry from the
     * mapping since it is no longer needed, now that the setting has been
     * created.
     */
    String getRemanentSimppValue(String simppKey) {
        synchronized(_simppProps) {
            return _remainderSimppSettings.remove(simppKey);
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
