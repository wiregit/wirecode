package com.limegroup.gnutella.licenses;

import com.limegroup.gnutella.xml.XMLStringUtils;
import com.limegroup.gnutella.metadata.AudioMetaData;

public final class CCConstants {
    
    /** 
     * The string that is inserted into QRP & goes out in license queries
     * when searching for Creative Commons licenses.
     */
    public static final String CC_URI_PREFIX = "creativecommons.org/licenses/";
    
    /**
     * The string that indicates all subsequent information is the URL where the
     * CC license is stored.
     */
    public static final String URL_INDICATOR = "verify at";
}
