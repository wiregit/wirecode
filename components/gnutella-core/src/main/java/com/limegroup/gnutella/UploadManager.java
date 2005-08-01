package com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.downloader.Interval;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPRequestMethod;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.uploader.FreeloaderUploadingException;
import com.limegroup.gnutella.uploader.HTTPUploader;
import com.limegroup.gnutella.uploader.LimitReachedUploadState;
import com.limegroup.gnutella.uploader.PushProxyUploadState;
import com.limegroup.gnutella.uploader.StalledUploadWatchdog;
import com.limegroup.gnutella.util.Buffer;
import com.limegroup.gnutella.util.FixedSizeExpiringSet;
import com.limegroup.gnutella.util.FixedsizeForgetfulHashMap;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.KeyValue;
import com.limegroup.gnutella.util.URLDecoder;

/**
 * This class parses HTTP requests and delegates to <tt>HTTPUploader</tt>
 * to handle individual uploads.
 *
 * The state of HTTPUploader is maintained by this class.
 * HTTPUploader's state follows the following pattern:
 *                                                           \ /
 *                             |->---- THEX_REQUEST ------->--|
 *                             |->---- UNAVAILABLE_RANGE -->--|
 *                             |->---- PUSH_PROXY --------->--|
 *                            /-->---- FILE NOT FOUND ----->--|
 *                           /--->---- MALFORMED REQUEST -->--|
 *                          /---->---- BROWSE HOST -------->--|
 *                         /----->---- UPDATE FILE -------->--|
 *                        /------>---- QUEUED ------------->--|
 *                       /------->---- LIMIT REACHED ------>--|
 *                      /-------->---- UPLOADING ---------->--|
 * -->--CONNECTING-->--/                                      |
 *        |                                                  \|/
 *        |                                                   |
 *       /|\                                                  |--->INTERRUPTED
 *        |--------<---COMPLETE-<------<-------<-------<------/      (done)
 *                        |
 *                        |
 *                      (done)
 *
 * The states in the middle (those other than CONNECTING, COMPLETE
 *   and INTERRUPTED) are part of the "State Pattern" and have an 
 * associated class that implements HTTPMessage.
 *
 * These state pattern classes are ONLY set while a transfer is active.
 * For example, after we determine a request should be 'File Not Found',
 * and send the response back, the state will become COMPLETE (unless
 * there was an IOException while sending the response, in which case
 * the state will become INTERRUPTED).  To retrieve the last state
 * that was used for transferring, use HTTPUploader.getLastTransferState().
 *
 * Of particular note is that Queued uploaders are actually in COMPLETED
 * state for the majority of the time.  The QUEUED state is only active
 * when we are actively writing back the 'You are queued' response.
 *
 * COMPLETE uploaders may be using HTTP/1.1, in which case the HTTPUploader
 * recycles back to CONNECTING upon receiving the next GET/HEAD request
 * and repeats.
 *
 * INTERRUPTED HTTPUploaders are never reused.  However, it is possible that
 * the socket may be reused.  This odd case is ONLY possible when a requester
 * is queued for one file and sends a subsequent request for another file.
 * The first HTTPUploader is set as interrupted and a second one is created
 * for the new file, using the same socket as the first one.
 *
 * @see com.limegroup.gnutella.uploader.HTTPUploader
 */
public class UploadManager implements BandwidthTracker {
    
    private static final Log LOG = LogFactory.getLog(UploadManager.class);

    /** An enumeration of return values for queue checking. */
    private final int BYPASS_QUEUE = -1;
    private final int REJECTED = 0;    
    private final int QUEUED = 1;
    private final int ACCEPTED = 2;
    private final int BANNED = 3;
    /** The min and max allowed times (in milliseconds) between requests by
     *  queued hosts. */
    public static final int MIN_POLL_TIME = 45000; //45 sec
    public static final int MAX_POLL_TIME = 120000; //120 sec

	/**
	 * This is a <tt>List</tt> of all of the current <tt>Uploader</tt>
	 * instances (all of the uploads in progress).  
	 */
	private List /* of Uploaders */ _activeUploadList = new LinkedList();

    /** The list of queued uploads.  Most recent uploads are added to the tail.
     *  Each pair contains the underlying socket and the time of the last
     *  request. */
    private List /*of KeyValue (Socket,Long) */ _queuedUploads = 
        new ArrayList();

    
	/** set to true when an upload has been succesfully completed. */
	private volatile boolean _hadSuccesfulUpload=false;
    
    /** Number of force-shared active uploads */
    private int _forcedUploads;
    
	/**
	 * LOCKING: obtain this' monitor before modifying any 
	 * of the data structures
	 */

    /** The number of uploads considered when calculating capacity, if possible.
     *  BearShare uses 10.  Settings it too low causes you to be fooled be a
     *  streak of slow downloaders.  Setting it too high causes you to be fooled
     *  by a number of quick downloads before your slots become filled.  */
    private static final int MAX_SPEED_SAMPLE_SIZE=5;
    /** The min number of uploads considered to give out your speed.  Same 
     *  criteria needed as for MAX_SPEED_SAMPLE_SIZE. */
    private static final int MIN_SPEED_SAMPLE_SIZE=5;
    /** The minimum number of bytes transferred by an uploadeder to count. */
    private static final int MIN_SAMPLE_BYTES=200000;  //200KB
    /** The average speed in kiloBITs/second of the last few uploads. */
    private Buffer /* of Integer */ speeds=new Buffer(MAX_SPEED_SAMPLE_SIZE);
    /** The highestSpeed of the last few downloads, or -1 if not enough
     *  downloads have been down for an accurate sample.
     *  INVARIANT: highestSpeed>=0 ==> highestSpeed==max({i | i in speeds}) 
     *  INVARIANT: speeds.size()<MIN_SPEED_SAMPLE_SIZE <==> highestSpeed==-1
     */
    private volatile int highestSpeed=-1;
    
    /**
     * The number of measureBandwidth's we've had
     */
    private int numMeasures = 0;
    
    /**
     * The current average bandwidth
     */
    private float averageBandwidth = 0f;

    /** The desired minimum quality of service to provide for uploads, in
     *  KB/s.  See testTotalUploadLimit. */
    private static final float MINIMUM_UPLOAD_SPEED=3.0f;
    
    /** 
     * The file index used in this structure to indicate a browse host
     * request
     */
    public static final int BROWSE_HOST_FILE_INDEX = -1;
    
    /**
     * The file index used in this structure to indicate an update-file
     * request
     */
    public static final int UPDATE_FILE_INDEX = -2;
    
    /**
     * The file index used in this structure to indicate a bad URN query.
     */
    public static final int BAD_URN_QUERY_INDEX = -3;
    
    /**
     * The file index used in this structure to indicate a malformed request.
     */
    public static final int MALFORMED_REQUEST_INDEX = -4;

    /** 
     * The file index used in this structure to indicate a Push Proxy 
     * request.
     */
    public static final int PUSH_PROXY_FILE_INDEX = -5;
    
    /** 
     * The file index used in this structure to indicate a HTTP File View
     * download request.
     */
    public static final int FILE_VIEW_FILE_INDEX = -6;
    
    /** 
     * The file index used in this structure to indicate a HTTP Resource Get.
     */
    public static final int RESOURCE_INDEX = -7;

    /** 
     * The file index used in this structure to indicate a special request from a browser.
     */
    public static final int BROWSER_CONTROL_INDEX = -8;

    /**
     * Constant for the beginning of a BrowserControl request.
     */
    public static final String BROWSER_CONTROL_STR = "/browser-control";
    
    /**
     * Constant for HttpRequestLine parameter
     */
    public static final String SERVICE_ID = "service_id";
                
    /**
     * Constant for the beginning of a file-view request.
     */
    public static final String FV_REQ_BEGIN = "/gnutella/file-view";

    /**
     * Constant for file-view gif get.
     */
    public static final String RESOURCE_GET = "/gnutella/res/";

