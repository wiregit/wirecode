package com.limegroup.gnutella;

import java.io.File;

public class NoOpSaveLocationManager implements SaveLocationManager {

    public boolean isSaveLocationTaken(File candidateFile) {
        return false;
    }

}
