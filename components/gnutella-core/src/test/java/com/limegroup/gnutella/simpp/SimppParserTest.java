package com.limegroup.gnutella.simpp;

import java.io.IOException;
import java.util.Locale;

import junit.framework.Test;

import org.limewire.util.Base32;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.security.Certificate;
import com.limegroup.gnutella.security.CertificateParserImpl;

public class SimppParserTest extends BaseTestCase {
    
    public SimppParserTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SimppParserTest.class);
    }
    
    public void testParseInfoAllLocales() throws Exception{
        for (Locale locale : Locale.getAvailableLocales()) {
            Locale.setDefault(locale);
            String certString = "GAWAEFAKBQQTJVLXAY2IRKTUUU27XD4GJHP3KRYCCR5ZLDRNCBUWEMDM42FKGA3GT6FKY3QBQE|19|GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMACHKDQHVN4BT2X3DZALDXR36BYVG3IJHTYGPZK2XAA225KH7HWSDBZTSRO57VHVPVAPM5DS5TCSDU4YNJWIC6DFRBGTCVZORZE7FZHAFF7EQXAF4BRXVW5JFTFPUCYVAKB2CF4QSBUZWPVAYYRPBDXPT2BJOFCUT2YBU7HP5CCKKEANYMS26MOV3WBGQN76G6RU7UGZKD7HFIE";
            String xml = "<SIMPP><VERSION>1</VERSION><KEYVERSION>19</KEYVERSION><NEWVERSION>3</NEWVERSION><SIGNATURE>SIGSIG</SIGNATURE><CERTIFICATE>"+certString+"</CERTIFICATE><PROPS>propsdata</PROPS></SIMPP>";
            SimppParser parser = new SimppParser(xml.getBytes("UTF-8"));
            assertEquals("Failed for locale: " + locale, 1, parser.getVersion());
            assertEquals("Failed for locale: " + locale, 19, parser.getKeyVersion());
            assertEquals("Failed for locale: " + locale, 3, parser.getNewVersion());
            assertEquals("Failed for locale: " + locale, "propsdata", parser.getPropsData());
            assertEquals("Failed for locale: " + locale, Base32.decode("SIGSIG"), parser.getSignature());
            assertEquals("Failed for locale: " + locale, 19, parser.getCertificate().getKeyVersion());
            
            xml = "<simpp><version>1</version><keyversion>19</keyversion><newversion>3</newversion><signature>SIGSIG</signature><certificate>"+certString+"</certificate><props>propsdata</props></simpp>";
            parser = new SimppParser(xml.getBytes("UTF-8"));
            assertEquals("Failed for locale: " + locale, 1, parser.getVersion());
            assertEquals("Failed for locale: " + locale, 19, parser.getKeyVersion());
            assertEquals("Failed for locale: " + locale, 3, parser.getNewVersion());
            assertEquals("Failed for locale: " + locale, "propsdata", parser.getPropsData());
            assertEquals("Failed for locale: " + locale, Base32.decode("SIGSIG"), parser.getSignature());
            assertEquals("Failed for locale: " + locale, 19, parser.getCertificate().getKeyVersion());
        }        
    }
    
    public void testParsesNewXMLElements() throws Exception {
        SimppParser parser = new SimppParser(StringUtils.toUTF8Bytes("<simpp><version>656</version><keyversion>1</keyversion><newversion>1</newversion><signature>GAWAEFBMGCFLMI5E5ACUNAQQ7AIWIP7JGBDUWCQCCR6MT6UIWBXIHJMZUGIT3IAKX2SKT55HEI</signature><certificate>GAWQEFIARSGMFXPFJZLZZMNQCYIT5VG66O7NWP6VAIKFJ6SZ2E2AJRHTD63UXONQT36SA5X7F7OA|1|GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMADGSCIYVEIUKQNN6A4D7B77A62EMIURPPHQE2ILP3GJLRTUSRSK2Q4IALFHHVMRA2HOERD25DUJXGX2IQDEAFWWLVG7FOHRG2Z3H4ER66UV2UVI7X5VG3UWGJ72ZW3V5V55ELJVJ52EAFCZW4WKUP2EAIUBDQYZ63LG5PFXYY5N2RHKJOU6UUAJZUV3VTGZNFDIU6G3BMMIIDG</certificate><props>propsdata</props></simpp>"));
        assertEquals(656, parser.getVersion());
        assertEquals(1, parser.getCertifiedMessage().getKeyVersion());
        assertEquals(1, parser.getNewVersion());
        assertEquals(Base32.decode("GAWAEFBMGCFLMI5E5ACUNAQQ7AIWIP7JGBDUWCQCCR6MT6UIWBXIHJMZUGIT3IAKX2SKT55HEI"), parser.getCertifiedMessage().getSignature());
        assertEquals(StringUtils.toUTF8Bytes("<simpp><version>656</version><keyversion>1</keyversion><newversion>1</newversion><certificate>GAWQEFIARSGMFXPFJZLZZMNQCYIT5VG66O7NWP6VAIKFJ6SZ2E2AJRHTD63UXONQT36SA5X7F7OA|1|GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMADGSCIYVEIUKQNN6A4D7B77A62EMIURPPHQE2ILP3GJLRTUSRSK2Q4IALFHHVMRA2HOERD25DUJXGX2IQDEAFWWLVG7FOHRG2Z3H4ER66UV2UVI7X5VG3UWGJ72ZW3V5V55ELJVJ52EAFCZW4WKUP2EAIUBDQYZ63LG5PFXYY5N2RHKJOU6UUAJZUV3VTGZNFDIU6G3BMMIIDG</certificate><props>propsdata</props></simpp>"), parser.getCertifiedMessage().getSignedPayload());
        Certificate certificate = new CertificateParserImpl().parseCertificate("GAWQEFIARSGMFXPFJZLZZMNQCYIT5VG66O7NWP6VAIKFJ6SZ2E2AJRHTD63UXONQT36SA5X7F7OA|1|GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMADGSCIYVEIUKQNN6A4D7B77A62EMIURPPHQE2ILP3GJLRTUSRSK2Q4IALFHHVMRA2HOERD25DUJXGX2IQDEAFWWLVG7FOHRG2Z3H4ER66UV2UVI7X5VG3UWGJ72ZW3V5V55ELJVJ52EAFCZW4WKUP2EAIUBDQYZ63LG5PFXYY5N2RHKJOU6UUAJZUV3VTGZNFDIU6G3BMMIIDG");
        assertEquals(certificate, parser.getCertifiedMessage().getCertificate());
        assertEquals("propsdata", parser.getPropsData());
    }
    
    public void testMissingSignatureFail() throws Exception{        
        String simppString = "<simpp><version>1</version><keyversion>2</keyversion><newversion>3</newversion><props>propsdata</props></simpp>";
        try {
            SimppParser simppParser = new SimppParser(StringUtils.toUTF8Bytes(simppString));
            fail("exception expected");
            assertEquals(-1, simppParser.getNewVersion());
            assertEquals(-1, simppParser.getKeyVersion());
            assertNull(SimppParser.SIGNATURE);
        } catch (IOException e) {
        }
    }
    public void testMissingNewVersionFail() throws Exception{        
        String simppString = "<simpp><version>1</version><keyversion>2</keyversion><signature>SIGSIG</signature><props>propsdata</props></simpp>";
        try {
            SimppParser simppParser = new SimppParser(StringUtils.toUTF8Bytes(simppString));
            fail("exception expected");
            assertEquals(-1, simppParser.getNewVersion());
            assertEquals(-1, simppParser.getKeyVersion());
            assertNull(SimppParser.SIGNATURE);
        } catch (IOException e) {            
        }
    }
    public void testMissingKeyVersionFail() throws Exception{        
        String simppString = "<simpp><version>1</version><newversion>3</newversion><signature>SIGSIG</signature><props>propsdata</props></simpp>";
        try {
            SimppParser simppParser = new SimppParser(StringUtils.toUTF8Bytes(simppString));
            fail("exception expected");
            assertEquals(-1, simppParser.getNewVersion());
            assertEquals(-1, simppParser.getKeyVersion());
            assertNull(SimppParser.SIGNATURE);
        } catch (IOException e) {            
        }
    }

}
