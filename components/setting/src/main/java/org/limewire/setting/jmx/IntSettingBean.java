package org.limewire.setting.jmx;

import javax.management.MXBean;

import org.limewire.setting.IntSetting;

/**
 * A JMX Bean for {@link IntSetting}s
 */
@MXBean
public interface IntSettingBean extends SettingBean {

    /**
     * Sets the current value
     */
    public void setValue(int value);
    
    /**
     * Returns the current value
     */
    public int getValue();
    
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
    
    public static class Impl extends AbstractSettingBean<IntSetting> 
            implements IntSettingBean {
        
        public Impl(IntSetting setting) {
            super(setting);
        }

        @Override
        public int getValue() {
            return setting.getValue();
        }

        @Override
        public void setValue(int value) {
            setting.setValue(value);
        }
    }
}
