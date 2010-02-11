package org.limewire.activation.serial;

import java.io.File;

public class ActivationSerializerSettingsStub  implements ActivationSerializerSettings {

    private final File backupFile;
    private final File saveFile;
    
    public ActivationSerializerSettingsStub(File backup, File save) {
        this.backupFile = backup;
        this.saveFile = save;
    }
    
    @Override
    public File getBackupFile() {
        return backupFile;
    }

    @Override
    public File getSaveFile() {
        return saveFile;
    }
}
