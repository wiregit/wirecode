package com.limegroup.gnutella;

import java.io.File;

public interface SaveLocationManager {

    boolean isSaveLocationTaken(File candidateFile);

}
