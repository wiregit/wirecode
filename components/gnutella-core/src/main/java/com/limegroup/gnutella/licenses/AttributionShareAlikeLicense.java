package com.limegroup.gnutella.licenses;

import com.limegroup.gnutella.Assert;

/**
 * Our abstraction for a Creative Commons AttributionShareAlikeLicense.
 * This license requires Attribution and allows for Commercial Use and
 * Modifications that DO have to be shared under the same license.
 */
public final class AttributionShareAlikeLicense 
    extends CreativeCommonsLicense {

    public AttributionShareAlikeLicense() throws IllegalArgumentException {
        super(true, false, false, true);
    }

}
