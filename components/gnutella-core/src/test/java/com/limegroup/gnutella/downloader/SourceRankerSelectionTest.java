package com.limegroup.gnutella.downloader;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;

/**
 * tests the selection of source ranker that is appropriate to the system.
 */
public class SourceRankerSelectionTest extends BaseTestCase {

    public SourceRankerSelectionTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(SourceRankerSelectionTest.class);
    }
    
    
    public void testSelectRanker() throws Exception {
        // for now, the only type of ranker we know about is
        // the legacy ranker
        SourceRanker ranker = SourceRanker.getAppropriateRanker();
        
        assertTrue(ranker instanceof LegacyRanker);
    }

}
