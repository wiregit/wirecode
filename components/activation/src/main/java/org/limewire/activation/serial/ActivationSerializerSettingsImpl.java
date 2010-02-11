package org.limewire.activation.serial;

import java.io.File;

import org.limewire.util.CommonUtils;

class ActivationSerializerSettingsImpl implements ActivationSerializerSettings {

    @Override
    public File getBackupFile() {
        return new File(CommonUtils.getUserSettingsDir(), "activationModules.bak");
    }

    @Override
    public File getSaveFile() {
        return new File(CommonUtils.getUserSettingsDir(), "activationModules.dat");
    }
}
