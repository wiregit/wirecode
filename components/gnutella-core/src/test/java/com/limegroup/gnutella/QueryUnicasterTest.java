package com.limegroup.gnutella;

import junit.framework.*;

public class QueryUnicasterTest extends TestCase {

    public QueryUnicasterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(QueryUnicasterTest.class);
    }

    
    public void testConstruction() {
        QueryUnicaster qu = QueryUnicaster.instance();
        assertTrue(qu.getUnicastEndpoints().size() == 0);
    }


}
