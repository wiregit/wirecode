package com.limegroup.gnutella.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.QueryRequest;

import junit.framework.Test;

@SuppressWarnings("unchecked")
public class QueryUtilsTest extends com.limegroup.gnutella.util.LimeTestCase {
    
    public QueryUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(QueryUtilsTest.class);
    }  

    public void testCreateQueryString() {
        QueryRequest qr;
        
        String query = QueryUtils.createQueryString("file and 42-name_minus #numbers");
        containsAll("file name minus numbers", query);
        
        query = QueryUtils.createQueryString("reallylongfilenamethatisgoingtotruncate");
        assertEquals("reallylongfilenamethatisgoingt", query);
        // verify that we can create local & network queries out of the query string
        qr = QueryRequest.createQuery(query);
        QueryRequest.createMulticastQuery(GUID.makeGuid(), qr);
        
        //such query will fit any 2 out of 3 words in it.
        query = QueryUtils.createQueryString("short one, reallylongotherfilename");
        assertEquals(2,query.split(" ").length);
        qr = QueryRequest.createQuery(query);
        QueryRequest.createMulticastQuery(GUID.makeGuid(), qr);
        
        query = QueryUtils.createQueryString("longfirstthingthatwontfitatall, but short others");
        containsAll("but short others", query);
        qr = QueryRequest.createQuery(query);
        QueryRequest.createMulticastQuery(GUID.makeGuid(), qr);
        
        query = QueryUtils.createQueryString("(5).jpg");
        assertEquals("5 jpg", query);
        qr = QueryRequest.createQuery(query);
        QueryRequest.createMulticastQuery(GUID.makeGuid(), qr);
        
        // test with allow numbers switched on
        assertEquals("500 jpg", QueryUtils.createQueryString("500 jpg", true));
        assertEquals("file 42 name minus numbers",
                QueryUtils.createQueryString("file and 42-name_minus #numbers", true));
        
        // test that string with a number + illegal characters + keyword too long
        // is truncating the keyword, not the whole string.
        query = QueryUtils.createQueryString("1920936_thisisaverylongkeywordwithanumbernadillegalcharacterthatwillbetruncated");
        assertEquals("thisisaverylongkeywordwithanum", query);
    }

    
    public void testRipExtension() {
        assertEquals("a", QueryUtils.ripExtension("a.b"));
        assertEquals("c.d", QueryUtils.ripExtension("c.d."));
        assertEquals("e.f.g", QueryUtils.ripExtension("e.f.g.h"));
    }
    
    public void testKeywords() {
        Set valid = new HashSet();
        valid.add("phish");
        valid.add("is");
        valid.add("dead");
        valid.add("long");
        valid.add("live");
        
        assertEquals(valid, QueryUtils.keywords("Phish is dead! :( Long-live Phish. :)"));
        
        
        valid.clear();
        valid.add("quick");
        valid.add("foot");
        valid.add("brown");
        valid.add("fox");
        valid.add("jumped");
        valid.add("over");
        valid.add("fence");
        assertEquals(valid, QueryUtils.keywords("THE 12 foot quick\\brown]\nfox[jumped [] # over-the _brown*fence_"));
        
        valid.clear();
        valid.add("sam");
        valid.add("born");
        assertEquals(valid, QueryUtils.keywords("sam, 2.1.81. born."));
        
        valid.clear();
        valid.add("file");
        assertEquals(valid, QueryUtils.keywords("a file.extension"));

        // test for allowNumers == true
        valid.clear();
        valid.add("11");
        valid.add("test");
        valid.add("13");
        valid.add("pg");
        // everything behind the last dot is removed by rip extension
        valid.add("3");
        assertEquals(valid, QueryUtils.keywords("11 test pg-13 3.1415947", true));
    }
    

    
    private void containsAll(String match, String query) {
        Collection matchSet = Arrays.asList(match.split(" "));
        
        String[] array = query.split(" ");
        List set = new LinkedList();
        for(int i = 0; i < array.length; i++)
            set.add(array[i]);
        for(Iterator i = matchSet.iterator(); i.hasNext(); ) {
            String next = (String)i.next();
            assertContains(set, next);
            set.remove(next);
        }
        assertEquals(set.toString(), 0, set.size());
    }
    
}
