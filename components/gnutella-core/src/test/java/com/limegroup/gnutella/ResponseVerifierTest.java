package com.limegroup.gnutella;

import junit.framework.*;
import com.sun.java.util.collections.*;

public class ResponseVerifierTest extends TestCase {

    public ResponseVerifierTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ResponseVerifierTest.class);
    }

    public void testLegacy() {
        ResponseVerifier rv = new ResponseVerifier();
        QueryRequest qr = new QueryRequest((byte)7,0,"test Sumeet");
        rv.record(qr);
        byte[] guid = qr.getGUID();
        Response r = new Response(1,1,"blah");
        assertTrue(rv.score(guid,r)==0);
    
        Response r1 = new Response(1,1,"test this file will ya");
        assertTrue(rv.score(guid,r1)==50);
    
        Response r2 = new Response(1,1,"Sumeet says that this is the best");
        assertTrue(rv.score(guid,r2)+"", rv.score(guid,r2)==50);
    
        Response r3 = new Response(1,1,"Sumeet test the moon");
        assertTrue(rv.score(guid,r3)+"", rv.score(guid,r3)==100);
    
        QueryRequest qr2=new QueryRequest((byte)7,0,"Big Al Bob");
        rv.record(qr2);
        Response r4=new Response(1,1,"Cannibalistic");  //substring match
        int score=rv.score(qr2.getGUID(), r4);
        assertTrue("Score is "+score, score==33);
    
        QueryRequest qr3=new QueryRequest((byte)7,0,"");
        rv.record(qr3);
        assertTrue(rv.score(qr3.getGUID(), r4)==100);
    
        QueryRequest qr4=new QueryRequest((byte)7,0,"weird chris");
        Response r5=new Response(1,1,"Wierd chris-The Weird chris circus.mp3");
        int score2=rv.score(qr4.getGUID(), r5);
        assertTrue("Score is "+score2, score2==100);
    
        //////////////////// Wildcard tests /////////////////////////////
      
        qr4=new QueryRequest((byte)7,0,"*.mp3 weird*whoops weird*circus");
        rv.record(qr4);
        r5=new Response(1,1,"Wierd chris-The WEIRD chris circus.mp3");
        score2=rv.score(qr4.getGUID(), r5);
        assertTrue("Score is "+score2, score2==66);
    
        qr4=new QueryRequest((byte)7,0,"circus+chris+weir*+sumeet");
        rv.record(qr4);
        r5=new Response(1,1,"Wierd Chris-The Weird Chris Circus.mp3");
        score2=rv.score(qr4.getGUID(), r5);
        assertTrue("Score is "+score2, score2==75);
    
    
        //////////////////////// matchesType tests //////////////////////
        MediaType mt=new MediaType("audio", "Audio", new String[] {"mp3"});
    
        rv=new ResponseVerifier();
        rv.record(qr4, mt);
        assertTrue(rv.matchesType(qr4.getGUID(), r5));
        assertTrue(! rv.matchesType(qr4.getGUID(), r4));
    
        rv=new ResponseVerifier();
        rv.record(qr4, null);
        assertTrue(rv.matchesType(qr4.getGUID(), r5));
        assertTrue(rv.matchesType(qr4.getGUID(), r4));
    
        ////////////////////// isMandragoreWorm tests /////////////////////
        rv=new ResponseVerifier();
        assertTrue(! rv.isMandragoreWorm(qr.getGUID(), r3));    
        qr=new QueryRequest((byte)7,0, "test");
        rv.record(qr);
        r1 = new Response(1, 8192, "test response.exe");
        r2 = new Response(1, 8000, "test.exe");
        r3 = new Response(1, 8192, "test.exe");
        assertTrue(! rv.isMandragoreWorm(qr.getGUID(), r1));
        assertTrue(! rv.isMandragoreWorm(qr.getGUID(), r2));
        assertTrue(rv.isMandragoreWorm(qr.getGUID(), r3));    
    
        //////////////////////        XML TESTS        /////////////////////
        rv=new ResponseVerifier();
        String xml = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio action=\"sumeet is cool\" identifier=\"/home/smd/music/Foozfest - Defense Song.mp3\">";
        String xml2 = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio action=\"sumeet cool\" identifier=\"/home/smd/music/Foozfest - Defense Song.mp3\">";
        qr=new QueryRequest(GUID.makeGuid(), (byte)7, 0, 
                            "sumeet thadani",
                            xml);
        rv.record(qr);
        r1=new Response(1, 9199, "sumeet", xml2);
        assertTrue(rv.score(qr.getGUID(), r1)==50);
    
    
        xml = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio action=\"sumeet is cool\" identifier=\"/home/smd/music/Foozfest - Defense Song.mp3\">";
        xml2 = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio action=\"susheel cold\" identifier=\"/home/smd/music/Foozfest - Defense Song.mp3\">";
        qr=new QueryRequest(GUID.makeGuid(), (byte)7, 0, 
                            "sumeet thadani",
                            xml);
        rv.record(qr);
        r1=new Response(1, 9199, "susheel", xml2);
        assertTrue(rv.score(qr.getGUID(), r1)==0);
    
    
        xml = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio action=\"sumeet is cool\" link=\"susheeldaswani.com\" identifier=\"/home/smd/music/Foozfest\">";
        xml2 = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio action=\"sumeet is cool\" link=\"smd.com\" identifier=\"/home/smd/music/Foozfest\">";
        qr=new QueryRequest(GUID.makeGuid(), (byte)7, 0, 
                            "",
                            xml);
        rv.record(qr);
        r1=new Response(1, 9199, "", xml2);
        assertTrue(rv.score(qr.getGUID(), r1)==100);
    }

    public void testScoreIgnoreCase() {
        assertEquals(100, 
                     ResponseVerifier.score("test query", null, 
                                            newRFD("test query.txt")));
        assertEquals(100, 
                     ResponseVerifier.score("TEST qUErY", null, 
                                            newRFD("test Query.txt")));
        assertEquals(50, 
                     ResponseVerifier.score("query oops", null, 
                                            newRFD("QQQQUERY.txt")));
    }

    private static RemoteFileDesc newRFD(String name) {
        Set urns=new HashSet(1);
        return new RemoteFileDesc("1.2.3.4", 6346, 13l,
                                  name, 1024,
                                  new byte[16], 56, false, 4, true, null, urns);
    }
}
