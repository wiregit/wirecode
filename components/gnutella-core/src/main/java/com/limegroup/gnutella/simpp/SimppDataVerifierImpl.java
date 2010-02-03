package com.limegroup.gnutella.simpp;

import java.security.PublicKey;
import java.security.SignatureException;

import org.limewire.security.SignatureVerifier;
import org.limewire.util.Base32;
import org.limewire.util.StringUtils;

public class SimppDataVerifierImpl implements SimppDataVerifier {
    
    //private static final Log LOG = LogFactory.getLog(SimppDataVerifierImpl.class);

    private static final String DEFAULT_PUBLIC_KEY = "GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMAAHNFDNZU6UKXDJP5N7NGWAD2YQMOU23C5IRAJHNHHSDJQITAY3BRZGMUONFNOJFR74VMICCOS4UNEPZMDA46ACY5BCGRSRLPGU3XIIXZATSCOL5KFHWGOJZCZUAVFHHQHENYOIJVGFSFULPIXRK2AS45PHNNFCYCDLHZ4SQNFLZN43UIVR4DOO6EYGYP2QYCPLVU2LJXW745S";
    
    private static final byte SEP = (byte)124;

    private final String ALGORITHM = "DSA";
    private final String DIG_ALG = "SHA1";


    private final String publicKey;

    /**
     * Constructor. Payload contains bytes with the following format:
     * [Base32 Encoded signature bytes]|[versionnumber|<props in xml format>]
     */
    public SimppDataVerifierImpl() {
        this(DEFAULT_PUBLIC_KEY);
    }
    
    public SimppDataVerifierImpl(String publicKey) {
        this.publicKey = publicKey;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.simpp.Sim#extractSignedData(byte[])
     */
    public byte[] extractSignedData(byte[] simppPayload) throws SignatureException {    
        int sepIndex = findSeperator(simppPayload);
        if(sepIndex < 0) //no separator? this cannot be the real thing.
            throw new SignatureException("no separator found");
        byte[] temp = new byte[sepIndex];
        System.arraycopy(simppPayload, 0, temp, 0, sepIndex);
        String base32 = StringUtils.getUTF8String(temp);

        byte[] signature = Base32.decode(base32);
        byte[] propsData = new byte[simppPayload.length-1-sepIndex];
        System.arraycopy(simppPayload, sepIndex+1, propsData, 
                                           0, simppPayload.length-1-sepIndex);
        
        PublicKey pk = SignatureVerifier.readKey(publicKey, ALGORITHM);
        if(pk == null)
            throw new SignatureException("invalid public key");

        SignatureVerifier verifier = 
                         new SignatureVerifier(propsData, signature, pk, ALGORITHM, DIG_ALG);
        if (verifier.verifySignature()) {
            return propsData;
        } else {
            throw new SignatureException("invalid payload or signature");
        }
    }

    static int findSeperator(byte[] data) {
        boolean found = false;
        int i = 0;
        for( ; i< data.length; i++) {
            if(data[i] == SEP) {
                found = true;
                break;
            }
        }
        if(found)
            return i;
        return -1;
    }

}
