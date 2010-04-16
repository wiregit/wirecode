package org.limewire.setting.jmx;

import javax.management.MXBean;

import org.limewire.setting.StringSetting;

/**
 * A JMX Bean for {@link StringSetting}s
 */
@MXBean
public interface StringSettingBean extends SettingBean {

    /**
     * Sets the current value
     */
    public void setValue(String value);
    
    /**
     * Returns the current value
     */
    public String getValue();
    
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
    
    public static class Impl extends AbstractSettingBean<StringSetting> 
            implements StringSettingBean {
        
        public Impl(StringSetting setting) {
            super(setting);
        }

        @Override
        public String getValue() {
            return setting.get();
        }

        @Override
        public void setValue(String value) {
            setting.set(value);
        }
    }
}