	/**
     * Remembers uploaders to disadvantage uploaders that
     * hammer us for download slots. Stores up to 250 entries
     * Maps IP String to RequestCache   
     */
    private final Map /* of String to RequestCache */ REQUESTS =
        new FixedsizeForgetfulHashMap(250);
                
	/**
	 * Accepts a new upload, creating a new <tt>HTTPUploader</tt>
	 * if it successfully parses the HTTP request.  BLOCKING.
	 *
	 * @param method the initial request type to use, e.g., GET or HEAD
	 * @param socket the <tt>Socket</tt> that will be used for the new upload.
     *  It is assumed that the initial word of the request (e.g., "GET") has
     *  been consumed (e.g., by Acceptor)
     * @param forceAllow forces the UploadManager to allow all requests
     *  on this socket to take place.
	 */
    public void acceptUpload(final HTTPRequestMethod method,
                             Socket socket, boolean forceAllow) {
        
        LOG.trace("accepting upload");
        HTTPUploader uploader = null;
        long startTime = -1;
		try {
            int queued = -1;
            String oldFileName = "";
            HTTPRequestMethod currentMethod=method;
            StalledUploadWatchdog watchdog = new StalledUploadWatchdog();
            InputStream iStream = null;
            boolean startedNewFile = false;
            //do uploads
            while(true) {
                if( uploader != null )
                    assertAsComplete( uploader.getState() );
                
                if(iStream == null)
                    iStream = new BufferedInputStream(socket.getInputStream());
                
                LOG.trace("parsing http line.");
                HttpRequestLine line = parseHttpRequest(socket, iStream);
                if (LOG.isTraceEnabled())
                    LOG.trace("line = " + line);
                
                if(LOG.isDebugEnabled())
                    LOG.debug(uploader + " successfully parsed request");
                
                String fileName = line._fileName;
                
                // Determine if this is a new file ...
                if( uploader == null                // no previous uploader
                 || currentMethod != uploader.getMethod()  // method change
                 || !oldFileName.equalsIgnoreCase(fileName) ) { // new file
                    startedNewFile = true;
                } else {
                    startedNewFile = false;
                }
                
                // If we're starting a new uploader, clean the old one up
                // and then create a new one.
                if(startedNewFile) {
                    if(LOG.isDebugEnabled())
                        LOG.debug(uploader + " starting new file "+line._fileName+" index "+line._index);
                    if (uploader != null) {
                        // Because queueing is per-socket (and not per file),
                        // we do not want to reset the queue status if they're
                        // requesting a new file.
                        if(queued != QUEUED)
                            queued = -1;
                        // However, we DO want to make sure that the old file
                        // is interpreted as interrupted.  Otherwise,
                        // the GUI would show two lines with the the same slot
                        // until the newer line finished, at which point
                        // the first one would display as a -1 queue position.
                        else
                            uploader.setState(Uploader.INTERRUPTED);

                        cleanupFinishedUploader(uploader, startTime);
                    }
                    uploader = new HTTPUploader(currentMethod,
                                                fileName, 
						    			        socket,
							    		        line._index,
							    		        line.getParameters(),
								    	        watchdog,
                                                line.hadPassword());
                }
                // Otherwise (we're continuing an uploader),
                // reinitialize the existing HTTPUploader.
                else {
                    if(LOG.isDebugEnabled())
                        LOG.debug(uploader + " continuing old file");
                    uploader.reinitialize(currentMethod, line.getParameters());
                }
                
                assertAsConnecting( uploader.getState() );
        
                setInitialUploadingState(uploader);
                try {
                    uploader.readHeader(iStream);
                    setUploaderStateOffHeaders(uploader);
                } catch(ProblemReadingHeaderException prhe) {
                    // if there was a problem reading the header,
                    // this is a bad request, so let them know.
                    // we do NOT throw the IOX again because the
                    // connection is still open.
                    uploader.setState(Uploader.MALFORMED_REQUEST);
                }catch (FreeloaderUploadingException fue){
                    // browser request
				     uploader.setState(Uploader.FREELOADER);
				}
                
                if(LOG.isDebugEnabled())
                    LOG.debug(uploader+" HTTPUploader created and read all headers");

                // If we have not accepted this file already, then
                // find out whether or not we should.
                if( queued != ACCEPTED ) {                	
                    queued = processNewRequest(uploader, socket, forceAllow);
                    
                    // If we just accepted this request,
                    // set the start time appropriately.
                    if( queued == ACCEPTED )
                        startTime = System.currentTimeMillis();     
                    
                }
                
                // If we started a new file with this request, attempt
                // to display it in the GUI.
                if( startedNewFile ) {
                    addToGUI(uploader);
                }

                // Do the actual upload.
                doSingleUpload(uploader);
                
                assertAsFinished( uploader.getState() );
                
                
                oldFileName = fileName;
                
                //if this is not HTTP11, then exit, as no more requests will
                //come.
                if ( !line.isHTTP11() )
                    return;

                //read the first word of the next request and proceed only if
                //"GET" or "HEAD" request.  Versions of LimeWire before 2.7
                //forgot to switch the request method.
                if(LOG.isDebugEnabled())
                    LOG.debug(uploader+" waiting for next request with socket ");
                int oldTimeout = socket.getSoTimeout();
                if(queued!=QUEUED)
                    socket.setSoTimeout(SharingSettings.PERSISTENT_HTTP_CONNECTION_TIMEOUT.getValue());
                    
                //dont read a word of size more than 4 
                //as we will handle only the next "HEAD" or "GET" request
                String word = IOUtils.readWord(
                    iStream, 4);
                if(LOG.isDebugEnabled())
                    LOG.debug(uploader+" next request arrived ");
                socket.setSoTimeout(oldTimeout);
                if (word.equals("GET")) {
                    currentMethod=HTTPRequestMethod.GET;
                    UploadStat.SUBSEQUENT_GET.incrementStat();
                } else if (word.equals("HEAD")) {
                    currentMethod=HTTPRequestMethod.HEAD;
                    UploadStat.SUBSEQUENT_HEAD.incrementStat();
                } else {
                    //Unknown request type
                    UploadStat.SUBSEQUENT_UNKNOWN.incrementStat();
                    return;
                }
            }//end of while
        } catch(IOException ioe) {//including InterruptedIOException
            if(LOG.isDebugEnabled())
                LOG.debug(uploader + " IOE thrown, closing socket", ioe);
        } finally {
            // The states SHOULD be INTERRUPTED or COMPLETED
            // here.  However, it is possible that an IOException
            // or other uncaught exception (that will be handled
            // outside of this method) were thrown at random points.
            // It is not a good idea to throw any exceptions here
            // because the triggering exception will be lost,
            // so we just set the state to INTERRUPTED if it was not
            // already complete.
            // It is possible to prove that the state is either
            // interrupted or complete in the case of normal
            // program flow.
            if( uploader != null ) {
            	if( uploader.getState() != Uploader.COMPLETE )
                	uploader.setState(Uploader.INTERRUPTED);
            }
            
            synchronized(this) {
                // If this uploader is still in the queue, remove it.
                // Also change its state from COMPLETE to INTERRUPTED
                // because it didn't really complete.
                boolean found = false;
                for(Iterator iter=_queuedUploads.iterator();iter.hasNext();){
                    KeyValue kv = (KeyValue)iter.next();
                    if(kv.getKey()==socket) {
                        iter.remove();
                        found = true;
                        break;
                    }
                }
                if(found)
                    uploader.setState(Uploader.INTERRUPTED);
            }
            
            // Always clean up the finished uploader
            // from the active list & report the upload speed
            if( uploader != null ) {
                uploader.stop();
                cleanupFinishedUploader(uploader, startTime);
            }
            
            if(LOG.isDebugEnabled())
                LOG.debug(uploader + " closing socket");
            //close the socket
            close(socket);
        }
    }
    
