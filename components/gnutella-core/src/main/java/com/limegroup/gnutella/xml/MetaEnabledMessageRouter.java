package com.limegroup.gnutella.xml;

import com.limegroup.gnutella.*;
import java.io.*;
import com.sun.java.util.collections.*;


/** A special meta-enabled Message router.  Inherits much from 
 *  StandardMessageRouter, just given extra functionality when
 *  xml comes into focus.
 */
public class MetaEnabledMessageRouter extends StandardMessageRouter {


    public MetaEnabledMessageRouter(ActivityCallback callback,FileManager fm) {
        super(callback,fm);
    }
  

    private final boolean debugOn = false;
    private void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }


    /** If there is special processing needed to building a query reply,
     * subclasses can override this method as necessary.
     * @return the query reply - may return null.
     */
    protected List createQueryReply(byte[] guid, byte ttl, int port, 
                                    byte[] ip , long speed, Response[] res,
                                    byte[] clientGUID, boolean notIncoming,
                                    boolean busy, boolean uploaded, 
                                    boolean measuredSpeed, 
                                    boolean supportsChat) {
        
        List queryReplies = new ArrayList();
        QueryReply queryReply = null;

        // get the xml collection string...
        String xmlCollectionString = 
        LimeXMLDocumentHelper.getAggregateString(res);
        if (xmlCollectionString == null)
            xmlCollectionString = "";

        // it may be too big....
        if (xmlCollectionString.length() > QueryReply.XML_MAX_SIZE) {
            // ok, need to partition responses up once again and send out
            // multiple query replies.....
            List splitResps = new LinkedList();
            splitAndAddResponses(splitResps, res);

            while (!splitResps.isEmpty()) {
                Response[] currResps = (Response[]) splitResps.remove(0);
                String currXML = 
                LimeXMLDocumentHelper.getAggregateString(currResps);
                if ((currXML.length() > QueryReply.XML_MAX_SIZE) &&
                    (currResps.length > 1)) 
                    splitAndAddResponses(splitResps, currResps);
                else {
                    // create xml bytes if possible...
                    byte[] xmlCompressed = null;
                    if ((currXML != null) &&
                        (!currXML.equals("")))
                        xmlCompressed = 
                        LimeXMLUtils.compress(currXML.getBytes());
                    else
                        xmlCompressed = new byte[0];
                    
                    try {
                        // create the new queryReply
                        queryReply = new QueryReply(guid, ttl, port, ip, speed, 
                                                    currResps, _clientGUID, 
                                                    xmlCompressed, notIncoming, 
                                                    busy, uploaded, 
                                                    measuredSpeed, supportsChat);
                        queryReplies.add(queryReply);
                    }
                    catch (IllegalArgumentException ignored) {
                    }
                }
            }

        }
        else {  // xml is small enough, no problem.....
            // get xml bytes if possible....
            byte[] xmlCompressed = null;
            if ((xmlCollectionString != null) &&
                (!xmlCollectionString.equals("")))
                xmlCompressed = 
                LimeXMLUtils.compress(xmlCollectionString.getBytes());
            else
                xmlCompressed = new byte[0];
            
            try {
                // create the new queryReply
                queryReply = new QueryReply(guid, ttl, port, ip, speed, res, 
                                            _clientGUID, xmlCompressed,
                                            notIncoming, busy, uploaded, 
                                            measuredSpeed,supportsChat);
                queryReplies.add(queryReply);
            }
            catch (IllegalArgumentException ignored) {
            }
        }

        return queryReplies;
    }

    
    /** @return Simply splits the input array into two (almost) equally sized
     *  arrays.
     */
    private Response[][] splitResponses(Response[] in) {
        int middle = in.length/2;
        Response[][] retResps = new Response[2][];
        retResps[0] = new Response[middle];
        retResps[1] = new Response[in.length-middle];
        for (int i = 0; i < middle; i++)
            retResps[0][i] = in[i];
        for (int i = 0; i < (in.length-middle); i++)
            retResps[1][i] = in[i+middle];
        return retResps;
    }

    private void splitAndAddResponses(List addTo, Response[] toSplit) {
        Response[][] splits = splitResponses(toSplit);
        addTo.add(splits[0]);
        addTo.add(splits[1]);
    }

}


