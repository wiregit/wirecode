package org.limewire.setting;

/**
 * Defines an interface for a class to remotely change the value for a key-value
 * pair.
 * <p> 
 * You can set the controller through 
 * {@link RemoteSettingManager#setRemoteSettingController(RemoteSettingController)}
 */
public interface RemoteSettingController {
    /**
     * Updates the setting that uses this remoteKey to the
     * correct value.  If no setting is loaded for this
     * remoteKey, this returns false.  Otherwise, this returns
     * true (even if the setting couldn't be updated).
     */
    public boolean updateSetting(String remoteKey, String value);
    
}
