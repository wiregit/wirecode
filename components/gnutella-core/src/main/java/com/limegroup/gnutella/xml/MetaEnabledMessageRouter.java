package com.limegroup.gnutella.xml;

import com.limegroup.gnutella.*;
import java.io.*;
import java.util.List;

/** A special meta-enabled Message router.  Inherits much from 
 *  StandardMessageRouter, just given extra functionality when
 *  xml comes into focus.
 */
public class MetaEnabledMessageRouter extends StandardMessageRouter {


    public MetaEnabledMessageRouter(ActivityCallback callback) {
        super(callback);
    }
  

    private final boolean debugOn = false;
    private void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }


    public void sendResponses(Response[] responses, QueryRequest queryRequest,
                              Acceptor acceptor, byte[] clientGUID) {
        
        debug("MetaEnabledMessageRouter.sendResponses(): entered.");

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
            (new LimeXMLDocumentHelper()).getAggregateString(res);
            if (xmlCollectionString == null)
                xmlCollectionString = "";
            debug("xmlCollectionString.length(): " + 
                  xmlCollectionString.length() + 
                  "\nvalue = " + xmlCollectionString);


            // create the new queryReply
            try {
                byte[] xmlCompressed = LimeXMLUtils.compress(xmlCollectionString.getBytes());
                queryReply = new QueryReply(guid, ttl, port, ip,
                                            speed, res, clientGUID, 
                                            xmlCompressed,
                                            !incoming, busy, uploaded, 
                                            measuredSpeed);
            }
            catch (Exception e) {
                // if i couldn't construct it, do nothing....
                e.printStackTrace();
                return;
            }

            // try to send the new queryReply
            try {
                sendQueryReply(queryReply);
            } 
            catch (IOException e) {
                // if there is an error, do nothing..
            }

            // we only want to send multiple queryReplies
            // if the number of hops is small.
            if (numHops > 2) {
                break;
            }

        }//end of while

        debug("MetaEnabledMessageRouter.sendResponses(): returning.");
    }


}


