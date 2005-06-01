package com.limegroup.gnutella.licenses;

import java.net.URL;
import java.net.MalformedURLException;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A collection of constants & utilities for Creative Commons licenses
 */
public final class CCConstants {
    
    private static final Log LOG = LogFactory.getLog(CCLicense.class);    
    
    /** 
     * The string that is inserted into QRP & goes out in license queries
     * when searching for Creative Commons licenses.
     *
     * THIS CAN NEVER EVER CHANGE.
     * (And, if you really do change it for some reason, make sure
     *  that you update the value in the various .xsd files.)
     */
    public static final String CC_URI_PREFIX = "creativecommons.org/licenses/";
    
    /**
     * The string that indicates all subsequent information is the URL where the
     * CC license is stored.
     */
    public static final String URL_INDICATOR = "verify at";
    
    /**
     * Guesses a license deed URL from a license string.
     */
    static URL guessLicenseDeed(String license) {
        if(license == null)
            return null;
        
        // find where "creativecommons.org/licenses/" is.
        int idx = license.indexOf(CCConstants.CC_URI_PREFIX);
        if(idx == -1)
            return null;
        // find the "http://" before it.
        int httpIdx = license.lastIndexOf("http://", idx);
        if(httpIdx == -1)
            return null;
        // make sure that there's a space before it or it's the start.
        if(httpIdx != 0 && license.charAt(httpIdx-1) != ' ')
            return null;

        // find where the first space is after the http://.
        // if it's before the creativecommons.org part, that's bad.
        int spaceIdx = license.indexOf(" ", httpIdx);
        if(spaceIdx == -1)
            spaceIdx = license.length();
        else if(spaceIdx < idx)
            return null;
     
        try {       
            return new URL(license.substring(httpIdx, spaceIdx));
        } catch(MalformedURLException bad) {
            LOG.warn("Unable to create URL from license: " + license, bad);
            return null;
        }
    }
}
