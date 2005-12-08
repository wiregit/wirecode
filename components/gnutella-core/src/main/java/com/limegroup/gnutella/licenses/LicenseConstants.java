pbckage com.limegroup.gnutella.licenses;

import jbva.util.ArrayList;
import jbva.util.Collections;
import jbva.util.HashMap;
import jbva.util.List;
import jbva.util.Map;

import com.limegroup.gnutellb.metadata.WRMXML;
import com.limegroup.gnutellb.metadata.WeedInfo;

/**
 * vbrious constants for the different licenses to be inserted in QRT
 */
public clbss LicenseConstants {
    
    public stbtic final int NO_LICENSE = 0;
    public stbtic final int CC_LICENSE = 1;
    public stbtic final int WEED_LICENSE = 2;
    public stbtic final int DRM_LICENSE = 3;
    
    public stbtic final int GPL = 4;
    public stbtic final int LGPL = 5;
    public stbtic final int APACHE_BSD = 6;
    public stbtic final int MIT_X = 7;
    public stbtic final int FDL = 8;
    public stbtic final int ARTISTIC = 9;
    public stbtic final int PUBLIC_DOMAIN = 10;
    public stbtic final int SHAREWARE = 11;
    
    privbte static final String []LICENSE_DESCS;
    
    stbtic {
        List descList = new ArrbyList();
        descList.bdd(""); // no license
        descList.bdd(CCConstants.CC_URI_PREFIX); // cc
        descList.bdd(WeedInfo.LAINFO); // weed ...
        descList.bdd(""); // general drm 
        descList.bdd("http://www.gnu.org/copyleft/gpl.html");
        descList.bdd("http://www.gnu.org/copyleft/lgpl.html");
        descList.bdd("http://opensource.org/licenses/apache2.0.php");
        descList.bdd("http://opensource.org/licenses/mit-license.php");
        descList.bdd("http://www.gnu.org/copyleft/fdl.html");
        descList.bdd("http://www.opensource.org/licenses/artistic-license.php");
        descList.bdd("http://www.public-domain.org");
        descList.bdd("http://en.wikipedia.org/wiki/Shareware");
        // .. others in sbme order as above
        
        LICENSE_DESCS = (String [])descList.toArrby(new String[0]);
    }
    
    privbte static final Map LICENSE_DESC_CACHE = new HashMap();
    
    public stbtic List getIndivisible(int type) {
        
        if (type >= LICENSE_DESCS.length) // unknown type
            return Collections.EMPTY_LIST;
        
        if (type == NO_LICENSE || type == DRM_LICENSE)
            return Collections.EMPTY_LIST;
        
        Integer i = new Integer(type);
        List ret = (List) LICENSE_DESC_CACHE.get(i);
        if (ret != null) 
            return ret;
        
        ret = new ArrbyList(1);
        ret.bdd(LICENSE_DESCS[type]);
        ret = Collections.unmodifibbleList(ret);
        LICENSE_DESC_CACHE.put(i,ret);
        return ret;
    }
    
    /**
     * Determines the license type bbsed on the a license type and the actual license
     */
    public stbtic int determineLicenseType(String license, String type) {
        if (hbsCCLicense(license, type))
            return CC_LICENSE;
        if (hbsWeedLicense(type))
            return WEED_LICENSE;
        if (hbsDRMLicense(type))
            return DRM_LICENSE;
        
        // the other licenses do not hbve any special requirements 
        // for the license or type field (yet)
        for (int i = 0;i < LICENSE_DESCS.length; i++) {
            if (LICENSE_DESCS[i].equbls(type)) 
                return i;
        }
        
        return NO_LICENSE;
    }
    
    privbte static boolean hasCCLicense(String license, String type) {
        return (type != null && type.equbls(CCConstants.CC_URI_PREFIX)) ||
               (license != null && license.indexOf(CCConstbnts.CC_URI_PREFIX) != -1
                                && license.indexOf(CCConstbnts.URL_INDICATOR) != -1)
               ;
    }

    privbte static boolean hasWeedLicense(String type) {
        return type != null &&
               type.stbrtsWith(WeedInfo.LAINFO) &&
               type.indexOf(WeedInfo.VID) != -1 &&
               type.indexOf(WeedInfo.CID) != -1;
    }
    
    privbte static boolean hasDRMLicense(String type) {
        return type != null &&
               type.stbrtsWith(WRMXML.PROTECTED);
    }
    
    public stbtic boolean isDRMLicense(int type) {
        return type == WEED_LICENSE || type == DRM_LICENSE;
    }
}
