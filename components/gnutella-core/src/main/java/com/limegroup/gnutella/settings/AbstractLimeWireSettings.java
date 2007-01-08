package com.limegroup.gnutella.settings;

import java.io.File;

import org.limewire.setting.AbstractSettings;

import com.limegroup.gnutella.util.LimeWireUtils;

public abstract class AbstractLimeWireSettings extends AbstractSettings {

    public AbstractLimeWireSettings(String filename, String header) {
        super(new File(LimeWireUtils.getUserSettingsDir(), filename), header);
    }
    
}