    /**
     * Determines whether or no this Uploader should be shown
     * in the GUI.
     */
    private boolean shouldShowInGUI(HTTPUploader uploader) {
        return uploader.getIndex() != BROWSE_HOST_FILE_INDEX &&
               uploader.getIndex() != PUSH_PROXY_FILE_INDEX &&
               uploader.getIndex() != UPDATE_FILE_INDEX &&
               uploader.getIndex() != MALFORMED_REQUEST_INDEX &&
               uploader.getIndex() != BAD_URN_QUERY_INDEX &&
               uploader.getIndex() != FILE_VIEW_FILE_INDEX &&
               uploader.getIndex() != RESOURCE_INDEX &&
               uploader.getIndex() != BROWSER_CONTROL_INDEX &&
               uploader.getMethod() != HTTPRequestMethod.HEAD &&
               !uploader.isForcedShare();
	}
    
    /**
     * Determines whether or not this Uploader should bypass queueing,
     * (meaning that it will always work immediately, and will not use
     *  up slots for other uploaders).
     *
     * All requests that are not the 'connecting' state should bypass
     * the queue, because they have already been queued once.
     *
     * Don't let FILE_VIEW requests bypass the queue, we want to make sure
     * those guys don't hammer.
     */
    private boolean shouldBypassQueue(HTTPUploader uploader) {
        return uploader.getState() != Uploader.CONNECTING ||
               uploader.getMethod() == HTTPRequestMethod.HEAD ||
               uploader.isForcedShare();
    }
    
    /**
     * Cleans up a finished uploader.
     * This does the following:
     * 1) Reports the speed at which this upload occured.
     * 2) Removes the uploader from the active upload list
     * 3) Closes the file streams that the uploader has left open
     * 4) Increments the completed uploads in the FileDesc
     * 5) Removes the uploader from the GUI.
     * (4 & 5 are only done if 'shouldShowInGUI' is true)
     */
    private void cleanupFinishedUploader(HTTPUploader uploader, long startTime) {
        if(LOG.isTraceEnabled())
            LOG.trace(uploader + " cleaning up finished.");
        
        int state = uploader.getState();
        int lastState = uploader.getLastTransferState();        
        assertAsFinished(state);
                     
        long finishTime = System.currentTimeMillis();
        synchronized(this) {
            //Report how quickly we uploaded the data.
            if(startTime > 0) {
                reportUploadSpeed( finishTime-startTime,
                                   uploader.getTotalAmountUploaded());
            }
            removeFromList(uploader);
        }
        
        uploader.closeFileStreams();
        
        switch(state) {
            case Uploader.COMPLETE:
                UploadStat.COMPLETED.incrementStat();
                if( lastState == Uploader.UPLOADING ||
                    lastState == Uploader.THEX_REQUEST)
                    UploadStat.COMPLETED_FILE.incrementStat();
                break;
            case Uploader.INTERRUPTED:
                UploadStat.INTERRUPTED.incrementStat();
                break;
        }
        
        if ( shouldShowInGUI(uploader) ) {
            FileDesc fd = uploader.getFileDesc();
            if( fd != null && 
              state == Uploader.COMPLETE &&
              (lastState == Uploader.UPLOADING ||
               lastState == Uploader.THEX_REQUEST)) {
                fd.incrementCompletedUploads();
                RouterService.getCallback().handleSharedFileUpdate(
                    fd.getFile());
    		}
            RouterService.getCallback().removeUpload(uploader);
        }
    }
    
    /**
     * Initializes the uploader's state.
     * If the file is valid for uploading, this leaves the state
     * as connecting.
     */
    private void setInitialUploadingState(HTTPUploader uploader) {
        switch(uploader.getIndex()) {
        case BROWSE_HOST_FILE_INDEX:
            uploader.setState(Uploader.BROWSE_HOST);
            return;
        case BROWSER_CONTROL_INDEX:
            uploader.setState(Uploader.BROWSER_CONTROL);
            return;
        case PUSH_PROXY_FILE_INDEX:
            uploader.setState(Uploader.PUSH_PROXY);
            return;
        case UPDATE_FILE_INDEX:
            uploader.setState(Uploader.UPDATE_FILE);
            return;
        case BAD_URN_QUERY_INDEX:
            uploader.setState(Uploader.FILE_NOT_FOUND);
            return;
        case MALFORMED_REQUEST_INDEX:
            uploader.setState(Uploader.MALFORMED_REQUEST);
            return;
        default:
        
            // This is the normal case ...
            FileManager fm = RouterService.getFileManager();
            FileDesc fd = null;
            int index = uploader.getIndex();
            // First verify the file index
            synchronized(fm) {
                if(fm.isValidIndex(index)) {
                    fd = fm.get(index);
                } 
            }

            // If the index was invalid or the file was unshared, FNF.
            if(fd == null) {
                if(LOG.isDebugEnabled())
                    LOG.debug(uploader + " fd is null");
                uploader.setState(Uploader.FILE_NOT_FOUND);
                return;
            }
            // If the name they want isn't the name we have, FNF.
            if(!uploader.getFileName().equals(fd.getFileName())) {
                if(LOG.isDebugEnabled())
                    LOG.debug(uploader + " wrong file name");
                uploader.setState(Uploader.FILE_NOT_FOUND);
                return;
            }
            
            try {
                uploader.setFileDesc(fd);
            } catch(IOException ioe) {
                if(LOG.isDebugEnabled())
                    LOG.debug(uploader + " could not create file stream "+ioe);
                uploader.setState(Uploader.FILE_NOT_FOUND);
                return;
            }

            assertAsConnecting( uploader.getState() );
        }
    }
    
    /**
     * Sets the uploader's state based off values read in the headers.
     */
    private void setUploaderStateOffHeaders(HTTPUploader uploader) {
        FileDesc fd = uploader.getFileDesc();
        
        // If it's still trying to connect, do more checks ...
        if( uploader.getState() == Uploader.CONNECTING ) {    
            // If it's the wrong URN, File Not Found it.
            URN urn = uploader.getRequestedURN();
    		if(fd != null && urn != null && !fd.containsUrn(urn)) {
    		    if(LOG.isDebugEnabled())
    		        LOG.debug(uploader + " wrong content urn");
                uploader.setState(Uploader.FILE_NOT_FOUND);
                return;
            }
    		
            //handling THEX Requests
            if (uploader.isTHEXRequest()) {
                if (uploader.getFileDesc().getHashTree() != null)
                    uploader.setState(Uploader.THEX_REQUEST);
                else
                    uploader.setState(Uploader.FILE_NOT_FOUND);
                return;
           }            
            
            // Special handling for incomplete files...
            if (fd instanceof IncompleteFileDesc) {                
                // Check to see if we're allowing PFSP.
                if( !UploadSettings.ALLOW_PARTIAL_SHARING.getValue() ) {
                    uploader.setState(Uploader.FILE_NOT_FOUND);
                    return;
                }
                
                // cannot service THEXRequests for partial files
                if (uploader.isTHEXRequest()) {
                	uploader.setState(Uploader.FILE_NOT_FOUND);
                	return;
                }
                                
                // If we are allowing, see if we have the range.
                IncompleteFileDesc ifd = (IncompleteFileDesc)fd;
                int upStart = uploader.getUploadBegin();
                // uploader.getUploadEnd() is exclusive!
                int upEnd = uploader.getUploadEnd() - 1;                
                // If the request contained a 'Range:' header, then we can
                // shrink the request to what we have available.
                if(uploader.containedRangeRequest()) {
                    Interval request = ifd.getAvailableSubRange(upStart, upEnd);
                    if ( request == null ) {
                        uploader.setState(Uploader.UNAVAILABLE_RANGE);
                        return;
                    }
                    uploader.setUploadBeginAndEnd(request.low, request.high + 1);
                } else {
                    if ( !ifd.isRangeSatisfiable(upStart, upEnd) ) {
                        uploader.setState(Uploader.UNAVAILABLE_RANGE);
                        return;
                    }
                }
            }
        }
    }
        
