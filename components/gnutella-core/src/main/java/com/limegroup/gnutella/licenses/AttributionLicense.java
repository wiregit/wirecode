package com.limegroup.gnutella.licenses;

import com.limegroup.gnutella.Assert;

/**
 * Our abstraction for a Creative Commons AttributionLicense.
 * This license requires Attribution but allows for Commercial Use and
 * Modifications that DO NOT have to be shared.
 */
public final class AttributionLicense extends CreativeCommonsLicense {

    public AttributionLicense() throws IllegalArgumentException {
        super(true, false, false, true);
    }

}
