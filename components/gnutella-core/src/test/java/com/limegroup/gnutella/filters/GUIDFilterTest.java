package com.limegroup.gnutella.filters;

import junit.framework.Test;

import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.FilterSettings;

/**
 * Unit tests for GUIDFilterTest
 */
public class GUIDFilterTest extends com.limegroup.gnutella.util.LimeTestCase {
    SpamFilter filter;
    byte[] guid;

    public GUIDFilterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(GUIDFilterTest.class);
    }

    public void setUp() {
        guid=new byte[16];
        FilterSettings.FILTER_DUPLICATES.setValue(false);
        FilterSettings.FILTER_GREEDY_QUERIES.setValue(false);
        filter=SpamFilter.newRouteFilter();
    }

    public void testDisallow() {
        guid[0]=(byte)0x41;
        guid[1]=(byte)0x61;
        guid[2]=(byte)0x42;
        guid[3]=(byte)0x62;
        guid[4]=(byte)0x5A;
		QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery(guid, "test query", "");
        assertTrue(! filter.allow(query));
    }

    public void testAllow1() {
        guid[0]=(byte)0x41;
        guid[1]=(byte)0x61;
        guid[2]=(byte)0x42;
        guid[3]=(byte)0x62;
        guid[4]=(byte)0x5B;
		QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery(guid, "test query", "");
        assertTrue(filter.allow(query));
    }

    public void testAllow2() {
        guid[0]=(byte)0x42;
        guid[1]=(byte)0x61;
        guid[2]=(byte)0x42;
        guid[3]=(byte)0x62;
        guid[4]=(byte)0x5A;
		QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery(guid, "test query", "");
        assertTrue(filter.allow(query));
    }
}
