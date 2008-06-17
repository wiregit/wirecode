package org.limewire.swarm.http;

import junit.framework.Test;

import org.limewire.collection.Range;
import org.limewire.util.BaseTestCase;

public class SwarmHttpUtilsTest extends BaseTestCase {

    public SwarmHttpUtilsTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(SwarmHttpUtilsTest.class);
    }
    
    public void testRangeForRequest() throws Exception {
        assertEquals(Range.createRange(0, 1), SwarmHttpUtils.rangeForRequest("bytes=0-1"));
    }
}
