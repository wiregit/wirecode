package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import java.util.StringTokenizer;
import com.sun.java.util.collections.*;


public class ResponseVerifier{
    /** A mapping from GUIDs to the words of the search made with that GUID.
     *
     *  INVARIANT: for each String[] V in the range of mapper,
     *        +V contains no duplicates
     *        +no string in V contains any of the characters in DELIMETER
     *        +no string in V contains upper-case characters
     */
    private ForgetfulHashMap /* GUID -> String[] */ mapper = new ForgetfulHashMap(50);
    /** The characters to use in stripping apart queries and replies. */
    private static final String DELIMITERS="_,.-+/\\ *()";
    
    /** 
     *  @modifies this
     *  @effects memorizes the query string for qr.  This will be used to score
     *   responses later.
     */
    public synchronized void record(QueryRequest qr){
	//Copy words in the query to queryWords.
	List buf=new ArrayList();
	StringTokenizer st = new StringTokenizer(qr.getQuery().toLowerCase(),
						 DELIMITERS);
	while(st.hasMoreTokens()) {
	    String word=(String)st.nextToken();
	    if (! buf.contains(word))
		buf.add(word);	    
	}
	String[] queryWords=new String[buf.size()];
	buf.toArray(queryWords);

	byte[] guid = qr.getGUID();
	mapper.put(new GUID(guid),queryWords);
    }
	
    
    /**
     * Returns the score of the given response to the reply with the given GUID,
     * on a scale from 0 to 100.  If Guid is not recognized, a result of
     * 100 is given.<p>
     *
     * Let words(s) be the set of all words in a string s, where word
     * divisions are marked by certain non-alphanumeric characters.
     * Let Q be the original search string with the given GUID.  Let R
     * be resp.getName().  The returned score is currently equal to
     * |words(Q)*words(R)|/words(Q), i.e., the percentage of words in
     * the query found in the reply.  Future versions may score responses
     * differently, perhaps to treat regular expressions properly.
     */
    public synchronized int score(byte[] guid, Response resp){
	int numMatchingWords=0;

	String[] queryWords = (String[])mapper.get(new GUID(guid));
	if (queryWords == null)
	    return 100; // assume 100% match if no corresponding query found.
	int numQueryWords=queryWords.length;
	if (numQueryWords==0)
	    return 100; // avoid divide-by-zero errors below

	//Extract words from reply into an array
	String Reply = resp.getName().toLowerCase(); // so we have the returned filename in lower case	
	StringTokenizer st = new StringTokenizer(Reply,DELIMITERS);
	List buf=new ArrayList();
	while (st.hasMoreTokens())
	    buf.add(st.nextToken());	
	String[] replyWords=new String[buf.size()];
	buf.toArray(replyWords);
	
	/* Note that this algorithm runs in O(M*N) time, where M=|words(Q)|
	   and N=|words(R)|.  It is possible to make this run is sub-quadratic
	   time by using more efficient set representations. */
	//For each word in the query...
	for (int i=0; i<numQueryWords; i++) {
	    String word=queryWords[i];
	    //Increment numMatchingWords if word in query
	    for (int j=0; j<replyWords.length; j++) {
		if (word.equals(replyWords[j])) {
		    numMatchingWords++;
		    break;
		}
	    }
	}	
	return (int)((float)numMatchingWords * 100.f/(float)numQueryWords);
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
	System.out.println("blah gets : " + rv.score(guid,r) );

	Response r1 = new Response(1,1,"test this file will ya");
	Assert.that(rv.score(guid,r1)==50);
	System.out.println("test this file will ya  gets : " + rv.score(guid,r1) );
	
	Response r2 = new Response(1,1,"Sumeet says that this is the best");
	Assert.that(rv.score(guid,r2)==50);
	System.out.println("Sumeet says that this is the best  gets : " + rv.score(guid,r2) );

	Response r3 = new Response(1,1,"Sumeet test the moon");
	Assert.that(rv.score(guid,r3)==100);
	System.out.println("Sumeet tests the moon  gets : " + rv.score(guid,r3) );

	QueryRequest qr2=new QueryRequest((byte)7,0,"Weird Al Cantina");
	rv.record(qr2);
	Response r4=new Response(1,1,"SSC2-CannibalTheMusical.asf");
	int score=rv.score(qr2.getGUID(), r4);
	Assert.that(score==0, "Score is "+score);	
       
	QueryRequest qr3=new QueryRequest((byte)7,0,"");
	rv.record(qr3);
	Assert.that(rv.score(qr3.getGUID(), r4)==100);

	QueryRequest qr4=new QueryRequest((byte)7,0,"weird al");
	Response r5=new Response(1,1,"Wierd Al-The Weird Al Show Theme.mp3");
	int score2=rv.score(qr4.getGUID(), r5);
	Assert.that(score2==100, "Score is "+score2);
    }    
    */
}

