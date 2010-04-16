package org.limewire.setting.jmx;

import javax.management.MXBean;

import org.limewire.setting.LongSetting;

/**
 * A JMX Bean for {@link LongSetting}s
 */
@MXBean
public interface LongSettingBean extends SettingBean {

    /**
     * Sets the current value
     */
    public void setValue(long value);
    
    /**
     * Returns the current value
     */
    public long getValue();
    
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
    
    public static class Impl extends AbstractSettingBean<LongSetting> 
            implements LongSettingBean {
        
        public Impl(LongSetting setting) {
            super(setting);
        }

        @Override
        public long getValue() {
            return setting.getValue();
        }

        @Override
        public void setValue(long value) {
            setting.setValue(value);
        }
    }
}
