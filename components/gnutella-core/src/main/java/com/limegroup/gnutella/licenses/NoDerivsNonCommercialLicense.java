package com.limegroup.gnutella.licenses;

/**
 * Our abstraction for a Creative Commons NoDerivs-NonCommercial License.
 * This license does NOT require Attribution and allows for NonCommercial Use
 * only and does NOT allow Modifications.
 */
public final class NoDerivsNonCommercialLicense extends CreativeCommonsLicense {

    public NoDerivsNonCommercialLicense() throws IllegalArgumentException {
        super(false, true, true, false);
    }

}
