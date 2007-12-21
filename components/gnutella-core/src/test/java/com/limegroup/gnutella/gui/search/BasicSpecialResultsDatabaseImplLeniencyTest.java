package com.limegroup.gnutella.gui.search;

import java.io.IOException;

import junit.framework.Test;

/**
 * This class tests that the database is lenient in creating results and doesn't
 * throw exceptions for missing values or incorrectly-formated entries. This is
 * separated out from {@link BasicSpecialResultsDatabaseImplAccuracyTest}
 * because this contains only tests that <b>should NOT</b> return anything.
 */
public class BasicSpecialResultsDatabaseImplLeniencyTest extends AbstractBasicSpecialResultsDatabaseImplTestSupport {
    
    
    public BasicSpecialResultsDatabaseImplLeniencyTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BasicSpecialResultsDatabaseImplLeniencyTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    /* Testing missing fields */
    
    public void testNoFileName() throws IOException {
        runTestAndExpectNothing(NO_FILE_NAME);
    }   
    
    public void testNoSize() throws IOException {
        runTestWithString(-1, NO_SIZE, new Getter() {
            public Object get(SearchResult sr) { return sr.getSize(); }
        });
    }
    
    public void testNoCreationTime() throws IOException {
        runTestWithString(0, NO_CREATION_TIME, new Getter() {
            public Object get(SearchResult sr) { return sr.getCreationTime(); }
        });
    }
    
    public void testNoVendor() throws IOException {
        runTestWithString("", NO_VENDOR, new Getter() {
            public Object get(SearchResult sr) { return sr.getVendor(); }
        });
    } 
    
    public void testNoHost() throws IOException {
        runTestWithString(null, NO_HOST, new Getter() {
            public Object get(SearchResult sr) { return sr.getHost(); }
        });
    }
    
    /* Testing bad fields */
    
    public void testBadCreationTime() throws IOException {
        runTestWithString(0, BAD_CREATION_TIME, new Getter() {
            public Object get(SearchResult sr) { return sr.getCreationTime(); }
        });
    }
    
    public void testBadSize() throws IOException {
        runTestWithString(-1, BAD_SIZE, new Getter() {
            public Object get(SearchResult sr) { return sr.getSize(); }
        });
    }    


    /*
     * Test strings
     */
      
    private final static String NO_FILE_NAME =
        "cat\tcat|url=http://limewire.com\tsize=1230\tartist=dr. soos0\talbum=turtles love frogs0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
     ;
    
    private final static String NO_SIZE =
        "cat\tcat|url=http://limewire.com\tname=1230\tname=the-nameartist=someone named anderson0\talbum=turtles love frogs0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
     ;
    
    private final static String NO_CREATION_TIME =
        "cat\tcat|url=http://limewire.com\tname=1230\tname=the-nameartist=someone named anderson0\talbum=turtles love frogs0\tsize=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
     ;
    
    private final static String NO_VENDOR =
        "cat\tcat|url=http://limewire.com\tname=1230\tname=the-nameartist=someone named anderson0\talbum=turtles love frogs0\tcreation_time=1231230\tcreation_time=someone0\tgenre=childrens0\tlicense=free\n"
     ;     
    
    private final static String NO_HOST =
        "cat\tcat|url=http://limewire.com\tname=1230\tname=the-nameartist=someone named anderson0\talbum=turtles love frogs0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
     ;
    
    private final static String BAD_CREATION_TIME =
        "cat\tcat|url=http://limewire.com\tname=1230\tname=the-nameartist=someone named anderson0\talbum=turtles love frogs0\tcreation_time=asdfasdf1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
     ;    
    
    private final static String BAD_SIZE =
        "cat\tcat|url=http://limewire.com\tname=1230\tname=the-nameartist=someone named anderson0\talbum=turtles love frogs0\tcreation_time=1231230\tsize=asdfasdfasdf\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
     ;  
}
