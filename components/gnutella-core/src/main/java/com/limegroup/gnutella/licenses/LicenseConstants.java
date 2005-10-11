package com.limegroup.gnutella.licenses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.limegroup.gnutella.metadata.WRMXML;
import com.limegroup.gnutella.metadata.WeedInfo;

/**
 * various constants for the different licenses to be inserted in QRT
 */
public class LicenseConstants {
    
    public static final int NO_LICENSE = 0;
    public static final int CC_LICENSE = 1;
    public static final int WEED_LICENSE = 2;
    public static final int UNKNOWN_LICENSE = 3;
    
    /** The indivisible keywords for a CC license. */
    private static final List CC_INDIVISIBLE;
    static {
        List l = new ArrayList(1);
        l.add(CCConstants.CC_URI_PREFIX);
        CC_INDIVISIBLE = Collections.unmodifiableList(l);
    }
    
    /** The indivisible keywords for a Weed license. */
    private static final List WEED_INDIVISIBLE;
    static {
        List l = new ArrayList(1);
        l.add(WeedInfo.LAINFO);
        WEED_INDIVISIBLE = Collections.unmodifiableList(l);
    }
    
    public static List getIndivisible(int type) {
        
        switch(type) {
        case NO_LICENSE: return Collections.EMPTY_LIST;
        case WEED_LICENSE: return WEED_INDIVISIBLE;
        case CC_LICENSE: return CC_INDIVISIBLE;
        case UNKNOWN_LICENSE: return Collections.EMPTY_LIST; // not searchable.
        default: return Collections.EMPTY_LIST;
        }
    }
    
    /**
     * Determines the license type based on the a license type and the actual license
     */
    public static int determineLicenseType(String license, String type) {
        if (hasCCLicense(license, type))
            return CC_LICENSE;
        if (hasWeedLicense(type))
            return WEED_LICENSE;
        if (hasUnknownLicense(type))
            return UNKNOWN_LICENSE;
        return NO_LICENSE;
    }
    
    private static boolean hasCCLicense(String license, String type) {
        return (type != null && type.equals(CCConstants.CC_URI_PREFIX)) ||
               (license != null && license.indexOf(CCConstants.CC_URI_PREFIX) != -1
                                && license.indexOf(CCConstants.URL_INDICATOR) != -1)
               ;
    }

    private static boolean hasWeedLicense(String type) {
        return type != null &&
               type.startsWith(WeedInfo.LAINFO) &&
               type.indexOf(WeedInfo.VID) != -1 &&
               type.indexOf(WeedInfo.CID) != -1;
    }
    
    private static boolean hasUnknownLicense(String type) {
        return type != null &&
               type.startsWith(WRMXML.PROTECTED);
    }
}
