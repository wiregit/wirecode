package com.limegroup.gnutella.search;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.xml.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import com.sun.java.util.collections.*;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Handles incoming search results from the network.  This class parses the 
 * results from <tt>QueryReply</tt> instances and performs the logic 
 * necessary to pass those results up to the UI.
 */
public final class SearchResultHandler {
    
    private static final Log LOG =
        LogFactory.getLog(SearchResultHandler.class);
        
    /**
     * The maximum amount of time to allow a query's processing
     * to pass before giving up on it as an 'old' query.
     */
    private static final int QUERY_EXPIRE_TIME = 30 * 1000; // 30 seconds.

    /** 
	 * The maximum number of queries to buffer at any time. 
	 */
    private static final int BUFFER_SIZE = 2000;

    /** 
     * The maximum number of results to send in a QueryStatusResponse -
     * basically sent to say 'shut off query'.
	 */
    public static final int MAX_RESULTS = 65535;

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
    public static final int REPORT_INTERVAL = 15;

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
		Thread resultThread = new ManagedThread(resultRunner, "Search Result Thread");
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
     * EVERY TIME you start a query so we can leaf guide it when possible.
     *
     * @param qr The query that has been started.  We really just acces the guid.
     */ 
    public void addQuery(QueryRequest qr) {
        LOG.trace("entered SearchResultHandler.addQuery(QueryRequest)");
        GuidCount gc = new GuidCount(qr);
        GUID_COUNTS.add(gc);
    }

    /**
     * Removes the Query frome the list of queries kept track of.  You should do
     * this EVERY TIME you stop a query.
     *
     * @param guid the guid of the query that has been removed.
     */ 
    public void removeQuery(GUID guid) {
        LOG.trace("entered SearchResultHandler.removeQuery(GUID)");
        GuidCount gc = removeQueryInternal(guid);
        if ((gc != null) && (!gc.isFinished())) {
            // shut off the query at the UPs - it wasn't finished so it hasn't
            // been shut off - at worst we may shut it off twice, but that is
            // a timing issue that has a small probability of happening, no big
            // deal if it does....
            QueryStatusResponse stat = new QueryStatusResponse(guid, 
                                                               MAX_RESULTS);
            RouterService.getConnectionManager().updateQueryStatus(stat);
        }
    }

    /**
     * Returns a <tt>List</tt> of queries that require replanting into
     * the network, based on the number of results they've had and/or
     * whether or not they're new enough.
     */
    public List getQueriesToReSend() {
        LOG.trace("entered SearchResultHandler.getQueriesToSend()");
        List reSend = null;
        synchronized (GUID_COUNTS) {
            long now = System.currentTimeMillis();
            Iterator iter = GUID_COUNTS.iterator();
            while (iter.hasNext()) {
                GuidCount currGC = (GuidCount) iter.next();
                if( isQueryStillValid(currGC, now) ) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("adding " + currGC + 
                                  " to list of queries to resend");
                    if( reSend == null )
                        reSend = new LinkedList();
                    reSend.add(currGC.getQueryRequest());
                }
            }
        }
        if( reSend == null )
            return DataUtils.EMPTY_LIST;
        else
            return reSend;
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
    