    /**
     * Maintains the internal state within UploadManager for this Upload.
     * This does the following:
     * 1) If 'shouldBypassQueue' & forceAllow are false, calls checkAndQueue
     *    in order to determine whether or not this uploader should
     *    be given a slot.
     *    If forceAllow is true, queued is set to ACCEPTED.
     * 2) If it is determined that the uploader is queued, the
     *    soTimeout on the socket is set to be MAX_POLL_TIME and the
     *    state is changed to QUEUED.
     *    If it is determined that the uploader is accepted, the uploader
     *    is added to the _activeUploadList.
     */
    private int processNewRequest(HTTPUploader uploader, 
                                  Socket socket,
                                  boolean forceAllow) throws IOException {
        if(LOG.isTraceEnabled())
            LOG.trace(uploader + " processing new request.");
        
        int queued = -1;
        
        // If this uploader should not bypass the queue, determine it's
        // slot.
        if( !shouldBypassQueue(uploader) ) {
            // If we are forcing this upload, intercept the queue check.
            if( forceAllow )
                queued = ACCEPTED;
            // Otherwise, determine whether or not to queue, accept
            // or reject the uploader.
            else
                // note that checkAndQueue can throw an IOException
                queued = checkAndQueue(uploader, socket);
        } else {
            queued = BYPASS_QUEUE;
        }
        
        // Act upon the queued state.
        switch(queued) {
            case REJECTED:
                uploader.setState(Uploader.LIMIT_REACHED);
                break;
            case BANNED:
            	uploader.setState(Uploader.BANNED_GREEDY);
            	break;
            case QUEUED:
                uploader.setState(Uploader.QUEUED);
                socket.setSoTimeout(MAX_POLL_TIME);
                break;
            case ACCEPTED:
                assertAsConnecting( uploader.getState() );
                synchronized (this) {
                    if (uploader.isForcedShare())
                        _forcedUploads++;
                    _activeUploadList.add(uploader);
                }
                break;
            case BYPASS_QUEUE:
                // ignore.
                break;
            default:
                Assert.that(false, "Invalid queued state: " + queued);
        }
        
        return queued;
        }

    /**
     * Adds this upload to the GUI and increments the attempted uploads.
     * Does nothing if 'shouldShowInGUI' is false.
     */
    private void addToGUI(HTTPUploader uploader) {
        
        // We want to increment attempted only for uploads that may
        // have a chance of failing.
        UploadStat.ATTEMPTED.incrementStat();
        
        //We are going to notify the gui about the new upload, and let
        //it decide what to do with it - will act depending on it's
        //state
        if (shouldShowInGUI(uploader)) {
            RouterService.getCallback().addUpload(uploader);
            FileDesc fd = uploader.getFileDesc();
			if(fd != null) {
    			fd.incrementAttemptedUploads();
    			RouterService.getCallback().handleSharedFileUpdate(
    			    fd.getFile());
			}
        }
    }

    /**
     * Does the actual upload.
     */
    private void doSingleUpload(HTTPUploader uploader) throws IOException {
        
        switch(uploader.getState()) {
            case Uploader.UNAVAILABLE_RANGE:
                UploadStat.UNAVAILABLE_RANGE.incrementStat();
                break;
            case Uploader.FILE_NOT_FOUND:
                UploadStat.FILE_NOT_FOUND.incrementStat();
                break;
            case Uploader.FREELOADER:
                UploadStat.FREELOADER.incrementStat();
                break;
            case Uploader.LIMIT_REACHED:
                UploadStat.LIMIT_REACHED.incrementStat();
                break;
            case Uploader.QUEUED:
                UploadStat.QUEUED.incrementStat();
                break;
			case Uploader.BANNED_GREEDY:
				UploadStat.BANNED.incrementStat();
                break;
            case Uploader.CONNECTING:
                uploader.setState(Uploader.UPLOADING);
                UploadStat.UPLOADING.incrementStat();
                break;
            case Uploader.THEX_REQUEST:
                UploadStat.THEX.incrementStat();
                break;
            case Uploader.COMPLETE:
            case Uploader.INTERRUPTED:
                Assert.that(false, "invalid state in doSingleUpload");
                break;
        }
        
        if(LOG.isTraceEnabled())
            LOG.trace(uploader + " doing single upload");
        
        boolean closeConnection = false;
        
        try {
            uploader.initializeStreams();
            uploader.writeResponse();
            // get the value before we change state to complete.
            closeConnection = uploader.getCloseConnection();
            uploader.setState(Uploader.COMPLETE);
        } finally {
            uploader.closeFileStreams();
        }
        
        // If the state wanted us to close the connection, throw an IOX.
        if(closeConnection)
            throw new IOException("close connection");
    }

    /**
     * closes the passed socket and its corresponding I/O streams
     */
    public void close(Socket socket) {
        //close the output streams, input streams and the socket
        try {
            if (socket != null)
                socket.getOutputStream().close();
        } catch (Exception e) {}
        try {
            if (socket != null)
                socket.getInputStream().close();
        } catch (Exception e) {}
        try {
            if (socket != null) 
                socket.close();
        } catch (Exception e) {}
    }
    
    /**
     * Returns whether or not an upload request can be serviced immediately.
     * In particular, if there are more available upload slots than queued
     * uploads this will return true. 
     */
    public synchronized boolean isServiceable() {
    	return hasFreeSlot(uploadsInProgress() + getNumQueuedUploads());
    }

	public synchronized int uploadsInProgress() {
		return _activeUploadList.size() - _forcedUploads;
	}

	public synchronized int getNumQueuedUploads() {
        return _queuedUploads.size();
    }

	/**
	 * Returns true if this has ever successfully uploaded a file
     * during this session.<p>
     * 
     * This method was added to adopt more of the BearShare QHD
	 * standard.
	 */
	public boolean hadSuccesfulUpload() {
		return _hadSuccesfulUpload;
	}
	
	public synchronized boolean isConnectedTo(InetAddress addr) {
	    for(Iterator i = _queuedUploads.iterator(); i.hasNext(); ) {
	        KeyValue next = (KeyValue)i.next();
	        Socket socket = (Socket)next.getKey();
	        if(socket != null && socket.getInetAddress().equals(addr))
	            return true;
	    }
	    for(Iterator i = _activeUploadList.iterator(); i.hasNext(); ) {
	        HTTPUploader next = (HTTPUploader)i.next();
	        InetAddress host = next.getConnectedHost();
	        if(host != null && host.equals(addr))
	            return true;
	    }
	    return false;
    }
	
	/**
	 * Kills all uploads that are uploading the given FileDesc.
	 */
	public synchronized boolean killUploadsForFileDesc(FileDesc fd) {
	    boolean ret = false;
	    // This causes the uploader to generate an exception,
	    // and ultimately remove itself from the list.
	    for(Iterator i = _activeUploadList.iterator(); i.hasNext();) {
	        HTTPUploader uploader = (HTTPUploader)i.next();
	        FileDesc upFD = uploader.getFileDesc();
	        if( upFD != null && upFD.equals(fd) ) {
	            ret = true;
	            uploader.stop();
            }
	    }
	    
	    return ret;
    }


	/////////////////// Private Interface for Testing Limits /////////////////

