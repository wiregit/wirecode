package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;

/**
 * Records information about queries so that responses can be validated later.
 * Typical use is to call record(..) on an outgoing query request, and
 * score/matchesType/isMandragoreWorm on each incoming response.  
 */
public class ResponseVerifier {
    private static class RequestData {
        /** The original query. */
        String query;
        /** The keywords of the original query, lowercased. */
        String[] queryWords;
        /** The type of the original query. */
        MediaType type;

        RequestData(String query, MediaType type) {
            this.query=query;
            this.queryWords=StringUtils.split(query.toLowerCase(),
                                              DELIMITERS);
            this.type=type;
        }
    }

    /**
     *  A mapping from GUIDs to the words of the search made with that GUID.
     */
    private ForgetfulHashMap /* GUID -> RequestData */ mapper =
        new ForgetfulHashMap(15);
    /** The characters to use in stripping apart queries. */
    private static final String DELIMITERS="+ ";
    /** The size of a Mandragore worm response, i.e., 8KB. */
    private static final long Mandragore_SIZE=8*1024l;

    /** Same as record(qr, null). */
    public synchronized void record(QueryRequest qr) {
        record(qr, null);
    }

    /**
     *  @modifies this
     *  @effects memorizes the query string for qr; this will be used to score
     *   responses later.  If type!=null, also memorizes that qr was for the given
     *   media type; otherwise, this is assumed to be for any type.
     */
    public synchronized void record(QueryRequest qr, MediaType type){
        byte[] guid = qr.getGUID();
        mapper.put(new GUID(guid),new RequestData(qr.getQuery(), type));
    }


    /**
     * Returns the score of the given response to the reply with the given GUID,
     * on a scale from 0 to 100.  If Guid is not recognized, a result of
     * 100 is given.<p>
     */
    public synchronized int score(byte[] guid, Response resp) {
        int numMatchingWords=0;

        RequestData request=(RequestData)mapper.get(new GUID(guid));
        if (request == null)
            return 100; // assume 100% match if no corresponding query found.
        String[] queryWords = request.queryWords;
        int numQueryWords=queryWords.length;
        if (numQueryWords==0)
            return 100; // avoid divide-by-zero errors below

        //Count the number of regular expressions from the query that
        //match the result's name.
        String name=resp.getName().toLowerCase();
        for (int i=0; i<numQueryWords; i++) {
            String pattern=queryWords[i];
            if (StringUtils.contains(name,pattern)) {
                numMatchingWords++;
                continue;
            }
        }

        return (int)((float)numMatchingWords * 100.f/(float)numQueryWords);
    }

    /**
     * Returns true if response has the same media type as the
     * corresponding query request the given GUID.  In the rare case
     * that guid is not known (because this' buffers overflowed),
     * conservatively returns true.
     */
    public boolean matchesType(byte[] guid, Response response) {
        RequestData request=(RequestData)mapper.get(new GUID(guid));
        if (request == null || request.type==null)
            return true;
        String reply = response.getName();
        return request.type.matches(reply);
    }

    /**
     * Returns true if the given response is an instance of the Mandragore
     * Worm.  This worm responds to the query "x" with a 8KB file named
     * "x.exe".  In the rare case that the query for guid can't be found
     * returns false.
     */
    public boolean isMandragoreWorm(byte[] guid, Response response) {
        RequestData request=(RequestData)mapper.get(new GUID(guid));
        if (request == null)
            return false;
        return response.getSize()==Mandragore_SIZE 
                   && response.getName().equals(request.query+".exe");
    }

    public String toString() {
        return mapper.toString();
    }

    /* Unit tests */
    /*
    public static void main(String args[]){
    ResponseVerifier rv = new ResponseVerifier();
    QueryRequest qr = new QueryRequest((byte)7,0,"test Sumeet");
    rv.record(qr);
    byte[] guid = qr.getGUID();
    Response r = new Response(1,1,"blah");
    Assert.that(rv.score(guid,r)==0);

    Response r1 = new Response(1,1,"test this file will ya");
    Assert.that(rv.score(guid,r1)==50);

    Response r2 = new Response(1,1,"Sumeet says that this is the best");
    Assert.that(rv.score(guid,r2)==50, rv.score(guid,r2)+"");

    Response r3 = new Response(1,1,"Sumeet test the moon");
    Assert.that(rv.score(guid,r3)==100,  rv.score(guid,r3)+"");

    QueryRequest qr2=new QueryRequest((byte)7,0,"Weird Al Cantina");
    rv.record(qr2);
    Response r4=new Response(1,1,"SSC2-CannibalTheMusical.asf");
    int score=rv.score(qr2.getGUID(), r4);
    Assert.that(score==33, "Score is "+score);

    QueryRequest qr3=new QueryRequest((byte)7,0,"");
    rv.record(qr3);
    Assert.that(rv.score(qr3.getGUID(), r4)==100);

    QueryRequest qr4=new QueryRequest((byte)7,0,"weird al");
    Response r5=new Response(1,1,"Wierd Al-The Weird Al Show Theme.mp3");
    int score2=rv.score(qr4.getGUID(), r5);
    Assert.that(score2==100, "Score is "+score2);

    //////////////////// Wildcard tests /////////////////////////////

    qr4=new QueryRequest((byte)7,0,"*.mp3 weird*whoops weird*show");
    rv.record(qr4);
    r5=new Response(1,1,"Wierd Al-The WEIRD Al Show Theme.mp3");
    score2=rv.score(qr4.getGUID(), r5);
    Assert.that(score2==66, "Score is "+score2);

    qr4=new QueryRequest((byte)7,0,"show+al+weir*+metallica");
    rv.record(qr4);
    r5=new Response(1,1,"Wierd Al-The Weird Al Show Theme.mp3");
    score2=rv.score(qr4.getGUID(), r5);
    Assert.that(score2==75, "Score is "+score2);


    //////////////////////// matchesType tests //////////////////////
    MediaType mt=new MediaType("Audio", new String[] {"mp3"});

    rv=new ResponseVerifier();
    rv.record(qr4, mt);
    Assert.that(rv.matchesType(qr4.getGUID(), r5));
    Assert.that(! rv.matchesType(qr4.getGUID(), r4));

    rv=new ResponseVerifier();
    rv.record(qr4, null);
    Assert.that(rv.matchesType(qr4.getGUID(), r5));
    Assert.that(rv.matchesType(qr4.getGUID(), r4));

    ////////////////////// isMandragoreWorm tests /////////////////////
    rv=new ResponseVerifier();
    Assert.that(! rv.isMandragoreWorm(qr.getGUID(), r3));    
    qr=new QueryRequest((byte)7,0, "test");
    rv.record(qr);
    r1 = new Response(1, 8192, "test response.exe");
    r2 = new Response(1, 8000, "test.exe");
    r3 = new Response(1, 8192, "test.exe");
    Assert.that(! rv.isMandragoreWorm(qr.getGUID(), r1));
    Assert.that(! rv.isMandragoreWorm(qr.getGUID(), r2));
    Assert.that(rv.isMandragoreWorm(qr.getGUID(), r3));    
    }
    */
}

