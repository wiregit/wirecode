package com.limegroup.gnutella.licenses;

/**
 * Our abstraction for a Creative Commons 
 * AttributionNonCommercialShareAlikeLicense.
 * This license requires Attribution and allows for NonCommercial Use ONLY and
 * Modifications that DO have to be shared under the same license.
 */
public final class AttributionNonCommercialShareAlikeLicense 
    extends CreativeCommonsLicense {

    public AttributionNonCommercialShareAlikeLicense() 
        throws IllegalArgumentException {
        super(true, true, false, true);
    }

}
