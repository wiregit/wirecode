package com.limegroup.gnutella.licenses;

import java.io.*;

/**
 * Our abstraction for a Creative Commons license.  This class is abstract
 * because we have specific subclasses which define rights.  This class does
 * have code, like Message and VendorMessage, that allows one to get a
 * subclass.  It also sets up the rights framework, as expected.....
 * @see http://www.creativecommons.org
 */
public abstract class CreativeCommonsLicense {

    private final boolean _requiresAttribution;
    private final boolean _allowsNoncommercialUseOnly;
    private final boolean _disallowsDerivateWorks;
    private final boolean _isShareAlike;

    /**
     * Does some sanity checking for the license.
     */
    protected CreativeCommonsLicense(boolean requiresAttribution,
                                     boolean allowsNoncommercialUseOnly,
                                     boolean disallowsDerivativeWorks,
                                     boolean isShareAlike) 
        throws IllegalArgumentException {
        
        if (disallowsDerivativeWorks && isShareAlike)
            throw new IllegalArgumentException("Can't disallow derivative works if you Share Alike!!");
        
        _requiresAttribution = requiresAttribution;
        _allowsNoncommercialUseOnly = allowsNoncommercialUseOnly;
        _disallowsDerivateWorks = disallowsDerivativeWorks;
        _isShareAlike = isShareAlike;
    }

    /** @return null if no license exists, or the specific subclass of a
     *  CreativeCommonsLicense.
     */
    public CreativeCommonsLicense deriveLicense(File file) {
        return null;
    }


    /** @return true if You let others copy, distribute, display, and perform 
     *  your copyrighted work and derivative works based upon it but only if 
     *  they give you credit. 
     */
    public boolean requiresAttribution() { return _requiresAttribution; }
    
    /** @return true if You let others copy, distribute, display, and perform 
     *  your work and derivative works based upon it but for noncommercial 
     *  purposes only. 
     */
    public boolean allowsNoncommercialUseOnly() { return _allowsNoncommercialUseOnly; }

    /** @return true if You let others copy, distribute, display, and perform 
     *  only verbatim copies of your work, not derivative works based upon it. 
     */
    public boolean disallowsDerivativeWorks() { return _disallowsDerivateWorks; }

    /** @return true if You allow others to distribute derivative works only 
     *  under a license identical to the license that governs your work. 
     */
    public boolean isShareAlike() { return _isShareAlike; }

}