    /** Checks whether the given upload may proceed based on number of slots,
     *  position in upload queue, etc.  Updates the upload queue as necessary.
     *  Always accepts Browse Host requests, though.  Notifies callback of this.
     *  
     * @return ACCEPTED if the download may proceed, QUEUED if this is in the
     *  upload queue, REJECTED if this is flat-out disallowed (and hence not
     *  queued) and BANNED if the downloader is hammering us, and BYPASS_QUEUE
     *  if this is a File-View request that isn't hammering us. If REJECTED, 
     *  <tt>uploader</tt>'s state will be set to LIMIT_REACHED. If BANNED,
     *  the <tt>Uploader</tt>'s state will be set to BANNED_GREEDY.
     * @exception IOException the request came sooner than allowed by upload
     *  queueing rules.  (Throwing IOException forces the connection to be
     *  closed by the calling code.)  */
	private synchronized int checkAndQueue(Uploader uploader,
	                                       Socket socket) throws IOException {
	    RequestCache rqc = (RequestCache)REQUESTS.get(uploader.getHost());
	    if (rqc == null)
	    	rqc = new RequestCache();
	    // make sure we don't forget this RequestCache too soon!
		REQUESTS.put(uploader.getHost(), rqc);

        rqc.countRequest();
        if (rqc.isHammering()) {
            if(LOG.isWarnEnabled())
                LOG.warn(uploader + " banned.");
        	return BANNED;
        }
        

        boolean isGreedy = rqc.isGreedy(uploader.getFileDesc().getSHA1Urn());
        int size = _queuedUploads.size();
        int posInQueue = positionInQueue(socket);//-1 if not in queue
        int maxQueueSize = UploadSettings.UPLOAD_QUEUE_SIZE.getValue();
        boolean wontAccept = size >= maxQueueSize || 
			rqc.isDupe(uploader.getFileDesc().getSHA1Urn());
        int ret = -1;

        // if this uploader is greedy and at least on other client is queued
        // send him another limit reached reply.
        boolean limitReached = false;
        if (isGreedy && size >=1) {
            if(LOG.isWarnEnabled())
                LOG.warn(uploader + " greedy -- limit reached."); 
        	UploadStat.LIMIT_REACHED_GREEDY.incrementStat(); 
        	limitReached = true;
        } else if (posInQueue < 0) {
            limitReached = hostLimitReached(uploader.getHost());
            // remember that we sent a LIMIT_REACHED only
            // if the limit was actually really reached and not 
            // if we just keep a greedy client from entering the
            // QUEUE
            if(limitReached)
                rqc.limitReached(uploader.getFileDesc().getSHA1Urn());
        }
        //Note: The current policy is to not put uploadrers in a queue, if they 
        //do not send am X-Queue header. Further. uploaders are removed from 
        //the queue if they do not send the header in the subsequent request.
        //To change this policy, chnage the way queue is set.
        boolean queue = uploader.supportsQueueing();

        Assert.that(maxQueueSize>0,"queue size 0, cannot use");
        Assert.that(uploader.getState()==Uploader.CONNECTING,
                    "Bad state: "+uploader.getState());
        Assert.that(uploader.getMethod()==HTTPRequestMethod.GET);

        if(posInQueue == -1) {//this uploader is not in the queue already
            if(LOG.isDebugEnabled())
                LOG.debug(uploader+"Uploader not in que(capacity:"+maxQueueSize+")");
            if(limitReached || wontAccept) { 
                if(LOG.isDebugEnabled())
                    LOG.debug(uploader+" limited? "+limitReached+" wontAccept? "
                      +wontAccept);
                return REJECTED; //we rejected this uploader
            }
            addToQueue(socket);
            posInQueue = size;//the index of the uploader in the queue
            ret = QUEUED;//we have queued it now
            if(LOG.isDebugEnabled())
                LOG.debug(uploader+" new uploader added to queue");
        }
        else {//we are alreacy in queue, update it
            KeyValue kv = (KeyValue)_queuedUploads.get(posInQueue);
            Long prev=(Long)kv.getValue();
            if(prev.longValue()+MIN_POLL_TIME > System.currentTimeMillis()) {
                _queuedUploads.remove(posInQueue);
                if(LOG.isDebugEnabled())
                    LOG.debug(uploader+" queued uploader flooding-throwing exception");
                throw new IOException();
            }
            
            //check if this is a duplicate request
            if (rqc.isDupe(uploader.getFileDesc().getSHA1Urn()))
            	return REJECTED;
            
            kv.setValue(new Long(System.currentTimeMillis()));
            if(LOG.isDebugEnabled())
                LOG.debug(uploader+" updated queued uploader");
            ret = QUEUED;//queued
        }
        if(LOG.isDebugEnabled())
            LOG.debug(uploader+" checking if given uploader is can be accomodated ");
        // If we have atleast one slot available, see if the position
        // in the queue is small enough to be accepted.
        if(hasFreeSlot(posInQueue + uploadsInProgress())) {
            ret = ACCEPTED;
            if(LOG.isDebugEnabled())
                LOG.debug(uploader+" accepting upload");
            //remove this uploader from queue
            _queuedUploads.remove(posInQueue);
        }
        else {
            //... no slot available for this uploader
            //If uploader does not support queueing,
            //it should be removed from the queue.
            if(!queue) {//downloader does not support queueing
                _queuedUploads.remove(posInQueue);//remove it
                ret = REJECTED;
            }
        }
        
        //register the uploader in the dupe table
        if (ret == ACCEPTED)
        	rqc.startedUpload(uploader.getFileDesc().getSHA1Urn());
        return ret;
    }

    private synchronized void addToQueue(Socket socket) {
        Long t = new Long(System.currentTimeMillis());
        _queuedUploads.add(new KeyValue(socket,t));
    }

    /**
     * @return the index of the uploader in the queue, -1 if not in queue
     */
    public synchronized int positionInQueue(Socket socket) {
        int i = 0;
        Iterator iter = _queuedUploads.iterator();
        while(iter.hasNext()) {
            Object curr = ((KeyValue)iter.next()).getKey();
            if(curr==socket)
                return i;
            i++;
        }
        return -1;
    }

	/**
	 * Decrements the number of active uploads for the host specified in
	 * the <tt>host</tt> argument, removing that host from the <tt>Map</tt>
	 * if this was the only upload allocated to that host.<p>
	 *
	 * This method also removes the <tt>Uploader</tt> from the <tt>List</tt>
	 * of active uploads.
	 */
  	private synchronized void removeFromList(Uploader uploader) {
  		//if the uploader is not in the active list, we should not
  		//try remove the urn from the map of unique uploaded files for that host.
  		
		if (_activeUploadList.remove(uploader)) {
		    if (((HTTPUploader)uploader).isForcedShare())
                _forcedUploads--;
            
			//at this point it is safe to allow other uploads from the same host
			RequestCache rcq = (RequestCache) REQUESTS.get(uploader.getHost());

			//check for nulls so that unit tests pass
        	if (rcq!=null && uploader!=null && uploader.getFileDesc()!=null) 
        		rcq.uploadDone(uploader.getFileDesc().getSHA1Urn());
		}
		
		// Enable auto shutdown
		if( _activeUploadList.size()== 0)
			RouterService.getCallback().uploadsComplete();
  	}
	
    /**
     * @return true if the number of uploads from the host is strictly LESS than
     * the MAX, although we want to allow exactly MAX uploads from the same
     * host. This is because this method is called BEFORE we add/allow the.
     * upload.
     */
	private synchronized boolean hostLimitReached(String host) {
        int max = UploadSettings.UPLOADS_PER_PERSON.getValue();
        int i=0;
        Iterator iter = _activeUploadList.iterator();
        while(iter.hasNext()) { //count active uploads to this host
            Uploader u = (Uploader)iter.next();
            if(u.getHost().equals(host))
                i++;
        }
        iter = _queuedUploads.iterator();
        while(iter.hasNext()) { //also count uploads in queue to this host
            Socket s = (Socket)((KeyValue)iter.next()).getKey();
            if(s.getInetAddress().getHostAddress().equals(host))
                i++;
        }
        return i>=max;
	}
	
