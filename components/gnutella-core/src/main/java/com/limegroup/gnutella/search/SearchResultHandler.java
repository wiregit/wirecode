package com.limegroup.gnutella.search;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.xml.*;

/**
 * The search panel.  Initiates search, displays search results, and provides
 * controls for the user to start downloads.<p>
 *
 * This provides the primary external interface to the search package.
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

	private final Map GUID_MAP = new HashMap();

    /**
	 * Creates a new <tt>SearchResultPipeliner</tt> instance.
     */
    //public SearchResultHandler() {
    //}

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

	public void addGuid(GUID key) {
		synchronized(GUID_MAP) {
			if(GUID_MAP.containsKey(key)) return;
			GUID_MAP.put(key, new Integer(0));
		}
	}

	public void removeGuid(GUID key) {
		synchronized(GUID_MAP) {
			GUID_MAP.remove(key);
		}
	}

	public int getNumResults(GUID key) {
		synchronized(GUID_MAP) {
			Integer results = (Integer)GUID_MAP.get(key);
			return results.intValue();
		}
	}

	private void addResult(GUID key) {
		synchronized(GUID_MAP) {
			if(!GUID_MAP.containsKey(key))
				throw new IllegalArgumentException("GUID not in map: "+key);
			Integer results = (Integer)GUID_MAP.get(key);
			int newResults = results.intValue();
			newResults++;
			GUID_MAP.put(key, new Integer(newResults));
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
				RouterService.error(t);
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
		SettingsManager settings = SettingsManager.instance();
		HostData data = new HostData(qr);

		// note that the minimum search quality will always be greater
		// than -1, so -1 qualities (the impossible case) are never
		// displayed
		if(data.getQuality() < settings.getMinimumSearchQuality()) return false;
		if(data.getSpeed() < settings.getMinimumSearchSpeed()) return false;

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
        }
        catch (Exception e) {
            // so what, i couldn't get xml, no biggie, don't kill everyone for
            // no reason...
            xmlCollectionString = "";
        }
        debug("xmlCollectionString = " + xmlCollectionString);
        List allDocsArray = LimeXMLDocumentHelper.getDocuments(xmlCollectionString, 
															   results.size());
        int z = allDocsArray.size();
        int numResults = results.size();
        int k = -1;//for counting iterations...initialized to -1

		byte[] replyGUID = data.getMessageGUID();
        final boolean isSpecificXMLSearch = 
			RouterService.isSpecificXMLSearch(replyGUID);

        Iterator iter = results.iterator();
        while(iter.hasNext()) {
            k++;
            Response response = (Response)iter.next();
            if (! RouterService.matchesType(replyGUID, response))
                continue;
            //Throw away results from Mandragore Worm
            if (RouterService.isMandragoreWorm(replyGUID, response))
                continue;
            //Throw away files that are two big to fit in an integer.
            long size = response.getSize();
            long index = response.getIndex();
            long maxIndex = LimeXMLProperties.DEFAULT_NONFILE_INDEX;
            if (size>Integer.MAX_VALUE || index > maxIndex)
                continue;
            int score = RouterService.score(replyGUID,response);
            if(isSpecificXMLSearch && (score == 0)) continue;
            
            ArrayList docs = null;
            if(xmlCollectionString==null || xmlCollectionString.equals("")){
                //Note:This means no XML in QHD. create docs from between nulls
                LimeXMLDocument doc;//there is only going to be one Document
                try {
                    String xmlStr = response.getMetadata();
                    doc = new LimeXMLDocument(xmlStr);
                }catch(Exception e){//could not create documnet
                    doc = null;
                }
                if(doc==null)
                    docs = null;
                else{
                    docs = new ArrayList();
                    docs.add(doc);
                }
            }
            else{//XML in QHD....make documents from there
                //lets gather the documents
                docs = new ArrayList(z);//size = number of schemas
                LimeXMLDocument[] metaDocs;
                for(int l=0; l<z;l++){//for each schema
                    metaDocs = (LimeXMLDocument[])allDocsArray.get(l);
                    if(metaDocs == null)
                        continue;
                    if(metaDocs[k]!=null)
                        //add doc corresponding to response from outer loop
                        docs.add(metaDocs[k]);
                }    
            }
						
			RouterService.getCallback().handleQueryResult(data, response, docs);
			addResult(new GUID(replyGUID));
        } //end of response loop
        return true;
    }

    private final boolean debugOn = false;
    private void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
}
