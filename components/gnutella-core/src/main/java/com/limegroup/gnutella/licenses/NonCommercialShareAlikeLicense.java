package com.limegroup.gnutella.licenses;

/**
 * Our abstraction for a Creative Commons NonCommercialShareAlike License.
 * This license does NOT require Attribution and allows for NonCommercial Use
 * only and does allow Modifications that must be shared under the same License.
 */
public final class NonCommercialShareAlikeLicense 
    extends CreativeCommonsLicense {

    public NonCommercialShareAlikeLicense() throws IllegalArgumentException {
        super(false, true, false, true);
    }

}
