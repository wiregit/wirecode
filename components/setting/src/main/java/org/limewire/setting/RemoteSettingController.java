package org.limewire.setting;

/**
 * A controller that is set on RemoteSettingManager.
 * 
 * The controller is set on the manager when the manager is
 * set on the SettingsFactory.  The controller is meant to be
 * used by the Manager in order to retrieve values from the
 * factory.
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
