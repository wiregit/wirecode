package org.limewire.setting;

import java.util.Properties;

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

    @Override
    protected void loadValue(String sValue) {
        super.loadValue(sValue);
        value = Math.random() <= getValue();
    }
}
