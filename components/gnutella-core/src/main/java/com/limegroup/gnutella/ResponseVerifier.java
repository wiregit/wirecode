package com.limegroup.gnutella;

import java.io.IOException;
import com.sun.java.util.collections.*;
import org.xml.sax.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.xml.*;
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
        /** The rich query. */
        String richQuery;
        /** The keywords of the original query, lowercased. */
        String[] queryWords;
        /** The type of the original query. */
        MediaType type;

        RequestData(String query, MediaType type) {
            this(query, null, type);
        }

        RequestData(String query, String richQuery, MediaType type) {
            this.query=query;
            this.richQuery=richQuery;
            this.queryWords=getSearchTerms(query, richQuery);
            this.type=type;
        }

        public boolean xmlQuery() {
            return ((richQuery != null) && (!richQuery.equals("")));
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

    public synchronized boolean isSpecificXMLSearch(byte[] guid) {
        boolean retVal=false;

        RequestData request=(RequestData)mapper.get(new GUID(guid));
        if (request != null) {
            try {
                if ((new LimeXMLDocument(request.richQuery)).getNumFields() > 1)
                    retVal = true;
            }
            catch (SAXException eSAX) {
            }                
            catch (SchemaNotFoundException eSchema) {
            }                
            catch (IOException eIO) {
            }                            
        }

        return retVal;
    }

    /**
     * Returns the score of the given response compared to a previosly recorded
     * query with the given GUID, on a scale from 0 to 100.  If guid is not
     * recognized, a result of 100 is given.
     *
     * @param guid the 16-byte guid used to look up the original query
     * @param resp the response to score
     * @return the percentage of query keywords (0-100) matching
     */
    public synchronized int score(byte[] guid, Response resp) {
        //Lookup original query from guid.
        RequestData request=(RequestData)mapper.get(new GUID(guid));
        if (request == null)
            return 100; // assume 100% match if no corresponding query found.
        String[] queryWords = request.queryWords;

        //Calculate score.  Consider metadata part of filename.
        String filename=request.xmlQuery()
                            ? getSearchTerms(resp)
                            : resp.getName().toLowerCase();
        return score(queryWords, filename);
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
                            String richQuery, 
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


    /** Returns all search terms as one long string.  This includes xml and
     *  the standard search terms.  the terms are all in lowercase....
     */
    private static String getSearchTerms(Response resp) {
        StringBuffer retSB = new StringBuffer();
        String[] terms = getSearchTerms(resp.getName(), resp.getMetadata());
        for (int i = 0; i < terms.length; i++)
            retSB.append(terms[i] + " ");
        return retSB.toString().trim();
    }

    
    private static String[] getSearchTerms(String query,
                                           String richQuery) {
        String[] retTerms = null;
        // combine xml and standard keywords
        // ---------------------------------------
        HashSet qWords=new HashSet();
        retTerms=StringUtils.split(query.toLowerCase(),
                                          DELIMITERS);
        // add the standard query words..
        for (int i = 0; i < retTerms.length; i++)
            qWords.add(retTerms[i]);
        List xmlWords=null;
        try {
            if ((richQuery != null) && (!richQuery.equals(""))) 
                xmlWords = (new LimeXMLDocument(richQuery)).getKeyWords();
            if (xmlWords != null) {
                final int size = xmlWords.size();
                // add a lowercase version of the xml words...
                for (int i = 0; i < size; i++) {
                    String currWord = (String) xmlWords.remove(0);
                    qWords.add(currWord.toLowerCase());
                }
            }
            retTerms = new String[qWords.size()];
            Iterator setWords = qWords.iterator();
            int index = 0;
            while (setWords.hasNext())
                retTerms[index++] = (String) setWords.next();
        }
        catch (SAXException eSAX) {
        }                
        catch (SchemaNotFoundException eSchema) {
        }                
        catch (IOException eIO) {
        }                
        // ---------------------------------------
        
        return retTerms;
    }
}

