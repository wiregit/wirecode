package com.limegroup.gnutella.xml;

import com.limegroup.gnutella.URN;
import java.io.File;

/**
 * A simple MetaFileManager.
 * Currently, overrides no methods.
 */
public class MFMStub extends MetaFileManager {

    public URN readFromMap(Object file) {
        URN hash = null;
        try {
            hash = URN.createSHA1Urn((File)file);
        }
        catch (Exception ignored) {}
        return hash;
    }

}
