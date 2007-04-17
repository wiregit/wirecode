package org.limewire.setting;

import java.util.Properties;

/**
 * A setting that holds a boolean value that is determined
 * based on a provided float probability. 
 */
public class ProbabilisticBooleanSetting extends FloatSetting {
    
    private volatile boolean value;

    public ProbabilisticBooleanSetting(Properties defaultProps, Properties props, String key, float defaultFloat, float min, float max) {
        super(defaultProps, props, key, defaultFloat, min, max);
    }

    public ProbabilisticBooleanSetting(Properties defaultProps, Properties props, String key, float defaultFloat) {
        super(defaultProps, props, key, defaultFloat);
    }
    
    public boolean getBoolean() {
        return value;
    }

    public void setBoolean(boolean b){
        setValue(b ? 1.0f : 0f);
    }
    
    @Override
    protected void loadValue(String sValue) {
        super.loadValue(sValue);
        value = Math.random() <= getValue();
    }
}