	/**
	 * Returns true iff another upload is allowed assuming that the
	 * amount of active uploaders is passed off to it.
	 * REQUIRES: this' monitor is held
	 */
	private boolean hasFreeSlot(int current) {
        //Allow another upload if (a) we currently have fewer than
        //SOFT_MAX_UPLOADS uploads or (b) some upload has more than
        //MINIMUM_UPLOAD_SPEED KB/s.  But never allow more than MAX_UPLOADS.
        //
        //In other words, we continue to allow uploads until everyone's
        //bandwidth is diluted.  The assumption is that with MAX_UPLOADS
        //uploads, the probability that all just happen to have low capacity
        //(e.g., modems) is small.  This reduces "Try Again Later"'s at the
        //expensive of quality, making swarmed downloads work better.
        
		if (current >= UploadSettings.HARD_MAX_UPLOADS.getValue()) {
            return false;
        } else if (current < UploadSettings.SOFT_MAX_UPLOADS.getValue()) {
            return true;
        } else {
            float fastest=0.0f;
            for (Iterator iter=_activeUploadList.iterator(); iter.hasNext(); ) {
                BandwidthTracker upload=(BandwidthTracker)iter.next();
                float speed = 0;
                try {
                    speed=upload.getMeasuredBandwidth();
                } catch (InsufficientDataException ide) {
                    speed = 0;
                }
                fastest=Math.max(fastest,speed);
            }
            return fastest>MINIMUM_UPLOAD_SPEED;
        }
    }


	////////////////// Bandwith Allocation and Measurement///////////////

	/**
	 * calculates the appropriate burst size for the allocating
	 * bandwith on the upload.
	 * @return burstSize.  if it is the special case, in which 
	 *         we want to upload as quickly as possible.
	 */
	public int calculateBandwidth() {
		// public int calculateBurstSize() {
		float totalBandwith = getTotalBandwith();
		float burstSize = totalBandwith/uploadsInProgress();
		return (int)burstSize;
	}
	
	/**
	 * @return the total bandwith available for uploads
	 */
	private float getTotalBandwith() {

		// To calculate the total bandwith available for
		// uploads, there are two properties.  The first
		// is what the user *thinks* their connection
		// speed is.  Note, that they may have set this
		// wrong, but we have no way to tell.
		float connectionSpeed = 
            ConnectionSettings.CONNECTION_SPEED.getValue()/8.0f;
		// the second number is the speed that they have 
		// allocated to uploads.  This is really a percentage
		// that the user is willing to allocate.
		float speed = UploadSettings.UPLOAD_SPEED.getValue();
		// the total bandwith available then, is the percentage
		// allocated of the total bandwith.
		float totalBandwith = connectionSpeed*speed/100.0f;
		return totalBandwith;
	}

    /** Returns the estimated upload speed in <b>KILOBITS/s</b> [sic] of the
     *  next transfer, assuming the client (i.e., downloader) has infinite
     *  bandwidth.  Returns -1 if not enough data is available for an 
     *  accurate estimate. */
    public int measuredUploadSpeed() {
        //Note that no lock is needed.
        return highestSpeed;
    }

    /**
     * Notes that some uploader has uploaded the given number of BYTES in the
     * given number of milliseconds.  If bytes is too small, the data may be
     * ignored.  
     *     @requires this' lock held 
     *     @modifies this.speed, this.speeds
     */
    private void reportUploadSpeed(long milliseconds, long bytes) {
        //This is critical for ignoring 404's messages, etc.
        if (bytes<MIN_SAMPLE_BYTES)
            return;

        //Calculate the bandwidth in kiloBITS/s.  We just assume that 1 kilobyte
        //is 1000 (not 1024) bytes for simplicity.
        int bandwidth=8*(int)((float)bytes/(float)milliseconds);
        speeds.add(new Integer(bandwidth));

        //Update maximum speed if possible.  This should be atomic.  TODO: can
        //the compiler replace the temporary variable max with highestSpeed?
        if (speeds.size()>=MIN_SPEED_SAMPLE_SIZE) {
            int max=0;
            for (int i=0; i<speeds.size(); i++) 
                max=Math.max(max, ((Integer)speeds.get(i)).intValue());
            this.highestSpeed=max;
        }
    }

	/**
	 * Returns a new <tt>HttpRequestLine</tt> instance, where the 
     * <tt>HttpRequestLine</tt>
	 * class is an immutable struct that contains all data for the "GET" line
	 * of the HTTP request.
	 *
	 * @param socket the <tt>Socket</tt> instance over which we're reading
	 * @return the <tt>HttpRequestLine</tt> struct for the HTTP request
	 */
	private HttpRequestLine parseHttpRequest(Socket socket, 
	                                         InputStream iStream)
      throws IOException {

		// Set the timeout so that we don't do block reading.
        socket.setSoTimeout(Constants.TIMEOUT);
		// open the stream from the socket for reading
		ByteReader br = new ByteReader(iStream);
		
        LOG.trace("trying to read request.");
        // read the first line. if null, throw an exception
        String str = br.readLine();
        if (LOG.isTraceEnabled()) LOG.trace("request is: " + str);

        try {

            if (str == null) {
                throw new IOException();
            }

            str.trim();

            if(this.isURNGet(str)) {
                // handle the URN get request
                return this.parseURNGet(str);
            }
		
            // handle the standard get request
            return UploadManager.parseTraditionalGet(str);
        } catch (IOException ioe) {
            LOG.debug("http request failed", ioe);
            // this means the request was malformed somehow.
            // instead of closing the connection, we tell them
            // by constructing a HttpRequestLine with a fake
            // index.  it is up to HttpUploader to interpret
            // this index correctly and send the appropriate
            // info.
            UploadStat.MALFORMED_REQUEST.incrementStat();
            if( str == null ) 
                return new HttpRequestLine(MALFORMED_REQUEST_INDEX,
                    "Malformed Request", false);
            else // we _attempt_ to determine if the request is http11
                return new HttpRequestLine(MALFORMED_REQUEST_INDEX,
                    "Malformed Request", isHTTP11Request(str));
        }
  	}

	/**
	 * Returns whether or not the get request for the specified line is
	 * a URN request.
	 *
	 * @param requestLine the <tt>String</tt> to parse to check whether it's
	 *  following the URN request syntax as specified in HUGE v. 0.93
	 * @return <tt>true</tt> if the request is a valid URN request, 
	 *  <tt>false</tt> otherwise
	 */
	private boolean isURNGet(final String requestLine) {
		int slash1Index = requestLine.indexOf("/");
		int slash2Index = requestLine.indexOf("/", slash1Index+1);
		if((slash1Index==-1) || (slash2Index==-1)) {
			return false;
		}
		String idString = requestLine.substring(slash1Index+1, slash2Index);
		return idString.equalsIgnoreCase("uri-res");
	}

