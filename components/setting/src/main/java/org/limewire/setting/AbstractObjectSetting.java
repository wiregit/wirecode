package org.limewire.setting;

import java.util.Properties;

/** A base class for all non-native settings. */
abstract class AbstractObjectSetting<T> extends AbstractSetting<T> {

    protected AbstractObjectSetting(Properties defaultProps, Properties props, String key,
            String defaultValue) {
        super(defaultProps, props, key, defaultValue);
    }
    
    @Override
    public T get() {
        return getValue();
    }
    
    @Override
    public void set(T newValue) {
        setValue(newValue);
    }
    
    /** Gets the current value of the setting. */
    public abstract T getValue();
    
    /** Sets the new value for this setting. */
    public abstract void setValue(T newValue);
    

}
