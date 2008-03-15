package org.limewire.promotion.impressions;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class PostIncrementTest extends BaseTestCase {

    public PostIncrementTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PostIncrementTest.class);
    }
 
    public void testSimple() {
        PostIncrement inc = new PostIncrement(0);
        assertEquals(0,inc.val());
        assertEquals(0,inc.inc(1));
        assertEquals(1,inc.val());
        assertEquals(1,inc.inc(5));
        assertEquals(6,inc.val());
        assertEquals(6,inc.inc());
        assertEquals(7,inc.val());
    }
    
}
