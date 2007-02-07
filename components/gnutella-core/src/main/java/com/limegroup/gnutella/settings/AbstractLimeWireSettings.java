package com.limegroup.gnutella.settings;

import java.io.File;

import org.limewire.setting.AbstractSettings;
import org.limewire.util.CommonUtils;


public abstract class AbstractLimeWireSettings extends AbstractSettings {

    public AbstractLimeWireSettings(String filename, String header) {
        super(new File(CommonUtils.getUserSettingsDir(), filename), header);
    }
    
}