    /**
     * Determines whether or not the specified 
    
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
					}
					
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
            LOG.debug("bad packet reading qr", bpe);
            return false;
        }

        // always handle reply to multicast queries.
        if( !data.isReplyToMulticastQuery() && !qr.isBrowseHostReply() ) {
            // note that the minimum search quality will always be greater
            // than -1, so -1 qualities (the impossible case) are never
            // displayed
            if(data.getQuality() < SearchSettings.MINIMUM_SEARCH_QUALITY.getValue()) {
                LOG.debug("Ignoring because low quality");
                return false;
            }
            if(data.getSpeed() < SearchSettings.MINIMUM_SEARCH_SPEED.getValue()) {
                LOG.debug("Ignoring because low speed");
                return false;
            }
            // if the other side is firewalled AND
            // we're not on close IPs AND
            // (we are firewalled OR we are a private IP) AND 
            // no chance for FW transfer then drop the reply.
            if(data.isFirewalled() && 
               !NetworkUtils.isVeryCloseIP(qr.getIPBytes()) &&               
               (!RouterService.acceptedIncomingConnection() ||
                NetworkUtils.isPrivateAddress(RouterService.getAddress())) &&
               !(UDPService.instance().canReceiveSolicited() && 
                 qr.getSupportsFWTransfer())
               )  {
               LOG.debug("Ignoring from firewall funkiness");
               return false;
            }
        }

        List results = null;
        try {
            results = qr.getResultsAsList();
        } catch (BadPacketException e) {
            LOG.debug("Error gettig results", e);
            return false;
        }
        
        // get xml collection string, then get dis-aggregated docs, then 
        // in loop
        // you can match up metadata to responses
        String xmlCollectionString = "";
        try {
            LOG.trace("Trying to do uncompress XML.....");
            byte[] xmlCompressed = qr.getXMLBytes();
            if (xmlCompressed.length > 1) {
                byte[] xmlUncompressed = LimeXMLUtils.uncompress(xmlCompressed);
                xmlCollectionString = new String(xmlUncompressed,"UTF-8");
            }
        }
        catch (UnsupportedEncodingException use) {
            //b/c this should never happen, we will show and error
            //if it ever does for some reason.
            //we won't throw a BadPacketException here but we will show it.
            //the uee will effect the xml part of the reply but we could
            //still show the reply so there shouldn't be any ill effect if
            //xmlCollectionString is ""
            ErrorService.error(use);
        }
        catch (IOException ignored) {}

        if(LOG.isDebugEnabled())
            LOG.debug("xmlCollectionString = " + xmlCollectionString);
        List allDocsArray = LimeXMLDocumentHelper.getDocuments(xmlCollectionString, 
															   results.size());
        Iterator iter = results.iterator();
        int numSentToFrontEnd = 0;
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
            numSentToFrontEnd++;
        } //end of response loop

        // ok - some responses may have got through to the GUI, we should account
        // for them....
        accountAndUpdateDynamicQueriers(qr, numSentToFrontEnd);

        return (numSentToFrontEnd > 0);
    }


    private void accountAndUpdateDynamicQueriers(final QueryReply qr,
                                                 final int numSentToFrontEnd) {

        LOG.trace("SRH.accountAndUpdateDynamicQueriers(): entered.");
        // we should execute if results were consumed
        // technically Ultrapeers don't use this info, but we are keeping it
        // around for further use
        if (numSentToFrontEnd > 0) {
            // get the correct GuidCount
            GuidCount gc = retrieveGuidCount(new GUID(qr.getGUID()));
            if (gc == null)
                // 0. probably just hit lag, or....
                // 1. we could be under attack - hits not meant for us
                // 2. programmer error - ejected a query we should not have
                return;
            
            // update the object
            LOG.trace("SRH.accountAndUpdateDynamicQueriers(): incrementing.");
            gc.increment(numSentToFrontEnd);

            // inform proxying Ultrapeers....
            if (RouterService.isShieldedLeaf()) {
                if (!gc.isFinished() && 
                    (gc.getNumResults() > gc.getNextReportNum())) {
                    LOG.trace("SRH.accountAndUpdateDynamicQueriers(): telling UPs.");
                    gc.tallyReport();
                    if (gc.getNumResults() > QueryHandler.ULTRAPEER_RESULTS)
                        gc.markAsFinished();
                    // if you think you are done, then undeniably shut off the
                    // query.
                    final int numResultsToReport = (gc.isFinished() ?
                                                    MAX_RESULTS :
                                                    gc.getNumResults()/4);
                    QueryStatusResponse stat = 
                        new QueryStatusResponse(gc.getGUID(), 
                                                numResultsToReport);
                    RouterService.getConnectionManager().updateQueryStatus(stat);
                }

            }
        }
        LOG.trace("SRH.accountAndUpdateDynamicQueriers(): returning.");
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
    
    /**
     * Determines whether or not the query contained in the
     * specified GuidCount is still valid.
     * This depends on values such as the time the query was
     * created and the amount of results we've received so far
     * for this query.
     */
    private boolean isQueryStillValid(GuidCount gc, long now) {
        LOG.trace("entered SearchResultHandler.isQueryStillValid(GuidCount)");
        return (now < (gc.getTime() + QUERY_EXPIRE_TIME)) &&
               (gc.getNumResults() < QueryHandler.ULTRAPEER_RESULTS);
    }

    /*---------------------------------------------------    
      END OF PRIVATE INTERFACE METHODS
     ----------------------------------------------------*/
    
    /** A container that simply pairs a GUID and an int.  The int should
     *  represent the number of non-filtered results for the GUID.
     */
    private static class GuidCount {

        private final long _time;
        private final GUID _guid;
        private final QueryRequest _qr;
        private int _numResults;
        private int _nextReportNum = REPORT_INTERVAL;
        private boolean markAsFinished = false;
        
        public GuidCount(QueryRequest qr) {
            _qr = qr;
            _guid = new GUID(qr.getGUID());
            _numResults = 0;
            _time = System.currentTimeMillis();
        }

        public GUID getGUID() { return _guid; }
        public int getNumResults() { return _numResults; }
        public int getNextReportNum() { return _nextReportNum; }
        public long getTime() { return _time; }
        public QueryRequest getQueryRequest() { return _qr; }
        public boolean isFinished() { return markAsFinished; }
        public void tallyReport() { 
            _nextReportNum = _numResults + REPORT_INTERVAL; 
        }

        public void increment(int incr) { _numResults += incr; }
        public void markAsFinished() { markAsFinished = true; }

        public String toString() {
            return "" + _guid + ":" + _numResults + ":" + _nextReportNum;
        }
    }

}
