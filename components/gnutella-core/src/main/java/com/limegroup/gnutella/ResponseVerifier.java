package com.limegroup.gnutella;

import java.util.HashSet;
import java.util.List;

import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.ForgetfulHashMap;
import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Records information about queries so that responses can be validated later.
 * Typical use is to call record(..) on an outgoing query request, and
 * score/matchesType/isMandragoreWorm on each incoming response.  
 */
public class ResponseVerifier {
    private static class RequestData {
        /** The original query. */
        String query;
        /** The rich query. */
        LimeXMLDocument richQuery;
        /** The keywords of the original query, lowercased. */
        String[] queryWords;
        /** The type of the original query. */
        MediaType type;

        RequestData(String query, MediaType type) {
            this(query, null, type);
        }

        RequestData(String query, LimeXMLDocument richQuery, MediaType type) {
            this.query=query;
            this.richQuery=richQuery;
            this.queryWords=getSearchTerms(query, richQuery);
            this.type=type;
        }

        public boolean xmlQuery() {
            return richQuery != null;
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
        mapper.put(new GUID(guid),new RequestData(qr.getQuery(), 
                                                  qr.getRichQuery(),
                                                  type));
    }

    /**
     * Returns the score of the given response compared to the given query.
     *
     * @param query the query keyword string sent
     * @param richQuery the XML metadata string sent, or null if none
     * @param response the response to score, converted to RemoteFileDesc
     * @return the percentage of query keywords (0-100) matching
     */
    public static int score(String query, 
                            LimeXMLDocument richQuery, 
                            RemoteFileDesc response) {
        return score(getSearchTerms(query, richQuery), response.getFileName());
    }

    /** Actual implementation of scoring; called from both public versions. 
     *  @param queryWords the tokenized query keywords
     *  @param filename the name of the response*/
    private static int score(String[] queryWords, String filename) {
        int numMatchingWords=0;
        int numQueryWords=queryWords.length;
        if (numQueryWords==0)
            return 100; // avoid divide-by-zero errors below

        //Count the number of regular expressions from the query that
        //match the result's name.  Ignore case in comparison.
        for (int i=0; i<numQueryWords; i++) {
            String pattern=queryWords[i];
            if (StringUtils.contains(filename, pattern, true)) {
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

    private static String[] getSearchTerms(String query,
                                           LimeXMLDocument richQuery) {
        String[] terms = null;
        // combine xml and standard keywords
        // ---------------------------------------
        HashSet qWords=new HashSet();
        terms = StringUtils.split(query.toLowerCase(), DELIMITERS);
        // add the standard query words..
        for (int i = 0; i < terms.length; i++)
            qWords.add(terms[i]);

        List xmlWords=null;
        if (richQuery != null) {
            xmlWords = richQuery.getKeyWords();
            final int size = xmlWords.size();
            // add a lowercase version of the xml words...
            for (int i = 0; i < size; i++) {
                String currWord = (String) xmlWords.remove(0);
                qWords.add(currWord.toLowerCase());
            }
        }
        
        return (String[])qWords.toArray(new String[qWords.size()]);
    }
}

