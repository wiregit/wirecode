package com.limegroup.gnutella.licenses;

/**
 * Our abstraction for a Creative Commons AttributionLicense.
 * This license requires Attribution but allows for Commercial Use and
 * Modifications that DO NOT have to be shared under the same license.
 */
public final class AttributionLicense extends CreativeCommonsLicense {

    public AttributionLicense() throws IllegalArgumentException {
        super(true, false, false, false);
    }

}
