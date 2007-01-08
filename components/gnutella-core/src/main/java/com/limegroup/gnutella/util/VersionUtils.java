package com.limegroup.gnutella.util;

import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.version.Version;
import com.limegroup.gnutella.version.VersionFormatException;

/**
 * Common methods for comparing versions.
 */
public class VersionUtils {

    private VersionUtils() {}
    
    /**
     * Determines if Java is above the given version.
     */
    public static boolean isJavaAbove(String version) {
        try {
            Version java = new Version(CommonUtils.getJavaVersion());
            Version given = new Version(version);
            return java.compareTo(given) == 1;
        } catch(VersionFormatException vfe) {
            return false;
        }
    }

}
