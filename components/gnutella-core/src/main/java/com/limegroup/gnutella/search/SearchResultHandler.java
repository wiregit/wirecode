package com.limegroup.gnutella.search;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
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
     * The "delay" between responses to wait to send a QueryStatusResponse.
     */
    public static final int REPORT_INTERVAL = 5;

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


    /** Used to keep track of the number of non-filtered responses per GUID.
     *  I need synchronization for every call I make, so a Vector is fine.
     */
    private final List GUID_COUNTS = new Vector();

    /*---------------------------------------------------    
      PUBLIC INTERFACE METHODS
     ----------------------------------------------------*/

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
     * Adds the Query to the list of queries kept track of.  You should do this
     * EVERYTIME you start a query so we can leaf guide it when possible.
     *
     * @param qr The query that has been started.  We really just acces the guid.
     */ 
    public void addQuery(QueryRequest qr) {
        GuidCount gc = new GuidCount(qr.getGUID());
        GUID_COUNTS.add(gc);
    }

    /**
     * Removes the Query frome the list of queries kept track of.  You should do
     * this EVERYTIME you stop a query.
     *
     * @param guid the guid of the query that has been removed.
     */ 
    public void removeQuery(GUID guid) {
        if (removeQueryInternal(guid) != null) {
            // shut off the query at the UPs....
            try {
                QueryStatusResponse stat = new QueryStatusResponse(guid, 65535);
                RouterService.getConnectionManager().updateQueryStatus(stat);
            }
            catch (BadPacketException terrible) {
                ErrorService.error(terrible);
            }
        }
    }


    /**
     * Use this to see how many results have been displayed to the user for the
     * specified query.
     *
     * @param guid the guid of the query.
     *
     * @return the number of non-filtered results for query with guid guid. -1
     * is returned if the guid was not found....
     */    
    public int getNumResultsForQuery(GUID guid) {
        GuidCount gc = retrieveGuidCount(guid);
        if (gc != null)
            return gc.getNumResults();
        else
            return -1;
    }
    
    /*---------------------------------------------------    
      END OF PUBLIC INTERFACE METHODS
     ----------------------------------------------------*/

    /*---------------------------------------------------    
      PRIVATE INTERFACE METHODS
     ----------------------------------------------------*/

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
        int numSentToBackEnd = 0;
        for(int currentResponse = 0; iter.hasNext(); currentResponse++) {
            Response response = (Response)iter.next();
            if (!RouterService.matchesType(data.getMessageGUID(), response))
                continue;
            //Throw away results from Mandragore Worm
            if (RouterService.isMandragoreWorm(data.getMessageGUID(),response))
                continue;
            
            // If there was no XML in the response itself, try to create
            // a doc from the EQHD.
            if(xmlCollectionString!=null && !xmlCollectionString.equals("")) {
                LimeXMLDocument[] metaDocs;
                for(int schema = 0; schema < allDocsArray.size(); schema++) {
                    metaDocs = (LimeXMLDocument[])allDocsArray.get(schema);
                    // If there are no documents in this schema, try another.
                    if(metaDocs == null)
                        continue;
                    // If this schema had a document for this response, use it.
                    if(metaDocs[currentResponse] != null) {
                        response.setDocument(metaDocs[currentResponse]);
                        break; // we only need one, so break out.
                    }
                }
            }
            
            RemoteFileDesc rfd = response.toRemoteFileDesc(data);
            Set alts = response.getLocations();
			RouterService.getCallback().handleQueryResult(rfd, data, alts);
            numSentToBackEnd++;
        } //end of response loop

        // ok - some responses may have got through to the GUI, we should account
        // for them....
        accountAndUpdateDynamicQueriers(qr, numSentToBackEnd);

        return (numSentToBackEnd > 0);
    }


    private void accountAndUpdateDynamicQueriers(final QueryReply qr,
                                                 final int numSentToBackEnd) {

        debug("SRH.accountAndUpdateDynamicQueriers(): entered.");
        // we should execute if results were consumed
        // technically Ultrapeers don't use this info, but we are keeping it
        // around for further use
        if (numSentToBackEnd > 0) {
            // get the correct GuidCount
            GuidCount gc = retrieveGuidCount(new GUID(qr.getGUID()));
            if (gc == null)
                // 0. probably just hit lag, or....
                // 1. we could be under attack - hits not meant for us
                // 2. programmer error - ejected a query we should not have
                return;
            
            // update the object
            debug("SRH.accountAndUpdateDynamicQueriers(): incrementing.");
            gc.increment(numSentToBackEnd);

            // inform proxying Ultrapeers....
            if (RouterService.isShieldedLeaf()) {
                if (gc.getNumResults() > gc.getNextReportNum()) {
                    debug("SRH.accountAndUpdateDynamicQueriers(): telling UPs.");
                    gc.tallyReport();
                    try {
                        QueryStatusResponse stat = 
                            new QueryStatusResponse(gc.getGUID(), 
                                                    gc.getNumResults()/4);
                        RouterService.getConnectionManager().updateQueryStatus(stat);
                    }
                    catch (BadPacketException terrible) {
                        ErrorService.error(terrible);
                    }
                }

            }
        }
        debug("SRH.accountAndUpdateDynamicQueriers(): returning.");
    }


    private GuidCount removeQueryInternal(GUID guid) {
        synchronized (GUID_COUNTS) {
            Iterator iter = GUID_COUNTS.iterator();
            while (iter.hasNext()) {
                GuidCount currGC = (GuidCount) iter.next();
                if (currGC.getGUID().equals(guid)) {
                    iter.remove();  // get rid of this dude
                    return currGC;  // and return it...
                }
            }
        }
        return null;
    }


    private GuidCount retrieveGuidCount(GUID guid) {
        synchronized (GUID_COUNTS) {
            Iterator iter = GUID_COUNTS.iterator();
            while (iter.hasNext()) {
                GuidCount currGC = (GuidCount) iter.next();
                if (currGC.getGUID().equals(guid))
                    return currGC;
            }
        }
        return null;
    }

    /*---------------------------------------------------    
      END OF PRIVATE INTERFACE METHODS
     ----------------------------------------------------*/

    private final boolean debugOn = false;
    private void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }

    
    /** A container that simply pairs a GUID and an int.  The int should
     *  represent the number of non-filtered results for the GUID.
     */
    private static class GuidCount {

        private final GUID _guid;
        private int _numResults;
        private int _nextReportNum = REPORT_INTERVAL;

        public GuidCount(byte[] guid) {
            _guid = new GUID(guid);
            _numResults = 0;
        }

        public GUID getGUID() { return _guid; }
        public int getNumResults() { return _numResults; }
        public int getNextReportNum() { return _nextReportNum; }
        public void tallyReport() { 
            _nextReportNum = _numResults + REPORT_INTERVAL; 
        }

        public void increment(int incr) { _numResults += incr; }
        
        public String toString() {
            return "" + _guid + ":" + _numResults + ":" + _nextReportNum;
        }
    }

}
