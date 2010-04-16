package org.limewire.setting.jmx;

import java.io.File;

import javax.management.MXBean;

import org.limewire.setting.FileSetting;

/**
 * A JMX Bean for {@link FileSetting}s
 */
@MXBean
public interface FileSettingBean extends SettingBean {

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
    
    public static class Impl extends AbstractSettingBean<FileSetting> 
            implements FileSettingBean {
        
        public Impl(FileSetting setting) {
            super(setting);
        }

        @Override
        public String getValue() {
            return setting.getValueAsString();
        }

        @Override
        public void setValue(String value) {
            setting.set(new File(value));
        }
    }
}
