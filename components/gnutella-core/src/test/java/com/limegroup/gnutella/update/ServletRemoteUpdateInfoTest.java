package com.limegroup.gnutella.update;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;

/**
 * Test for the old, servlet-style update class.
 */
public final class ServletRemoteUpdateInfoTest extends BaseTestCase {

    public ServletRemoteUpdateInfoTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ServletRemoteUpdateInfoTest.class);
    }

    public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());        
    }

    /**
     * Test to make sure that parsing of the java version string
     * is working as we expect it to.
     */
    public void testJavaVersionParsing() throws Exception {
        TestLocalInfo tli = new TestLocalInfo("1.3.1");
        ServletRemoteUpdateInfo srui = 
            new ServletRemoteUpdateInfo(tli); 
        String testString = parseDirectiveValue(srui);
        assertEquals("should be instructing us to open a web page",
                     testString, "OPEN_WEB_PAGE");

        tli = new TestLocalInfo("1.4.1");
        srui = new ServletRemoteUpdateInfo(tli); 
        testString = parseDirectiveValue(srui);
        assertEquals("should be no update",
                     testString, "NO_UPDATE");
        
    }

    /**
     * Utility method for returning the DIRECTIVE value -- the action
     * the servlet is telling us to take.
     */
    private static String parseDirectiveValue(ServletRemoteUpdateInfo srui) {
        String urlString = srui.getURLEncodedString();
        String directiveString = "DIRECTIVE=";
        int dirIndex = urlString.indexOf(directiveString);
        int endDirective = urlString.indexOf("&");
        return urlString.substring(dirIndex+directiveString.length(), 
                                   endDirective);        
    }

    private static class TestLocalInfo extends ServletLocalUpdateInfo {
        
        private final String JAVA_VERSION;
        private final String LIME_VERSION;

        TestLocalInfo(String jvm) {
            this(jvm, "2.8.6");
        }

        TestLocalInfo(String jvm, String lwVersion) {
            JAVA_VERSION = jvm;
            LIME_VERSION = lwVersion;
        }

        public String getLimeWireVersion() {
            return LIME_VERSION;
        }

        public String getOS() {
            return "Windows";
        }

        public String getJavaVersion() {
            return JAVA_VERSION;
        }
    }
}
