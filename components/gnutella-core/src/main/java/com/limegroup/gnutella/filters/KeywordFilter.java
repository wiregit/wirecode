package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import java.util.Locale;

/** 
 * A spam filter that removes certain "bad" keywords. 
 * If <i>any</i> words in a query are in the banned set, the
 * query is disallowed.
 */
public class KeywordFilter extends SpamFilter {
    /** INVARIANT: strings in ban contain only lowercase */
    private List /* of String */ ban=new ArrayList();
    /** For converting strings to lower case. */
    private Locale locale=Locale.getDefault();

    /** 
     * @modifies this
     * @effects bans the given phrase.  Capitalization does not matter.
     */
    public void disallow(String phrase) { 
        String canonical=phrase.toLowerCase();
        if (! ban.contains(canonical))
            ban.add(canonical);
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

    /**
     * @modifies this
     * @effects bans .vbs files
     */
    public void disallowVbs() {
        disallow(".vbs");
    }

    /**
     * @modifies this
     * @effects bans .htm and html files
     */
    public void disallowHtml() {
        disallow(".htm");
    }

    public boolean allow(Message m) {
        boolean ok=true;
        if (m instanceof QueryRequest) 
            return allow((QueryRequest)m);
        else if (m instanceof QueryReply)
            return allow((QueryReply)m);
        else
            return ok;
    }

    protected boolean allow(QueryRequest qr) {
        //return false iff any of the words in query are in ban
        String query=qr.getQuery();
        return ! matches(query);
    }

    protected boolean allow(QueryReply qr) {
        //if any of the file names in qr contain bad words, the whole
        //thing is disallowed
        try {
            for (Iterator iter=qr.getResults(); iter.hasNext(); ) {
                Response response=(Response)iter.next();
                if (matches(response.getName()))
                    return false;
            }
        } catch (BadPacketException e) {
            return false;
        }
        return true;
    }

    /** 
     * Returns true if phrase matches any of the entries in ban.
     */
    protected boolean matches(String phrase) {
        String canonical=phrase.toLowerCase();
        for (int i=0; i<ban.size(); i++) {
            String badWord=(String)ban.get(i);
            //phrase contains badWord?
            //Hopefully indexOf uses some reasonably efficient 
            //algorithm, such as Knuth-Morris-Pratt.
            if (canonical.indexOf(badWord)!=-1)
                return true;
        }
        return false;
    }

    //      public static void main(String args[]) {
    //      KeywordFilter filter=new KeywordFilter();
    //      QueryRequest qr=null;

    //      qr=new QueryRequest((byte)1,0,"Britney");
    //      Assert.that(filter.allow(qr));
    //      filter.disallow("britney spears");
    //      Assert.that(filter.allow(qr));
    
    //      qr=new QueryRequest((byte)1,0,"pie with rhubarb");
    //      Assert.that(filter.allow(qr));
    //      filter.disallow("rhuBarb");
    //      Assert.that(!filter.allow(qr));
    //      qr=new QueryRequest((byte)1,0,"rhubarb.txt");
    //      Assert.that(!filter.allow(qr));
    //      qr=new QueryRequest((byte)1,0,"Rhubarb*");
    //      Assert.that(!filter.allow(qr));

    //      filter.disallowVbs();
    //      qr=new QueryRequest((byte)1,0,"test.vbs");
    //      Assert.that(!filter.allow(qr));

    //      filter.disallowHtml();
    //      qr=new QueryRequest((byte)1,0,"test.htm");
    //      Assert.that(!filter.allow(qr));
    //      }
}
