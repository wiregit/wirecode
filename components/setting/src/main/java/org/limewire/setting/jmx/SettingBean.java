package org.limewire.setting.jmx;

import java.io.File;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.ByteSetting;
import org.limewire.setting.FileSetting;
import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;
import org.limewire.setting.Setting;
import org.limewire.setting.StringSetting;

/**
 * A JMX Bean interface for {@link Setting}s.
 */
public interface SettingBean {

    /**
     * Returns the type of the {@link Setting}.
     */
    public String getType();
    
    /**
     * Returns the value of the {@link Setting}
     */
    public String getValue();
    
    /**
     * Sets the value of the {@link Setting}
     */
    public void setValue(String value);
    
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
    
    /**
     * An implementation of {@link SettingBean}
     */
    public static class Impl implements SettingBean {
        
        private final Setting setting;
        
        public Impl(Setting setting) {
            this.setting = setting;
        }
        
        @Override
        public String getType() {
            return setting.getClass().getName();
        }

        @Override
        public String getValue() {
            return setting.getValueAsString();
        }

        @Override
        public void setValue(String value) {
            if (setting instanceof BooleanSetting) {
                ((BooleanSetting)setting).setValue(Boolean.parseBoolean(value));
            } else if (setting instanceof ByteSetting) {
                ((ByteSetting)setting).setValue(Byte.parseByte(value));
            } else if (setting instanceof IntSetting) {
                ((IntSetting)setting).setValue(Integer.parseInt(value));
            } else if (setting instanceof FloatSetting) {
                ((FloatSetting)setting).setValue(Float.parseFloat(value));
            } else if (setting instanceof LongSetting) {
                ((LongSetting)setting).setValue(Long.parseLong(value));
            } else if (setting instanceof FileSetting) {
                ((FileSetting)setting).set(new File(value));
            } else if (setting instanceof StringSetting) {
                ((StringSetting)setting).set(value);
            }
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
}
