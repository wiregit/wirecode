package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import java.util.*;


public class ResponseVerifier{
    private ForgetfulHashMap mapper = new ForgetfulHashMap(50);
    
    /** This method populates mapper. It will be called from RouterService.query() when 
     *  a queryRequest is about to be sent.
     *  @ modifies: this
     *  @ affects : 
     */
    protected void record(QueryRequest qr){
	byte[] Guid = qr.getGUID();
	String Query = qr.getQuery().toLowerCase();
	mapper.put(Guid,Query);
    }
    protected int score(byte[] Guid, Response resp){
	int numQueryWords;
	int numMatchingWords=0;

	String Query = (String)mapper.get(Guid);
	if (Query == null)
	    return 100; // assume 100% match if no Query found.

	String Reply = resp.getName().toLowerCase(); // so we have the returned filename in lower case
	
	StringTokenizer st = new StringTokenizer(Query,",.-+/\\ ");
	Vector wordsInQuery = new Vector();
	while(st.hasMoreTokens())
	    wordsInQuery.add(st.nextToken());
	
	numQueryWords = wordsInQuery.size();
	for(int i=0; i<numQueryWords;i++){
	    if( Reply.indexOf((String)wordsInQuery.get(i))!=-1  )
		numMatchingWords++;
	}
	return ( (numMatchingWords * 100)/numQueryWords );
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
	System.out.println("blah gets : " + rv.score(guid,r) );

	Response r1 = new Response(1,1,"test this file will ya");
	Assert.that(rv.score(guid,r1)==50);
	System.out.println("test this file will ya  gets : " + rv.score(guid,r1) );
	
	Response r2 = new Response(1,1,"Sumeet says that this is the best");
	Assert.that(rv.score(guid,r2)==50);
	System.out.println("Sumeet says that this is the best  gets : " + rv.score(guid,r2) );

	Response r3 = new Response(1,1,"Sumeet tests the moon");
	Assert.that(rv.score(guid,r3)==100);
	System.out.println("Sumeet tests the moon  gets : " + rv.score(guid,r3) );
	
    }
    */
}

