package com.limegroup.gnutella.licenses;

/**
 * Our abstraction for a Creative Commons PublicDomain License.
 * There are no rights associated with this License.  It is free to use in any
 * form.
 */
public final class PublicDomainLicense 
    extends CreativeCommonsLicense {

    public PublicDomainLicense() throws IllegalArgumentException {
        super(false, false, false, false);
    }

}
