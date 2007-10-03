package com.limegroup.gnutella.gui.search;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class BasicSpecialResultsDatabaseImplTest extends BaseTestCase {
    
    public BasicSpecialResultsDatabaseImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BasicSpecialResultsDatabaseImplTest.class);
    }

    public void testFromFile() throws IOException {
        runTest("test.txt", "cat", 1);
    }  
    
    public void testFromFile50() throws IOException {
        runTest("test50.txt", "cat", 3);
    } 
    
    public void testNone() throws IOException {
        runTestWithString(S0, "cat", 0);
    }
    
    public void test1_2() throws IOException {
        runTestWithString(S1_2, "cat", 2);
    }
    
    public void test1_3() throws IOException {
        runTestWithString(S1_3, "cat", 3);
    }
    
    public void test2() throws IOException {
        runTestWithString(S2, "cat", 2);
    }
    
    public void test2_3() throws IOException {
        runTestWithString(S2_3, "cat", 3);
    }
    
    public void test3() throws IOException {
        runTestWithString(S3, "cat", 3);
    }
    
    public void test3_4() throws IOException {
        runTestWithString(S3_4, "cat", 4);
    }    
    
    private final static String S0 =
        "cat0|url=http://limewire.com\tsize=1230\tartist=dr. seuss0\talbum=cat in the hat0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
     ;
    
    private final static String S1_2 =
        "cat\tcat|url=http://limewire.com\tsize=1230\tartist=dr. seuss0\talbum=cat in the hat0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
     ;
    
    private final static String S1_3 =
        "cat\tcat\tcat|url=http://limewire.com\tsize=1230\tartist=dr. seuss0\talbum=cat in the hat0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
     ;
    
    private final static String S2 =
        "cat|url=http://limewire.com\tsize=1230\tartist=dr. seuss0\talbum=cat in the hat0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
      +"cat|url=http://limewire.com\tsize=1230\tartist=dr. seuss0\talbum=cat in the hat0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
     ;
    
    private final static String S2_3 =
        "cat|url=http://limewire.com\tsize=1230\tartist=dr. seuss0\talbum=cat in the hat0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
      +"cat\tcat|url=http://limewire.com\tsize=1230\tartist=dr. seuss0\talbum=cat in the hat0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
     ;
    
    private final static String S3 =
        "cat|url=http://limewire.com\tsize=1230\tartist=dr. seuss0\talbum=cat in the hat0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
       +"cat|url=http://limewire.com\tsize=1230\tartist=dr. seuss0\talbum=cat in the hat0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
       +"cat|url=http://limewire.com\tsize=1230\tartist=dr. seuss0\talbum=cat in the hat0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"

        ;
    
    private final static String S3_4 =
        "cat\tcat|url=http://limewire.com\tsize=1230\tartist=dr. seuss0\talbum=cat in the hat0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
       +"cat|url=http://limewire.com\tsize=1230\tartist=dr. seuss0\talbum=cat in the hat0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
       +"cat|url=http://limewire.com\tsize=1230\tartist=dr. seuss0\talbum=cat in the hat0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"

        ; 

    private void runTest(String filename, String query, int numEntries) throws IOException {
        final String path ="tests/com/limegroup/gnutella/gui/search/" + filename;
        BufferedReader in = new BufferedReader(new FileReader(path));
        final StringBuffer sb = new StringBuffer();
        String line;
        while ((line = in.readLine()) != null) sb.append(line).append("\n");
        
        final String buf = sb.toString();
        runTestWithString(buf, query, numEntries);
    }
    
    private void runTestWithString(String buf, String query, int numEntries) throws IOException {
        final BasicSpecialResultsDatabaseImpl db = new BasicSpecialResultsDatabaseImpl(buf);
                
        final List<Map<String,String>> lst = db.getSearchResults(query);
        assertEquals(numEntries, lst.size());
    }
}
