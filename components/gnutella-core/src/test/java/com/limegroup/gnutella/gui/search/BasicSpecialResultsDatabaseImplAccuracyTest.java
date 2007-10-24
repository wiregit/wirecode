package com.limegroup.gnutella.gui.search;

import java.io.IOException;

import junit.framework.Test;

import com.limegroup.gnutella.SpeedConstants;

/**
 * This class tests that search results contain information that's not contained
 * in the search string, such as quality and speed. These are always the same.
 * This is separated out from
 * {@link BasicSpecialResultsDatabaseImplLeniencyTest} because this contains
 * only tests that <b>should</b> return something.
 */
public class BasicSpecialResultsDatabaseImplAccuracyTest extends AbstractBasicSpecialResultsDatabaseImplTestSupport {

    public BasicSpecialResultsDatabaseImplAccuracyTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BasicSpecialResultsDatabaseImplAccuracyTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    
     
    public void testSpeed() throws IOException {
        runTestWithString(SpeedConstants.THIRD_PARTY_SPEED_INT, NORMAL, new Getter() {
            public Object get(SearchResult sr) { return sr.getSpeed(); }
        });
    }
    
    public void testQuality() throws IOException {
        runTestWithString(QualityRenderer.THIRD_PARTY_RESULT_QUALITY, NORMAL, new Getter() {
            public Object get(SearchResult sr) { return sr.getQuality(); }
        });
    }   
    
    /*
     * Test strings
     */    
  
    private final static String NORMAL =
        "cat\tcat|url=http://limewire.com\tsize=1230\tname=the-name\tartist=the artist0\talbum=playing with fire0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
     ;       
  
}
