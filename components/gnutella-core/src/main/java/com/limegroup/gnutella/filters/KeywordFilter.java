package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.*;
import java.util.*;

/** 
 * A spam filter that removes certain "bad" keywords. 
 * If <i>any</i> words in a query are in the banned set, the
 * query is disallowed.
 */
public class KeywordFilter extends SpamFilter {
    /** INVARIANT: strings in ban contain only lowercase, 
     *  and none of the delimiters */
    private Set /* of String */ ban=new HashSet();
    private static final String DELIMITERS=" .*";
    /** For converting strings to lower case. */
    private Locale locale=Locale.getDefault();

    /** 
     * @modifies this
     * @effects bans all the words in query. (Be careful of writing something
     *  like ban("britney mp3")--you'll ban all MP3's!)  Case does not matter.
     */
    public void disallow(String query) { 
	StringTokenizer lexer=new StringTokenizer(query, DELIMITERS);
	while (lexer.hasMoreTokens())
	    ban.add(lexer.nextToken().toLowerCase(locale));
    }

    /** 
     * @modifies this
     * @effects bans several well-known "adult" words.
     */
    public void disallowAdult() {
	disallow("sex");
	disallow("porn");
	disallow("xxx");
	disallow("fuck");
    }

    public boolean allow(Message m) {
	boolean ok=true;
	if (m instanceof QueryRequest) {
	    //return false if any of the words in query are in ban
	    String query=((QueryRequest)m).getQuery();
	    StringTokenizer lexer=new StringTokenizer(query, DELIMITERS);
	    while (lexer.hasMoreTokens()) {
		String word=lexer.nextToken();
		if (ban.contains(word.toLowerCase(locale))) {
		    //System.out.println("Banned \""+query+"\" because of \""+word+"\"");
		    ok=false;
		    break;
		}
	    }
	}
	return ok;
    }

//      public static void main(String args[]) {
//  	KeywordFilter filter=new KeywordFilter();
//  	QueryRequest qr=null;

//  	qr=new QueryRequest((byte)1,0,"Britney");
//  	Assert.that(filter.allow(qr));
//  	filter.disallow("britney spears");
//  	Assert.that(!filter.allow(qr));
	
//  	qr=new QueryRequest((byte)1,0,"pie with rhubarb");
//  	Assert.that(filter.allow(qr));
//  	filter.disallow("rhuBarb");
//  	Assert.that(!filter.allow(qr));
//  	qr=new QueryRequest((byte)1,0,"rhubarb.txt");
//  	Assert.that(!filter.allow(qr));
//  	qr=new QueryRequest((byte)1,0,"Rhubarb*");
//  	Assert.that(!filter.allow(qr));
//      }
}
