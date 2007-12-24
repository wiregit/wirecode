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
    private final String KEY = "GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJC"
            + "SUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2"
            + "NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6"
            + "FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7"
            + "AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25"
            + "NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4"
            + "N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMANJHPNL2"
            + "K3FJIH54PPBPLMCHVEAVTDQQSU3GKB3N2WG7RDC4WSWCM3HACQJ3MNHJ32STPGSZJCTYZRPCHJORQR4HN2"
            + "J4KXHJ6JYYLTIBM64EKRTDBVLTWFJDEIC5SYR24CTHM3H3"
            + "NTBHY4AB26LFPFYMOSK3O4BACF2I4GCRGUPNJS6XGTSNU33APRHI2BJ7ZDJTTU5C4EI6DY";
    
    
    public String getVerifiedData(byte[] data) {
        return SignatureVerifier.getVerifiedData(data, KEY, "DSA", "SHA1");
    }
    
    public byte[] inflateNetworkData(byte[] input) throws IOException {
        return IOUtils.inflate(input);
    }

}
