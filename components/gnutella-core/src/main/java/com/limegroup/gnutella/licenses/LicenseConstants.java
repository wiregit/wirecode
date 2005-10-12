package com.limegroup.gnutella.licenses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.limegroup.gnutella.metadata.WRMXML;
import com.limegroup.gnutella.metadata.WeedInfo;

/**
 * various constants for the different licenses to be inserted in QRT
 */
public class LicenseConstants {
    
    public static final int NO_LICENSE = 0;
    public static final int CC_LICENSE = 1;
    public static final int WEED_LICENSE = 2;
    public static final int DRM_LICENSE = 3;
    
    public static final int GPL = 4;
    public static final int LGPL = 5;
    public static final int APACHE_BSD = 6;
    public static final int MIT_X = 7;
    public static final int FDL = 8;
    public static final int ARTISTIC = 9;
    public static final int PUBLIC_DOMAIN = 10;
    public static final int SHAREWARE = 11;
    
    private static final String []LICENSE_DESCS;
    
    static {
        List descList = new ArrayList();
        descList.add(""); // no license
        descList.add(CCConstants.CC_URI_PREFIX); // cc
        descList.add(WeedInfo.LAINFO); // weed ...
        descList.add(""); // general drm 
        descList.add("http://www.gnu.org/copyleft/gpl.html");
        descList.add("http://www.gnu.org/copyleft/lgpl.html");
        descList.add("http://opensource.org/licenses/apache2.0.php");
        descList.add("http://opensource.org/licenses/mit-license.php");
        descList.add("http://www.gnu.org/copyleft/fdl.html");
        descList.add("http://www.opensource.org/licenses/artistic-license.php");
        descList.add("http://www.public-domain.org");
        descList.add("http://en.wikipedia.org/wiki/Shareware");
        // .. others in same order as above
        
        LICENSE_DESCS = (String [])descList.toArray(new String[0]);
    }
    
    private static final Map LICENSE_DESC_CACHE = new HashMap();
    
    public static List getIndivisible(int type) {
        
        if (type >= LICENSE_DESCS.length) // unknown type
            return Collections.EMPTY_LIST;
        
        if (type == NO_LICENSE || type == DRM_LICENSE)
            return Collections.EMPTY_LIST;
        
        Integer i = new Integer(type);
        List ret = (List) LICENSE_DESC_CACHE.get(i);
        if (ret != null) 
            return ret;
        
        ret = new ArrayList(1);
        ret.add(LICENSE_DESCS[type]);
        ret = Collections.unmodifiableList(ret);
        LICENSE_DESC_CACHE.put(i,ret);
        return ret;
    }
    
    /**
     * Determines the license type based on the a license type and the actual license
     */
    public static int determineLicenseType(String license, String type) {
        if (hasCCLicense(license, type))
            return CC_LICENSE;
        if (hasWeedLicense(type))
            return WEED_LICENSE;
        if (hasDRMLicense(type))
            return DRM_LICENSE;
        
        // the other licenses do not have any special requirements 
        // for the license or type field (yet)
        for (int i = 0;i < LICENSE_DESCS.length; i++) {
            if (LICENSE_DESCS[i].equals(type)) 
                return i;
        }
        
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
    
    private static boolean hasDRMLicense(String type) {
        return type != null &&
               type.startsWith(WRMXML.PROTECTED);
    }
    
    public static boolean isDRMLicense(int type) {
        return type == WEED_LICENSE || type == DRM_LICENSE;
    }
}
