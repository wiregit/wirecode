package com.limegroup.gnutella.licenses;

/**
 * Our abstraction for a Creative Commons NonCommercial License.
 * This license does NOT require Attribution and allows for NonCommercial Use
 * only and does allow Modifications.
 */
public final class NonCommercialLicense extends CreativeCommonsLicense {

    public NonCommercialLicense() throws IllegalArgumentException {
        super(false, true, false, false);
    }

}
