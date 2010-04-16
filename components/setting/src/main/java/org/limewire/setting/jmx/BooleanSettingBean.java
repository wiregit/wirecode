package org.limewire.setting.jmx;

import javax.management.MXBean;

import org.limewire.setting.BooleanSetting;

/**
 * A JMX Bean for {@link BooleanSetting}s
 */
@MXBean
public interface BooleanSettingBean extends SettingBean {

    /**
     * Sets the current value
     */
    public void setValue(boolean value);
    
    /**
     * Returns the current value
     */
    public boolean getValue();
    
    /**
     * Inverts the current value
     */
    public void invert();
    
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
    
    public static class Impl extends AbstractSettingBean<BooleanSetting> 
            implements BooleanSettingBean {
        
        public Impl(BooleanSetting setting) {
            super(setting);
        }

        @Override
        public boolean getValue() {
            return setting.getValue();
        }

        @Override
        public void setValue(boolean value) {
            setting.setValue(value);
        }
        
        @Override
        public void invert() {
            setting.invert();
        }
    }
}
