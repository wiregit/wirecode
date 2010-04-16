package org.limewire.setting.jmx;

import javax.management.MXBean;

import org.limewire.setting.ByteSetting;

/**
 * A JMX Bean for {@link ByteSetting}s
 */
@MXBean
public interface ByteSettingBean extends SettingBean {

    /**
     * Sets the current value
     */
    public void setValue(byte value);
    
    /**
     * Returns the current value
     */
    public byte getValue();
    
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
    
    public static class Impl extends AbstractSettingBean<ByteSetting> 
            implements ByteSettingBean {
        
        public Impl(ByteSetting setting) {
            super(setting);
        }

        @Override
        public byte getValue() {
            return setting.getValue();
        }

        @Override
        public void setValue(byte value) {
            setting.setValue(value);
        }
    }
}
