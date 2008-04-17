package com.limegroup.gnutella.version;

import java.io.IOException;

import org.limewire.io.IOUtils;
import org.limewire.security.SignatureVerifier;

import com.google.inject.Singleton;

/**
 * An implementation of UpdateMessageVerifier that uses
 * SignatureVerifier.getVerifiedData(data, publicKey, "DSA", "SHA1") to verify,
 * where public key is the key.
 */
@Singleton
public class UpdateMessageVerifierImpl implements UpdateMessageVerifier {

    /** The public key. */
    private final String KEY = "GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMAGKQF4ZHEOIG3ZEQPBBGLRNPGPJYF3B2YXD44YQ3TQPICPKFQO5TLPDWSCCUL4YHWYMLO43FRT4L5EBICT7J4EWUVSFFUHB244HFDWEKWD3LV3XXCZONDFTFKGNUUKQJTKWJA7GF3DNDEUHWEECM4WW2HMRYOGQWMYQVXWB3BC7GUNEXRQOYWWBTDWSOLI73KZRDUGND52UVJG";
    
    
    public String getVerifiedData(byte[] data) {
        return SignatureVerifier.getVerifiedData(data, KEY, "DSA", "SHA1");
    }
    
    public byte[] inflateNetworkData(byte[] input) throws IOException {
        return IOUtils.inflate(input);
    }

}
