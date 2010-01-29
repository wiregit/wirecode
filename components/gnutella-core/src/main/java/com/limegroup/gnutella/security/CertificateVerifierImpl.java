package com.limegroup.gnutella.security;

import java.security.SignatureException;

import org.limewire.security.SignatureVerifier;

public class CertificateVerifierImpl implements CertificateVerifier {

    private static final String PUBLIC_MASTER_KEY= "GCBADOBQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQUAAFAMBADRAMTBWOA43OM6DE3H4U3OYATOWNG7EO4G52MWL4W2SXBQQAG5AKFI2GGOUL7HCH734WYYX33TT27MPJWOO424YA4LK3HWHNKJB66R736VBH2ATYTK223AVJP2LNZUIRDGHU2CDAYQ2O6SEO3SLIX5DYHITRPJX7JMX6IMKRLBTOSOZMDU2VRJXG5K3JNPNAIYR5CPLYINZK7A";
    
    private final String base32MasterKey;
    
    public CertificateVerifierImpl() {
        this(PUBLIC_MASTER_KEY);
    }
    
    public CertificateVerifierImpl(String base32MasterKey) {
        this.base32MasterKey = base32MasterKey;
    }
    
    @Override
    public Certificate verify(Certificate certificate) throws SignatureException {
        SignatureVerifier signatureVerifier = new SignatureVerifier(certificate.getSignedPayload(), certificate.getSignature(), SignatureVerifier.readKey(base32MasterKey, "DSA"), "DSA");
        if (!signatureVerifier.verifySignature()) {
            throw new SignatureException("Invalid signature for: " + certificate);
        }
        return certificate;
    }

}
