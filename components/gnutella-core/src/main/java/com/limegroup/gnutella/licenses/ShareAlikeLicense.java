package com.limegroup.gnutella.licenses;

/**
 * Our abstraction for a Creative Commons ShareAlike License.
 * This license does NOT require Attribution and allows for Commercial Use
 * and does allow Modifications that must be shared under the same License.
 */
public final class ShareAlikeLicense extends CreativeCommonsLicense {

    public ShareAlikeLicense() throws IllegalArgumentException {
        super(false, false, false, true);
    }

}
