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
}
