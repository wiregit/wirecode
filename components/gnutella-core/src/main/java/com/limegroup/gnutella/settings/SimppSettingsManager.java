package com.limegroup.gnutella.settings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.service.ErrorService;
import org.limewire.setting.RemoteSettingController;
import org.limewire.setting.RemoteSettingManager;

/**
 * A RemoteSettingManager that uses SIMPP to remotely control the settings.
 * 
 * Before this can be used, you MUST call setRemoteSettingController,
 * otherwise activating any simpp settings will fail.
 * (Note: this is typically done by calling SettingsFactory.setRemoteSettingManager
 *        with this instance.)
 */
public class SimppSettingsManager implements RemoteSettingManager {

    private static final Log LOG = LogFactory.getLog(SimppSettingsManager.class);

    /**  The instance */
    private static SimppSettingsManager INSTANCE = new SimppSettingsManager();
    
    /** The properties we crete from the string we get via simpp message */
    private final Properties _simppProps = new Properties();

    /**
     * A mapping of simppKeys to simppValues which have not been initialized
     * yet. Newly created settings must check with this map to see if they
     * should load defualt value or the simpp value
     */
    private final Map<String, String> _remainderSimppSettings = new HashMap<String, String>();
    
    /** The controller used to set remote settings. */
    private volatile RemoteSettingController _remoteController;
    
    public static final SimppSettingsManager instance() {
        return INSTANCE;
    }
    
    /**
     * Call this method with the verified simppSettings which are used to
     * replace other settings if they exist in the system.
     */
    public synchronized void updateSimppSettings(String simppSettings) {
        byte[] settings = null;
        try {            
            settings = simppSettings.getBytes("UTF-8");
        } catch (UnsupportedEncodingException uex) {
            ErrorService.error(uex);
            return;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(settings);
        _simppProps.clear();
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
        synchronized(this) {
            if(_remoteController == null)
                throw new IllegalStateException("No RemoteSettingController set!");
            
            synchronized(_simppProps) {
                for(Map.Entry<Object, Object> entry : _simppProps.entrySet()) {
                    String key = (String)entry.getKey();
                    String value = (String)entry.getValue();
                    if(!_remoteController.updateSetting(key, value))
                        _remainderSimppSettings.put(key, value);
                }
            }
        }
    }
    
    /**
     * @return the simpp value for a simppkey from the map that remembers simpp
     * settings which have not been loaded yet. Removes the entry from the
     * mapping since it is no longer needed, now that the setting has been
     * created.
     */
    public synchronized String getUnloadedValueFor(String simppKey) {
        return _remainderSimppSettings.remove(simppKey);
    }

    public synchronized void setRemoteSettingController(RemoteSettingController controller) {
        _remoteController = controller;
    }

}
