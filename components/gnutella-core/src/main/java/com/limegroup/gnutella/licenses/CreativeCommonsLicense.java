package com.limegroup.gnutella.licenses;

/**
 * Our abstraction for a Creative Commons license.  This class is abstract
 * because we have specific subclasses which define rights.  This class does
 * have code, like Message and VendorMessage, that allows one to get a
 * subclass.  It also sets up the rights framework, as expected.....
 * @see http://www.creativecommons.org
 */
public abstract class CreativeCommonsLicense {

    private final boolean _allowsAttribution;
    private final boolean _allowsNoncommercialUse;
    private final boolean _disallowsDerivateWorks;
    private final boolean _isShareAlike;

    /**
     * Does some sanity checking for the license.
     */
    protected CreativeCommonsLicense(boolean allowsAttribution,
                                     boolean allowsNoncommercialUse,
                                     boolean disallowsDerivativeWorks,
                                     boolean isShareAlike) 
        throws IllegalArgumentException {
        
        if (disallowsDerivativeWorks && isShareAlike)
            throw new IllegalArgumentException("Can't disallow derivative works if you Share Alike!!");
        
        _allowsAttribution = allowsAttribution;
        _allowsNoncommercialUse = allowsNoncommercialUse;
        _disallowsDerivateWorks = disallowsDerivativeWorks;
        _isShareAlike = isShareAlike;
    }


    /** @return true if You let others copy, distribute, display, and perform 
     *  your copyrighted work and derivative works based upon it but only if 
     *  they give you credit. 
     */
    public boolean allowsAttribution() { return _allowsAttribution; }
    
    /** @return true if You let others copy, distribute, display, and perform 
     *  your work and derivative works based upon it but for noncommercial 
     *  purposes only. 
     */
    public boolean allowsNoncommercialUse() { return _allowsNoncommercialUse; }

    /** @return true if You let others copy, distribute, display, and perform 
     *  only verbatim copies of your work, not derivative works based upon it. 
     */
    public boolean disallowsDerivativeWorks() { return _disallowsDerivateWorks; }

    /** @return true if You allow others to distribute derivative works only 
     *  under a license identical to the license that governs your work. 
     */
    public boolean isShareAlike() { return _isShareAlike; }

}
