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


    public void sendResponses(Response[] responses, QueryRequest queryRequest,
                              Acceptor acceptor, byte[] clientGUID) {
        
        debug("MetaEnabledMessageRouter.sendResponses(): entered.");
        
        boolean supportsChat = SettingsManager.instance().getChatEnabled();
        
        // if either there are no responses or, the
        // response array came back null for some reason,
        // exit this method
        if ( (responses == null) || ((responses.length < 1)) )
            return;

        // get the appropriate queryReply information
        byte[] guid = queryRequest.getGUID();
        byte ttl = (byte)(queryRequest.getHops() + 1);
        int port = acceptor.getPort();
        byte[] ip = acceptor.getAddress();

        //Return measured speed if possible, or user's speed otherwise.
        long speed = _uploadManager.measuredUploadSpeed();
        boolean measuredSpeed=true;
        if (speed==-1) {
            speed=SettingsManager.instance().getConnectionSpeed();
            measuredSpeed=false;
        }

        int numResponses = responses.length;
        int index = 0;

        int numHops = queryRequest.getHops();

        QueryReply queryReply = null;

        // modified by rsoule, 11/16/00

        while (numResponses > 0) {
            int arraySize;
            // if there are more than 255 responses,
            // create an array of 255 to send in the queryReply
            // otherwise, create an array of whatever size is left.
            if (numResponses < 255) {
                // break;
                arraySize = numResponses;
            }
            else
                arraySize = 255;

            Response[] res;
            // a special case.  in the common case where there
            // are less than 256 responses being sent, there
            // is no need to copy one array into another.
            if ( (index == 0) && (arraySize < 255) ) {
                res = responses;
            }
            else {
                res = new Response[arraySize];
                // copy the reponses into bite-size chunks
                for(int i =0; i < arraySize; i++) {
                    res[i] = responses[index];
                    index++;
                }
            }

            // decrement the number of responses we have left
            numResponses-= arraySize;

			// see id there are any open slots
			boolean busy = _uploadManager.isBusy();
            boolean uploaded = _uploadManager.hadSuccesfulUpload();

			// see if we have ever accepted an incoming connection
			boolean incoming = _acceptor.acceptedIncoming();

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
                        try {

                            // create xml bytes if possible...
                            byte[] xmlCompressed = null;
                            if ((currXML != null) &&
                                (!currXML.equals("")))
                                xmlCompressed = 
                                LimeXMLUtils.compress(currXML.getBytes());
                            else
                                xmlCompressed = new byte[0];

                            // create the new queryReply
                            queryReply = new QueryReply(guid, ttl, 
                                                        port, ip,
                                                        speed, currResps, 
                                                        clientGUID, 
                                                        xmlCompressed,
                                                        !incoming, 
                                                        busy, 
                                                        uploaded, 
                                                        measuredSpeed,
                                                        supportsChat);
                        }
                        catch (Exception e) {
                            continue;  // this can only happen VERY rarely...
                        }
                        
                        // try to send the new queryReply
                        try {
                            sendQueryReply(queryReply);
                        } 
                        catch (IOException e) {
                            // if there is an error, do nothing..
                        }        
                    }
                }
            }
            else {  // xml is small enough, no problem.....
                try {
                    
                    // get xml bytes if possible....
                    byte[] xmlCompressed = null;
                    if ((xmlCollectionString != null) &&
                        (!xmlCollectionString.equals("")))
                        xmlCompressed = 
                        LimeXMLUtils.compress(xmlCollectionString.getBytes());
                    else
                        xmlCompressed = new byte[0];
                    
                    // create the new queryReply
                    queryReply = new QueryReply(guid, ttl, port, ip,
                                                speed, res, 
                                                clientGUID, 
                                                xmlCompressed,
                                                !incoming, busy, 
                                                uploaded, 
                                                measuredSpeed,supportsChat);
                }
                catch (Exception e) {
                    return; // never expect this....
                }
                
                // try to send the new queryReply
                try {
                    sendQueryReply(queryReply);
                } 
                catch (IOException e) {
                    // if there is an error, do nothing..
                }        
            }

            // we only want to send multiple queryReplies
            // if the number of hops is small.
            if (numHops > 2) {
                break;
            }

        }//end of while

        debug("MetaEnabledMessageRouter.sendResponses(): returning.");
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