	/**
	 * Performs the parsing for a traditional HTTP Gnutella get request,
	 * returning a new <tt>RequestLine</tt> instance with the data for the
	 * request.
	 *
	 * @param requestLine the HTTP get request string
	 * @return a new <tt>RequestLine</tt> instance for the request
	 * @throws <tt>IOException</tt> if there is an error parsing the
	 *  request
	 */
	private static HttpRequestLine parseTraditionalGet(final String requestLine) 
		throws IOException {
		try {           
			int index = -1;
            //tokenize the string to separate out file information part
            //and the http information part
            StringTokenizer st = new StringTokenizer(requestLine);

            if(st.countTokens() < 2) {
                throw new IOException("invalid request: "+requestLine);
            }
            //file information part: /get/0/sample.txt
            String fileInfoPart = st.nextToken().trim();
			String fileName = null;
			Map parameters = null;
            boolean hadPassword = false;
			
            if(fileInfoPart.equals("/")) {
                //special case for browse host request
                index = BROWSE_HOST_FILE_INDEX;
                fileName = "Browse-Host Request";
                UploadStat.BROWSE_HOST.incrementStat();
            } else if(fileInfoPart.startsWith(BROWSER_CONTROL_STR)) {
                //special case for browser-control request
                index = BROWSER_CONTROL_INDEX;
                fileName = fileInfoPart;
            } else if(fileInfoPart.startsWith(FV_REQ_BEGIN)) {
                //special case for file view request
                index = FILE_VIEW_FILE_INDEX;
                fileName = fileInfoPart;
            } else if(fileInfoPart.startsWith(RESOURCE_GET)) {
                //special case for file view gif get
                index = RESOURCE_INDEX;
                fileName = fileInfoPart.substring(RESOURCE_GET.length());
            } else if (fileInfoPart.equals("/update.xml")) {
                index = UPDATE_FILE_INDEX;
                fileName = "Update-File Request";
                UploadStat.UPDATE_FILE.incrementStat();
            } else if (fileInfoPart.startsWith("/gnutella/push-proxy") ||
                       fileInfoPart.startsWith("/gnet/push-proxy")) {
                // start after the '?'
                int question = fileInfoPart.indexOf('?');
                if( question == -1 )
                    throw new IOException("Malformed PushProxy Req");
                fileInfoPart = fileInfoPart.substring(question + 1);
                index = PUSH_PROXY_FILE_INDEX;
                // set the filename as the servent ID
                StringTokenizer stLocal = new StringTokenizer(fileInfoPart, "=&");
                // iff less than two tokens, or no value for a parameter, bad.
                if (stLocal.countTokens() < 2 || stLocal.countTokens() % 2 != 0)
                    throw new IOException("Malformed PushProxy HTTP Request");
                Integer fileIndex = null;
                while( stLocal.hasMoreTokens()  ) {
                    final String k = stLocal.nextToken();
                    final String val = stLocal.nextToken();
                    if(k.equalsIgnoreCase(PushProxyUploadState.P_SERVER_ID)) {
                        if( fileName != null ) // already have a name?
                            throw new IOException("Malformed PushProxy Req");
                        // must convert from base32 to base 16.
                        byte[] base16 = Base32.decode(val);
                        if( base16.length != 16 )
                            throw new IOException("Malformed PushProxy Req");
                        fileName = new GUID(base16).toHexString();
                    } else if(k.equalsIgnoreCase(PushProxyUploadState.P_GUID)){
                        if( fileName != null ) // already have a name?
                            throw new IOException("Malformed PushProxy Req");
                        if( val.length() != 32 )
                            throw new IOException("Malformed PushProxy Req");
                        fileName = val; //already in base16.
                    } else if(k.equalsIgnoreCase(PushProxyUploadState.P_FILE)){
                        if( fileIndex != null ) // already have an index?
                            throw new IOException("Malformed PushProxy Req");
                        fileIndex = Integer.valueOf(val);
                        if( fileIndex.intValue() < 0 )
                            throw new IOException("Malformed PushProxy Req");
                        if( parameters == null ) // create the param map
                            parameters = new HashMap();
                        parameters.put("file", fileIndex);
                     }
                }
                UploadStat.PUSH_PROXY.incrementStat();
            } else {
                //NORMAL CASE
                // parse this for the appropriate information
                // find where the get is...
                int g = requestLine.indexOf("/get/");
                // find the next "/" after the "/get/".  the number 
                // between should be the index;
                int d = requestLine.indexOf( "/", (g + 5) ); 
                // get the index
                String str_index = requestLine.substring( (g+5), d );
                index = java.lang.Integer.parseInt(str_index);
                // get the filename, which should be right after
                // the "/", and before the next " ".
                int f = requestLine.indexOf( " HTTP/", d );
				try {
					fileName = URLDecoder.decode(
					             requestLine.substring( (d+1), f));
				} catch(IllegalArgumentException e) {
					fileName = requestLine.substring( (d+1), f);
				}
                UploadStat.TRADITIONAL_GET.incrementStat();				
            }
            //check if the protocol is HTTP1.1.
            //Note that this is not a very strict check.
            boolean http11 = isHTTP11Request(requestLine);
			return new HttpRequestLine(index, fileName, http11, parameters,
                                       hadPassword);
		} catch (NumberFormatException e) {
			throw new IOException();
		} catch (IndexOutOfBoundsException e) {
			throw new IOException();
		}
	}

	/**
	 * Parses the get line for a URN request, throwing an exception if 
	 * there are any errors in parsing.
     *
     * If we do not have the URN, we request a HttpRequestLine whose index
     * is BAD_URN_QUERY_INDEX.  It is up to HTTPUploader to properly read
     * the index and set the state to FILE_NOT_FOUND.
	 *
	 * @param requestLine the <tt>String</tt> instance containing the get request
	 * @return a new <tt>RequestLine</tt> instance containing all of the data
	 *  for the get request
	 */
	private HttpRequestLine parseURNGet(final String requestLine)
      throws IOException {
		URN urn = URN.createSHA1UrnFromHttpRequest(requestLine);
		Map params = new HashMap();
		
        // Parse the service identifier, whether N2R, N2X or something
        // we cannot satisfy.  URI scheme names are not case-sensitive.
        String requestUpper = requestLine.toUpperCase(Locale.US);
        if (requestUpper.indexOf(HTTPConstants.NAME_TO_THEX) > 0)
            params.put(SERVICE_ID, HTTPConstants.NAME_TO_THEX);
        else if (requestUpper.indexOf(HTTPConstants.NAME_TO_RESOURCE) > 0)
            params.put(SERVICE_ID, HTTPConstants.NAME_TO_RESOURCE);
        else {
            if(LOG.isWarnEnabled())
			    LOG.warn("Invalid URN query: " + requestLine);
			return new HttpRequestLine(BAD_URN_QUERY_INDEX,
				"Invalid URN query", isHTTP11Request(requestLine));
        }
		
		FileDesc desc = RouterService.getFileManager().getFileDescForUrn(urn);
		if(desc == null) {
            UploadStat.UNKNOWN_URN_GET.incrementStat();
            return new HttpRequestLine(BAD_URN_QUERY_INDEX,
                  "Invalid URN query", isHTTP11Request(requestLine));
		}		
        UploadStat.URN_GET.incrementStat();
		return new HttpRequestLine(desc.getIndex(), desc.getFileName(), 
								   isHTTP11Request(requestLine), params, false);
	}

	/**
	 * Returns whether or the the specified get request is using HTTP 1.1.
	 *
	 * @return <tt>true</tt> if the get request specifies HTTP 1.1,
	 *  <tt>false</tt> otherwise
	 */
	private static boolean isHTTP11Request(final String requestLine) {
		return requestLine.endsWith("1.1");
	}
	
	/**
	 * Asserts the state is CONNECTING.
	 */
	private void assertAsConnecting(int state) {
	    Assert.that( state == Uploader.CONNECTING,
	     "invalid state: " + state);
	}
	
	/**
	 * Asserts the state is COMPLETE.
	 */
	private void assertAsComplete(int state) {
	    Assert.that( state == Uploader.COMPLETE,
	     "invalid state: " + state);
	}
	
	/**
	 * Asserts that the state is an inactive/finished state.
	 */
	private void assertAsFinished(int state) {
	    Assert.that(state==Uploader.INTERRUPTED || state==Uploader.COMPLETE,
	     "invalid state: " + state);
	}	    
    
	/**
	 * This is an immutable class that contains the data for the GET line of
	 * the HTTP request.
	 */
	private final static class HttpRequestLine {
		
		/**
		 * The index of the request.
		 */
  		final int _index;

		/**
		 * The file name of the request.
		 */
  		final String _fileName;

        /** 
		 * Flag indicating if the protocol is HTTP1.1.
		 */
        final boolean _http11;
        
        /**
         * Flag of the params in this request line.
         * Guaranteed to be non null.
         */
        final Map _params;

        public String toString() {
            return "Index = " + _index + ", FileName = " + _fileName +
            ", is HTTP1.1? " + _http11 + ", Parameters = " + _params;
        }
        
        /**
         * Flag for whether or not the get request had the correct password.
         */
        final boolean _hadPass;

