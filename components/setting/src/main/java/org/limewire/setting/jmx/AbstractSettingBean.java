package org.limewire.setting.jmx;

import org.limewire.setting.Setting;

/**
 * A base class for JMX Bean {@link Setting}s.
 */
abstract class AbstractSettingBean<T extends Setting> implements SettingBean {

    protected final T setting;
    
    public AbstractSettingBean(T setting) {
        this.setting = setting;
    }
    
    @Override
    public void revertToDefault() {
        setting.revertToDefault();
    }
    
    @Override
    public boolean isDefault() {
        return setting.isDefault();
    }
    
    @Override
    public boolean isPrivate() {
        return setting.isPrivate();
    }
    
    @Override
    public void setPrivate(boolean value) {
        setting.setPrivate(value);
    }
    
    @Override
    public boolean isShouldAlwaysSave() {
        return setting.shouldAlwaysSave();
    }
    
    @Override
    public String getKey() {
        return setting.getKey();
    }
    
    @Override
    public void reload() {
        setting.reload();
    }
}
