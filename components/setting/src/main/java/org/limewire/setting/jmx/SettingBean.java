package org.limewire.setting.jmx;

import org.limewire.setting.Setting;

/**
 * A base interface for JMX Bean {@link Setting}s.
 * 
 * <p>NOTE: JMX does not support inheritance when it comes to
 * defining Bean interfaces. We must therefore re-declare each
 * method in the actual Bean interface.
 * 
 * @see BooleanSettingBean
 */
interface SettingBean {

    /**
     * Reverts the current value to default
     */
    public void revertToDefault();
    
    /**
     * Returns true if the current value is the default value
     */
    public boolean isDefault();
    
    /**
     * Returns true if the current value is private
     */
    public boolean isPrivate();
    
    /**
     * Sets weather or not the current value is private
     */
    public void setPrivate(boolean value);
    
    /**
     * Determines whether or not this value should always be saved to disk.
     */
    public boolean isShouldAlwaysSave();
    
    /**
     * Returns the key of the {@link Setting}
     */
    public String getKey();
    
    /**
     * Reloads the {@link Setting}'s value from the properties
     */
    public void reload();
}
