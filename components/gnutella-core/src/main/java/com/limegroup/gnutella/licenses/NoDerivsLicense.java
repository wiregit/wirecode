package com.limegroup.gnutella.licenses;

/**
 * Our abstraction for a Creative Commons NoDerivsLicnese.
 * This license does NOT require Attribution and allows for Commercial Use but
 * does NOT allow Modifications.
 */
public final class NoDerivsLicense extends CreativeCommonsLicense {

    public NoDerivsLicense() throws IllegalArgumentException {
        super(false, false, true, false);
    }

}
