package org.limewire.activation.serial;

import java.io.File;

import org.limewire.util.CommonUtils;

class ActivationSerializerSettingsImpl implements ActivationSerializerSettings {

    private static final String backupModules = "activationModules.bak";
    private static final String saveModules = "activationModules.dat";
    
    @Override
    public File getBackupFile() {
        return new File(CommonUtils.getUserSettingsDir(), backupModules);
    }

    @Override
    public File getSaveFile() {
        return new File(CommonUtils.getUserSettingsDir(), saveModules);
    }
}
