package com.limegroup.gnutella.licenses;

/**
 * Our abstraction for a Creative Commons 
 * AttributionNoDervisNonCommercialLicense.
 * This license requires Attribution and allows for NonCommercial Use ONLY but 
 * does not allow for modifications to the content.
 */
public final class AttributionNoDerivsNonCommercialLicense 
    extends CreativeCommonsLicense {

    public AttributionNoDerivsNonCommercialLicense() 
        throws IllegalArgumentException {
        super(true, true, true, false);
    }

}
