package com.limegroup.gnutella.search;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.xml.*;

import org.xml.sax.SAXException;
import java.io.IOException;
import java.net.*;

import com.sun.java.util.collections.*;

/**
 * Handles incoming search results from the network.  This class parses the 
 * results from <tt>QueryReply</tt> instances and performs the logic 
 * necessary to pass those results up to the UI.
 */
public final class SearchResultHandler {

    /** 
	 * The maximum number of queries to buffer at any time. 
	 */
    private static final int BUFFER_SIZE = 2000;

    /** 
	 * The maximum number of replies to display per SECOND.  Must be greater
     * than 0. Note that one query reply may have many (up to 255!) results. 
	 */
    private static final int MAX_RATE = 200;

    /** 
	 * The (amortized) min time to wait between any two results, in MSECS.
     * DELAY_TIME==1/(MAX_RATE [replies/sec] * .001 [sec/msec]) 
	 */
    private static final int DELAY_TIME = 1000/MAX_RATE;

    /** 
	 * The queue of buffered query replies.  Used to decouple backend,
     * grouping, and Swing threads.  Also needed for rate limiting.  Follows
     * stack policy, with replies added to and removed from tail.  This means
     * that a slow older search won't prevent newer results from coming in. We
     * use a linked list instead of standard buffer because it shrinks in size
     * as needed.  We limit the size in handleQueryReply. 
	 */
    private final LinkedList /* of QueryReply */ REPLIES = new LinkedList();

    /** 
	 * The time of the last result passed to the GUI, as returned by
     * System.currentTimeMillis().  Used for rate limiting. 
	 */
    private long lastTime;


	/**
	 * Starts the thread that processes search results.
	 */
	public void start() {
        //Start REPLIES consumer thread.
		Runnable resultRunner = new ReplyProcessor();
		Thread resultThread = new Thread(resultRunner, "Search Result Thread");
        resultThread.setDaemon(true);
        resultThread.start();
	}

    /**
     * Adds the query reply.  It may take some time to actually process
     * the result.
	 *
	 * @param qr the <tt>QueryReply</tt> to add
     */
    public void handleQueryReply(QueryReply qr) {
        synchronized (REPLIES) {
            REPLIES.addLast(qr);
            //Ensure bounds on size of REPLIES.
            if (REPLIES.size() > BUFFER_SIZE)
                REPLIES.removeFirst();
            REPLIES.notify();
        }
    }


	/**
	 * Private class for processing replies as they come in -- does some
	 * buffering to avoid brining the ui thread to a crawl.
	 */
    private class ReplyProcessor implements Runnable {
        public void run() {
			try {
				while (true) {
					//1. Wait for result.
					QueryReply qr = null;
					synchronized (REPLIES) {
						while (REPLIES.isEmpty()) {
							try {
								REPLIES.wait();
							} catch (InterruptedException e) { }
						}
						qr = (QueryReply)REPLIES.removeLast();
					}
					
					//2. Look at time. If not enough time has elapsed, sleep
					//long enough so that
					//       (elapsed+sleepTime)/qr.getResultCount()==DELAY_TIME
					long now = System.currentTimeMillis();
					long elapsed = now-lastTime;
					long sleepTime = DELAY_TIME*qr.getResultCount()-elapsed;
					if (sleepTime>0) {
						try {
							Thread.sleep(sleepTime);
						} catch (InterruptedException e) { }
					};
					
					//3. Actually handle this.
					boolean displayed = handleReply(qr);
					if (displayed) 
						lastTime=System.currentTimeMillis();
				}
			} catch(Throwable t) {
				ErrorService.error(t);
			}
		}
	}


    /** 
	 * Handles the given query reply. Only one thread may call it at a time.
     *      
	 * @return <tt>true</tt> if the GUI will (probably) display the results,
	 *  otherwise <tt>false</tt> 
     */
    private boolean handleReply(final QueryReply qr) {
        HostData data;
        try {
            data = qr.getHostData();
        } catch(BadPacketException bpe) {
            return false;
        }
        
        // always handle reply to multicast queries.
        if( !data.isReplyToMulticastQuery() ) {
            // note that the minimum search quality will always be greater
            // than -1, so -1 qualities (the impossible case) are never
            // displayed
            if(data.getQuality() < SearchSettings.MINIMUM_SEARCH_QUALITY.getValue()) {
                return false;
            }
            if(data.getSpeed() < SearchSettings.MINIMUM_SEARCH_SPEED.getValue()) {
                return false;
            }
            // if the other side is firewalled AND
            // we're not on close IPs AND
            // (we are firewalled OR we are a private IP)
            // then drop the reply.
            if(data.isFirewalled() && 
               !NetworkUtils.isVeryCloseIP(qr.getIPBytes()) &&               
               (!RouterService.acceptedIncomingConnection() ||
                NetworkUtils.isPrivateAddress(RouterService.getAddress()))
               )  {
               return false;
            }
        }

        List results = null;
        try {
            results = qr.getResultsAsList();
        } catch (BadPacketException e) {
            return false;
        }
        
        // get xml collection string, then get dis-aggregated docs, then 
        // in loop
        // you can match up metadata to responses
        String xmlCollectionString = "";
        try {
            debug("Trying to do uncompress.....");
            byte[] xmlCompressed = qr.getXMLBytes();
            if (xmlCompressed.length > 1) {
                byte[] xmlUncompressed = LimeXMLUtils.uncompress(xmlCompressed);
                xmlCollectionString = new String(xmlUncompressed);
            }
        } catch (IOException ignored) {}

        debug("xmlCollectionString = " + xmlCollectionString);
        List allDocsArray = LimeXMLDocumentHelper.getDocuments(xmlCollectionString, 
															   results.size());
        Iterator iter = results.iterator();
        for(int currentResponse = 0; iter.hasNext(); currentResponse++) {
            Response response = (Response)iter.next();
            if (!RouterService.matchesType(data.getMessageGUID(), response))
                continue;
            //Throw away results from Mandragore Worm
            if (RouterService.isMandragoreWorm(data.getMessageGUID(),response))
                continue;
            
            LimeXMLDocument doc = null;
            // If there was no XML in the response itself, try to create
            // a doc from the EQHD.
            if(xmlCollectionString!=null && xmlCollectionString.equals("")) {
                // The RFD is only going to use one doc, so only
                // take one.
                LimeXMLDocument[] metaDocs;
                for(int schema = 0; schema < allDocsArray.size(); schema++) {
                    metaDocs = (LimeXMLDocument[])allDocsArray.get(schema);
                    // If there are no documents in this schema, try another.
                    if(metaDocs == null)
                        continue;
                    // If this schema had a document for this response, use it.
                    if(metaDocs[currentResponse] != null) {
                        doc = metaDocs[currentResponse];
                        break; // we only need one, so break out.
                    }
                }    
            }
            
            RemoteFileDesc rfd = response.toRemoteFileDesc(data, doc);
            Set alts = response.getLocations();
			RouterService.getCallback().handleQueryResult(rfd, data, alts);

        } //end of response loop
        return true;
    }

    private final boolean debugOn = false;
    private void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
}
