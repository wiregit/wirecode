package org.limewire.setting.jmx;

import javax.management.MXBean;

import org.limewire.setting.FloatSetting;

/**
 * A JMX Bean for {@link FloatSetting}s
 */
@MXBean
public interface FloatSettingBean extends SettingBean {

    /**
     * Sets the current value
     */
    public void setValue(float value);
    
    /**
     * Returns the current value
     */
    public float getValue();
    
    @Override
    public void revertToDefault();
    
    @Override
    public boolean isDefault();
    
    @Override
    public boolean isPrivate();
    
    @Override
    public void setPrivate(boolean value);
    
    @Override
    public boolean isShouldAlwaysSave();
    
    @Override
    public String getKey();
    
    @Override
    public void reload();
    
    public static class Impl extends AbstractSettingBean<FloatSetting> 
            implements FloatSettingBean {
        
        public Impl(FloatSetting setting) {
            super(setting);
        }

        @Override
        public float getValue() {
            return setting.getValue();
        }

        @Override
        public void setValue(float value) {
            setting.setValue(value);
        }
    }
}
