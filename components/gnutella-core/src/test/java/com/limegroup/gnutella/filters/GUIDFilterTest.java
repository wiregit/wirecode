package com.limegroup.gnutella.filters;

import junit.framework.*;
import com.limegroup.gnutella.*;

/**
 * Unit tests for GUIDFilterTest
 */
public class GUIDFilterTest extends TestCase {
    SpamFilter filter;
    byte[] guid;

    public GUIDFilterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(GUIDFilterTest.class);
    }

    public void setUp() {
        guid=new byte[16];
        SettingsManager settings=SettingsManager.instance();
        settings.setFilterDuplicates(false);
        settings.setFilterGreedyQueries(false);
        settings.setFilterBearShareQueries(false);
        filter=SpamFilter.newRouteFilter();
    }

    public void testDisallow() {
        guid[0]=(byte)0x41;
        guid[1]=(byte)0x61;
        guid[2]=(byte)0x42;
        guid[3]=(byte)0x62;
        guid[4]=(byte)0x5A;
        QueryRequest query=new QueryRequest(guid, (byte)3, 0, "test query");
        assertTrue(! filter.allow(query));
    }

    public void testAllow1() {
        guid[0]=(byte)0x41;
        guid[1]=(byte)0x61;
        guid[2]=(byte)0x42;
        guid[3]=(byte)0x62;
        guid[4]=(byte)0x5B;
        QueryRequest query=new QueryRequest(guid, (byte)3, 0, "test query");
        assertTrue(filter.allow(query));
    }

    public void testAllow2() {
        guid[0]=(byte)0x42;
        guid[1]=(byte)0x61;
        guid[2]=(byte)0x42;
        guid[3]=(byte)0x62;
        guid[4]=(byte)0x5A;
        QueryRequest query=new QueryRequest(guid, (byte)3, 0, "test query");
        assertTrue(filter.allow(query));
    }
}
