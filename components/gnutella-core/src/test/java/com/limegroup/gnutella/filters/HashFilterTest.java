
package com.limegroup.gnutella.filters;

import java.io.IOException;

import junit.framework.Test;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.BaseTestCase;


public class HashFilterTest extends BaseTestCase {
    
    public HashFilterTest(String name){
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HashFilterTest.class);
    }
    
    static QueryRequest urn,urnFile,noUrn;
    static HashFilter filter;
    
    public static void globalSetUp() {
        try{
            URN sha1 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB");
            urn = QueryRequest.createRequery(sha1);
            urnFile = QueryRequest.createQuery(sha1,"some file");
            noUrn = QueryRequest.createQuery("some file");
        }catch(IOException impossible){
            ErrorService.error(impossible);
        }
        
        filter = new HashFilter();
    }
    
    public void testUrn() throws Exception {
        assertFalse(filter.allow(urn));
    }
    
    public void testUrnFile() throws Exception{
        assertFalse(filter.allow(urnFile));
    }
    
    public void testNoUrn() throws Exception {
        assertTrue(filter.allow(noUrn));
    }
    
    public void testOtherMessage() throws Exception {
        assertTrue(filter.allow(new PingRequest((byte)1)));
    }
}
