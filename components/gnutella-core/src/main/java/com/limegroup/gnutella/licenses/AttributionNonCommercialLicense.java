package com.limegroup.gnutella.licenses;

import com.limegroup.gnutella.Assert;

/**
 * Our abstraction for a Creative Commons AttributionNonCommercialLicense.
 * This license requires Attribution and allows for NonCommercial Use and
 * Modifications that DO NOT have to be shared under the same license.
 */
public final class AttributionNonCommercialLicense 
    extends CreativeCommonsLicense {

    public AttributionNonCommercialLicense() throws IllegalArgumentException {
        super(true, true, false, false);
    }

}
