package com.limegroup.gnutella.licenses;

/**
 * Our abstraction for a Creative Commons AttributionNoDervisLicense.
 * This license requires Attribution and allows for Commercial Use but does
 * not allow for modifications to the content.
 */
public final class AttributionNoDerivsLicense 
    extends CreativeCommonsLicense {

    public AttributionNoDerivsLicense() throws IllegalArgumentException {
        super(true, false, true, false);
    }

}
