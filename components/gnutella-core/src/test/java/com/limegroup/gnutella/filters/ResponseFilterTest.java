package com.limegroup.gnutella.filters;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.limegroup.gnutella.settings.FilterSettings;

/**
 * Unit tests for ResponseFilter, URNReponseFilter
 */
public class ResponseFilterTest extends com.limegroup.gnutella.util.BaseTestCase {

    public ResponseFilterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ResponseFilterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testResponseFilterFactory() {
        //is singleton?
        assertTrue(ResponseFilter.instance()==ResponseFilter.instance());
    }

    public void testURNResponseFilter() {
        URNResponseFilter f = new URNResponseFilter();
        String urn1 = "urn:sha1:4UWZJVBV3CXEGLA736FK47KAQJQSMZ5J";
        String urn2 = "urn:sha1:52MWSJ275Y7UUXPHP4JBEKDVUFKW7JQY";
        String[] blockedUrns;

        blockedUrns = new String[0];
        FilterSettings.BLOCKED_URNS.setValue(blockedUrns);
        f.refresh();
        assertTrue(f.allow(urn1));
        assertTrue(f.allow(urn2));

        blockedUrns = new String[1];
        blockedUrns[0] = urn1;
        FilterSettings.BLOCKED_URNS.setValue(blockedUrns);
        f.refresh();
        assertFalse(f.allow(urn1));
        assertTrue(f.allow(urn2));

        blockedUrns = new String[2];
        blockedUrns[0] = urn1;
        blockedUrns[1] = urn2;
        FilterSettings.BLOCKED_URNS.setValue(blockedUrns);
        f.refresh();
        assertFalse(f.allow(urn1));
        assertFalse(f.allow(urn2));
            
        blockedUrns = new String[0];
        FilterSettings.BLOCKED_URNS.setValue(blockedUrns);
        f.refresh();
        assertTrue(f.allow(urn1));
        assertTrue(f.allow(urn2));
    }
}