		/**
		 * Constructs a new <tt>RequestLine</tt> instance with no parameters.
		 *
		 * @param index the index for the file to get
		 * @param fileName the name of the file to get
		 * @param http11 specifies whether or not it's an HTTP 1.1 request
		 */
		HttpRequestLine(int index, String fileName, boolean http11) {
		    this(index, fileName, http11, Collections.EMPTY_MAP, false);
  		}
  		
		/**
		 * Constructs a new <tt>RequestLine</tt> instance with parameters.
		 *
		 * @param index the index for the file to get
		 * @param fName the name of the file to get
		 * @param http11 specifies whether or not it's an HTTP 1.1 request
		 * @param params a map of params in this request line
		 */
  		HttpRequestLine(int index, String fName, boolean http11, Map params,
                        boolean hadPass) {
  			_index = index;
  			_fileName = fName;
            _http11 = http11;
            if( params == null )
                _params = Collections.EMPTY_MAP;
            else
                _params = params;
            _hadPass = hadPass;
        }
        
		/**
		 * Returns whether or not the request is an HTTP 1.1 request.
		 *
		 * @return <tt>true</tt> if this is an HTTP 1.1 request, <tt>false</tt>
		 *  otherwise
		 */
        boolean isHTTP11() {
            return _http11;
        }
        
        /**
         * Returns the parameter map for this request line.
         */
        Map getParameters() {
            return _params;
        }

        /**
         * @return true if the get request had a matching password
         */
        boolean hadPassword() {
            return _hadPass;
        }
  	}

    /** Calls measureBandwidth on each uploader. */
    public void measureBandwidth() {
        List activeCopy;
        synchronized(this) {
            activeCopy = new ArrayList(_activeUploadList);
        }
        
        float currentTotal = 0f;
        boolean c = false;
        for (Iterator iter = activeCopy.iterator(); iter.hasNext(); ) {
			HTTPUploader up = (HTTPUploader)iter.next();
            if (up.isForcedShare())
                continue;
            c = true;
			up.measureBandwidth();
			currentTotal += up.getAverageBandwidth();
		}
		if ( c ) {
            synchronized(this) {
                averageBandwidth = ( (averageBandwidth * numMeasures) + currentTotal ) 
                    / ++numMeasures;
            }
        }
    }

    /** Returns the total upload throughput, i.e., the sum over all uploads. */
	public float getMeasuredBandwidth() {
        List activeCopy;
        synchronized(this) {
            activeCopy = new ArrayList(_activeUploadList);
        }
        
        float sum=0;
        for (Iterator iter = activeCopy.iterator(); iter.hasNext(); ) {
			HTTPUploader up = (HTTPUploader)iter.next();
            if (up.isForcedShare())
                continue;
            
            sum += up.getMeasuredBandwidth();
		}
        return sum;
	}
	
	/**
	 * returns the summed average of the uploads
	 */
	public synchronized float getAverageBandwidth() {
        return averageBandwidth;
	}

    static void tBandwidthTracker(UploadManager upman) {
        upman.reportUploadSpeed(100000, 1000000);  //10 kB/s
        Assert.that(upman.measuredUploadSpeed()==-1);
        upman.reportUploadSpeed(100000, 2000000);  //20 kB/s
        Assert.that(upman.measuredUploadSpeed()==-1);
        upman.reportUploadSpeed(100000, 3000000);  //30 kB/s
        Assert.that(upman.measuredUploadSpeed()==-1);
        upman.reportUploadSpeed(100000, 4000000);  //40 kB/s
        Assert.that(upman.measuredUploadSpeed()==-1);
        upman.reportUploadSpeed(100000, 5000000);  //50 kB/s == 400 kb/sec
        Assert.that(upman.measuredUploadSpeed()==400);
        upman.reportUploadSpeed(100000, 6000000);  //60 kB/s == 480 kb/sec
        Assert.that(upman.measuredUploadSpeed()==480);
        upman.reportUploadSpeed(1, 1000);          //too little data to count
        Assert.that(upman.measuredUploadSpeed()==480);
        upman.reportUploadSpeed(100000, 1000000);  //10 kB/s = 80 kb/s
        upman.reportUploadSpeed(100000, 1000000);
        upman.reportUploadSpeed(100000, 1000000);
        upman.reportUploadSpeed(100000, 1000000);
        upman.reportUploadSpeed(100000, 1000000);
        Assert.that(upman.measuredUploadSpeed()==80);
    }

	/**
	 * This class keeps track of client requests.
	 * 
	 * IMPORTANT: Always call isGreedy() method, because it counts requests,
	 * expires lists, etc.
	 */
    private static class RequestCache {
		// we don't allow more than 1 request per 5 seconds
    	private static final double MAX_REQUESTS = 5 * 1000;
    	
    	// don't keep more than this many entries
    	private static final int MAX_ENTRIES = 10;
    	
    	// time we expect the downloader to wait before sending 
    	// another request after our initial LIMIT_REACHED reply
    	// must be greater than or equal to what we send in our RetryAfter
    	// header, otherwise we'll incorrectly mark guys as greedy.
    	static long WAIT_TIME =
    	    LimitReachedUploadState.RETRY_AFTER_TIME * 1000;

		// time to wait before checking for hammering: 30 seconds.
		// if the averge number of requests per time frame exceeds MAX_REQUESTS
		// after FIRST_CHECK_TIME, the downloader will be banned.
		static long FIRST_CHECK_TIME = 30*1000;
		
		/**
		 * The set of sha1 requests we've seen in the past WAIT_TIME.
		 */
		private final Set /* of SHA1 (URN) */ REQUESTS;
		
		private final Set /* of SHA1 (URN) */ ACTIVE_UPLOADS; 
		
		/**
		 * The number of requests we've seen from this host so far.
		 */
		private double _numRequests;
		
		/**
		 * The time of the last request.
		 */
		private long _lastRequest;
		
		/**
		 * The time of the first request.
		 */
		private long _firstRequest;
 
        /**
         * Constructs a new RequestCache.
         */
     	RequestCache() {
    		REQUESTS = new FixedSizeExpiringSet(MAX_ENTRIES, WAIT_TIME);
    		ACTIVE_UPLOADS = new HashSet();
    		_numRequests = 0;
    		_lastRequest = _firstRequest = System.currentTimeMillis();
        }
        
        /**
         * Determines whether or not the host is being greedy.
         *
         * Calling this method has a side-effect of counting itself
         * as a request.
         */
    	boolean isGreedy(URN sha1) {
    		return REQUESTS.contains(sha1);
    	}
    	
    	/**
    	 * tells the cache that an upload to the host has started.
    	 * @param sha1 the urn of the file being uploaded.
    	 */
    	void startedUpload(URN sha1) {
    		ACTIVE_UPLOADS.add(sha1);
    	}
    	
    	/**
    	 * Determines whether or not the host is hammering.
    	 */
    	boolean isHammering() {
            if (_lastRequest - _firstRequest <= FIRST_CHECK_TIME) {
    			return false;
    		} else  {
    		    return ((double)(_lastRequest - _firstRequest) / _numRequests)
    		           < MAX_REQUESTS;
    		}
    	}
    	
    	/**
    	 * Informs the cache that the limit has been reached for this SHA1.
    	 */
    	void limitReached(URN sha1) {
			REQUESTS.add(sha1);
    	}
    	
    	/**
    	 * Adds a new request.
    	 */
    	void countRequest() {
    		_numRequests++;
    		_lastRequest = System.currentTimeMillis();
    	}
    	
    	/**
    	 * checks whether the given URN is a duplicate request
    	 */
    	boolean isDupe(URN sha1) {
    		return ACTIVE_UPLOADS.contains(sha1);
    	}
    	
    	/**
    	 * informs the request cache that the given URN is no longer
    	 * actively uploaded.
    	 */
    	void uploadDone(URN sha1) {
    		ACTIVE_UPLOADS.remove(sha1);
    	}
    }
}
