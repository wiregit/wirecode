package com.limegroup.gnutella.xml;

/**
 * A simple MetaFileManager.
 * Currently, overrides no methods.
 */
public class MFMStub extends MetaFileManager {

    public String readFromMap(Object file, boolean audio) {
        String hash = null;
        try {
            hash = new String(LimeXMLUtils.hashFile((java.io.File)file));
        }
        catch (Exception ignored) {}
        return hash;
    }

}
