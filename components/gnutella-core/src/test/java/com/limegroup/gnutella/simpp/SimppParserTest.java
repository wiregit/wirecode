package com.limegroup.gnutella.simpp;

import java.util.Locale;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

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
            String xml = "<SIMPP><VERSION>334</VERSION><PROPS>test</PROPS></SIMPP>";
            
            SimppParser parser = new SimppParser(xml.getBytes("UTF-8"));
            assertEquals("Failed for locale: " + locale, 334, parser.getVersion());
            assertEquals("Failed for locale: " + locale, "test", parser.getPropsData());
            
            xml = "<simpp><version>334</version><props>test</props></simpp>";
            parser = new SimppParser(xml.toLowerCase(Locale.US).getBytes("UTF-8"));
            assertEquals("Failed for locale: " + locale, 334, parser.getVersion());
            assertEquals("Failed for locale: " + locale, "test", parser.getPropsData());
        }   
    }

    /**
     * Test to ensure that the simpp parser before the introduction of the new
     * simpp key scheme gracefully ignores new and unknown elements in the simpp
     * message. 
     */
    public void testOldParserIgnoresNewXMLElements() throws Exception {
        String simppWithExtraElements = "<simpp><version>333</version><newVersion>1</newVersion><cert>AKSDFklsddd|15|lSDFKK34555</cert><props>propsdata</props></simpp>";
        SimppParser parser = new SimppParser(StringUtils.toUTF8Bytes(simppWithExtraElements));
        assertEquals(333, parser.getVersion());
        assertEquals("propsdata", parser.getPropsData());
    }
    
    public void testParsesNewXMLElements() throws Exception {
        SimppParser parser = new SimppParser(StringUtils.toUTF8Bytes("<simpp><version>656</version><keyversion>1</keyversion><newversion>1</newversion><signature>GAWAEFBMGCFLMI5E5ACUNAQQ7AIWIP7JGBDUWCQCCR6MT6UIWBXIHJMZUGIT3IAKX2SKT55HEI</signature><certificate>GAWQEFIARSGMFXPFJZLZZMNQCYIT5VG66O7NWP6VAIKFJ6SZ2E2AJRHTD63UXONQT36SA5X7F7OA|1|GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMADGSCIYVEIUKQNN6A4D7B77A62EMIURPPHQE2ILP3GJLRTUSRSK2Q4IALFHHVMRA2HOERD25DUJXGX2IQDEAFWWLVG7FOHRG2Z3H4ER66UV2UVI7X5VG3UWGJ72ZW3V5V55ELJVJ52EAFCZW4WKUP2EAIUBDQYZ63LG5PFXYY5N2RHKJOU6UUAJZUV3VTGZNFDIU6G3BMMIIDG</certificate><props>propsdata</props></simpp>"));
        assertEquals(656, parser.getVersion());
        assertEquals(1, parser.getKeyVersion());
        assertEquals(1, parser.getNewVersion());
        assertEquals("GAWAEFBMGCFLMI5E5ACUNAQQ7AIWIP7JGBDUWCQCCR6MT6UIWBXIHJMZUGIT3IAKX2SKT55HEI", parser.getSignature());
        assertEquals("GAWQEFIARSGMFXPFJZLZZMNQCYIT5VG66O7NWP6VAIKFJ6SZ2E2AJRHTD63UXONQT36SA5X7F7OA|1|GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMADGSCIYVEIUKQNN6A4D7B77A62EMIURPPHQE2ILP3GJLRTUSRSK2Q4IALFHHVMRA2HOERD25DUJXGX2IQDEAFWWLVG7FOHRG2Z3H4ER66UV2UVI7X5VG3UWGJ72ZW3V5V55ELJVJ52EAFCZW4WKUP2EAIUBDQYZ63LG5PFXYY5N2RHKJOU6UUAJZUV3VTGZNFDIU6G3BMMIIDG", parser.getCertificateString());
        assertEquals("propsdata", parser.getPropsData());
    }
}
