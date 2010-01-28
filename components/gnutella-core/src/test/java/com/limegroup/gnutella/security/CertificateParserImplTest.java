package com.limegroup.gnutella.security;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import junit.framework.Test;

import org.limewire.util.Base32;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

public class CertificateParserImplTest extends BaseTestCase {

    private CertificateParserImpl certificateParser;

    public static Test suite() {
        return buildTestSuite(CertificateParserImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        certificateParser = new CertificateParserImpl();
    }
    
    public void testExtractSignedPayloadFailsOnMissingPipeSymbol() {
        try {
            CertificateParserImpl.extractSignedPayload("ABCDE\\?///<..>>123434");
            fail("exception expected");
        } catch (IOException ie) {
        }
        try {
            CertificateParserImpl.extractSignedPayload("");
            fail("exception expected");
        } catch (IOException ie) {
        }
    }
    
    public void testExtractSignedPayload() throws Exception {
        assertEquals(StringUtils.toUTF8Bytes("ABCDDEFF98495039485"), CertificateParserImpl.extractSignedPayload("helloabcdde123434|ABCDDEFF98495039485"));
        assertEquals(StringUtils.toUTF8Bytes("|ABCDDEFF98495039485"), CertificateParserImpl.extractSignedPayload("helloabcdde123434||ABCDDEFF98495039485"));
    }
    
    public void testParseCertificateFailsOnInvalidInput() {
        try {
            certificateParser.parseCertificate("ABCDE");
            fail("expected exception, input doesn't have enough pipe symbols");
        } catch (IOException ie) {
        }
        try {
            certificateParser.parseCertificate("ABCDE||");
            fail("expected exception, input doesn't have enough pipe symbols");
        } catch (IOException ie) {
        }
        try {
            certificateParser.parseCertificate("ABCDE||||");
            fail("expected exception, input has too many pipe symbols");
        } catch (IOException ie) {
        }
        try {
            certificateParser.parseCertificate("ABCDE");
            fail("expected exception, input doesn't have enough pipe symbols");
        } catch (IOException ie) {
        }
    }
    
    public void testParseCertificate() throws Exception {
        String certificateString = "GAWQEFIARSGMFXPFJZLZZMNQCYIT5VG66O7NWP6VAIKFJ6SZ2E2AJRHTD63UXONQT36SA5X7F7OA|1|GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMADGSCIYVEIUKQNN6A4D7B77A62EMIURPPHQE2ILP3GJLRTUSRSK2Q4IALFHHVMRA2HOERD25DUJXGX2IQDEAFWWLVG7FOHRG2Z3H4ER66UV2UVI7X5VG3UWGJ72ZW3V5V55ELJVJ52EAFCZW4WKUP2EAIUBDQYZ63LG5PFXYY5N2RHKJOU6UUAJZUV3VTGZNFDIU6G3BMMIIDG";
        Certificate certificate = certificateParser.parseCertificate(certificateString);
        assertEquals(Base32.decode("GAWQEFIARSGMFXPFJZLZZMNQCYIT5VG66O7NWP6VAIKFJ6SZ2E2AJRHTD63UXONQT36SA5X7F7OA"), certificate.getSignature());
        assertEquals(1, certificate.getKeyVersion());
        assertEquals(StringUtils.toUTF8Bytes("1|GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMADGSCIYVEIUKQNN6A4D7B77A62EMIURPPHQE2ILP3GJLRTUSRSK2Q4IALFHHVMRA2HOERD25DUJXGX2IQDEAFWWLVG7FOHRG2Z3H4ER66UV2UVI7X5VG3UWGJ72ZW3V5V55ELJVJ52EAFCZW4WKUP2EAIUBDQYZ63LG5PFXYY5N2RHKJOU6UUAJZUV3VTGZNFDIU6G3BMMIIDG"), certificate.getSignedPayload());
        
        EncodedKeySpec spec = new X509EncodedKeySpec(Base32.decode("GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMADGSCIYVEIUKQNN6A4D7B77A62EMIURPPHQE2ILP3GJLRTUSRSK2Q4IALFHHVMRA2HOERD25DUJXGX2IQDEAFWWLVG7FOHRG2Z3H4ER66UV2UVI7X5VG3UWGJ72ZW3V5V55ELJVJ52EAFCZW4WKUP2EAIUBDQYZ63LG5PFXYY5N2RHKJOU6UUAJZUV3VTGZNFDIU6G3BMMIIDG"));
        PublicKey publicKey = KeyFactory.getInstance("DSA").generatePublic(spec);
        assertEquals(publicKey, certificate.getPublicKey());
        assertEquals(certificateString, certificate.getCertificateString());
    }
}
