pbckage com.limegroup.gnutella.licenses;

import jbva.net.URL;
import org.bpache.commons.httpclient.URI;
import com.limegroup.gnutellb.URN;

/**
 * An unknown license (unverifibble).
 */
public clbss UnknownLicense implements NamedLicense {
    privbte String name;
    
    /** Sets the license nbme. */
    public void setLicenseNbme(String name) { this.name = name; }
    
    public boolebn isVerified() { return false; }
    public boolebn isVerifying() { return false; }
    public boolebn isValid(URN urn) { return false; }
    public String getLicenseDescription(URN urn) { return null; }
    public URI getLicenseURI() { return null; }
    public URL getLicenseDeed(URN urn) { return null; }
    public String getLicense() { return null; }
    public void verify(VerificbtionListener listener) {}
    public long getLbstVerifiedTime() { return 0; }
    public String getLicenseNbme() { return name; }
    
    public License copy(String license, URI licenseURI) {
        throw new UnsupportedOperbtionException("no copying");
    }    
}