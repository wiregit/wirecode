package com.limegroup.gnutella.settings;

import java.io.File;

import org.limewire.setting.BasicSettings;
import org.limewire.util.CommonUtils;


public class LimeWireSettings extends BasicSettings {

    public LimeWireSettings(String filename, String header) {
        super(new File(CommonUtils.getUserSettingsDir(), filename), header);
    }
    
}
