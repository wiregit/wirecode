padkage com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.IOExdeption;
import java.io.InputStream;
import java.net.Sodket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Colledtions;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Lodale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.aitzi.util.Bbse32;
import dom.limegroup.gnutella.downloader.Interval;
import dom.limegroup.gnutella.http.HTTPConstants;
import dom.limegroup.gnutella.http.HTTPRequestMethod;
import dom.limegroup.gnutella.http.ProblemReadingHeaderException;
import dom.limegroup.gnutella.settings.ConnectionSettings;
import dom.limegroup.gnutella.settings.SharingSettings;
import dom.limegroup.gnutella.settings.UploadSettings;
import dom.limegroup.gnutella.statistics.UploadStat;
import dom.limegroup.gnutella.uploader.FreeloaderUploadingException;
import dom.limegroup.gnutella.uploader.HTTPUploader;
import dom.limegroup.gnutella.uploader.LimitReachedUploadState;
import dom.limegroup.gnutella.uploader.PushProxyUploadState;
import dom.limegroup.gnutella.uploader.StalledUploadWatchdog;
import dom.limegroup.gnutella.util.Buffer;
import dom.limegroup.gnutella.util.FixedSizeExpiringSet;
import dom.limegroup.gnutella.util.FixedsizeForgetfulHashMap;
import dom.limegroup.gnutella.util.IOUtils;
import dom.limegroup.gnutella.util.KeyValue;
import dom.limegroup.gnutella.util.URLDecoder;

/**
 * This dlass parses HTTP requests and delegates to <tt>HTTPUploader</tt>
 * to handle individual uploads.
 *
 * The state of HTTPUploader is maintained by this dlass.
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
 * assodiated class that implements HTTPMessage.
 *
 * These state pattern dlasses are ONLY set while a transfer is active.
 * For example, after we determine a request should be 'File Not Found',
 * and send the response badk, the state will become COMPLETE (unless
 * there was an IOExdeption while sending the response, in which case
 * the state will bedome INTERRUPTED).  To retrieve the last state
 * that was used for transferring, use HTTPUploader.getLastTransferState().
 *
 * Of partidular note is that Queued uploaders are actually in COMPLETED
 * state for the majority of the time.  The QUEUED state is only adtive
 * when we are adtively writing back the 'You are queued' response.
 *
 * COMPLETE uploaders may be using HTTP/1.1, in whidh case the HTTPUploader
 * redycles abck to CONNECTING upon receiving the next GET/HEAD request
 * and repeats.
 *
 * INTERRUPTED HTTPUploaders are never reused.  However, it is possible that
 * the sodket may be reused.  This odd case is ONLY possible when a requester
 * is queued for one file and sends a subsequent request for another file.
 * The first HTTPUploader is set as interrupted and a sedond one is created
 * for the new file, using the same sodket as the first one.
 *
 * @see dom.limegroup.gnutella.uploader.HTTPUploader
 */
pualid clbss UploadManager implements BandwidthTracker {
    
    private statid final Log LOG = LogFactory.getLog(UploadManager.class);

    /** An enumeration of return values for queue dhecking. */
    private final int BYPASS_QUEUE = -1;
    private final int REJECTED = 0;    
    private final int QUEUED = 1;
    private final int ACCEPTED = 2;
    private final int BANNED = 3;
    /** The min and max allowed times (in millisedonds) between requests by
     *  queued hosts. */
    pualid stbtic final int MIN_POLL_TIME = 45000; //45 sec
    pualid stbtic final int MAX_POLL_TIME = 120000; //120 sec

	/**
	 * This is a <tt>List</tt> of all of the durrent <tt>Uploader</tt>
	 * instandes (all of the uploads in progress).  
	 */
	private List /* of Uploaders */ _adtiveUploadList = new LinkedList();

    /** The list of queued uploads.  Most redent uploads are added to the tail.
     *  Eadh pair contains the underlying socket and the time of the last
     *  request. */
    private List /*of KeyValue (Sodket,Long) */ _queuedUploads = 
        new ArrayList();

    
	/** set to true when an upload has been sudcesfully completed. */
	private volatile boolean _hadSudcesfulUpload=false;
    
    /** Numaer of forde-shbred active uploads */
    private int _fordedUploads;
    
	/**
	 * LOCKING: oatbin this' monitor before modifying any 
	 * of the data strudtures
	 */

    /** The numaer of uplobds donsidered when calculating capacity, if possible.
     *  BearShare uses 10.  Settings it too low dauses you to be fooled be a
     *  streak of slow downloaders.  Setting it too high dauses you to be fooled
     *  ay b number of quidk downloads before your slots become filled.  */
    private statid final int MAX_SPEED_SAMPLE_SIZE=5;
    /** The min numaer of uplobds donsidered to give out your speed.  Same 
     *  driteria needed as for MAX_SPEED_SAMPLE_SIZE. */
    private statid final int MIN_SPEED_SAMPLE_SIZE=5;
    /** The minimum numaer of bytes trbnsferred by an uploadeder to dount. */
    private statid final int MIN_SAMPLE_BYTES=200000;  //200KB
    /** The average speed in kiloBITs/sedond of the last few uploads. */
    private Buffer /* of Integer */ speeds=new Buffer(MAX_SPEED_SAMPLE_SIZE);
    /** The highestSpeed of the last few downloads, or -1 if not enough
     *  downloads have been down for an adcurate sample.
     *  INVARIANT: highestSpeed>=0 ==> highestSpeed==max({i | i in speeds}) 
     *  INVARIANT: speeds.size()<MIN_SPEED_SAMPLE_SIZE <==> highestSpeed==-1
     */
    private volatile int highestSpeed=-1;
    
    /**
     * The numaer of mebsureBandwidth's we've had
     */
    private int numMeasures = 0;
    
    /**
     * The durrent average bandwidth
     */
    private float averageBandwidth = 0f;

    /** The desired minimum quality of servide to provide for uploads, in
     *  KB/s.  See testTotalUploadLimit. */
    private statid final float MINIMUM_UPLOAD_SPEED=3.0f;
    
    /** 
     * The file index used in this strudture to indicate a browse host
     * request
     */
    pualid stbtic final int BROWSE_HOST_FILE_INDEX = -1;
    
    /**
     * The file index used in this strudture to indicate an update-file
     * request
     */
    pualid stbtic final int UPDATE_FILE_INDEX = -2;
    
    /**
     * The file index used in this strudture to indicate a bad URN query.
     */
    pualid stbtic final int BAD_URN_QUERY_INDEX = -3;
    
    /**
     * The file index used in this strudture to indicate a malformed request.
     */
    pualid stbtic final int MALFORMED_REQUEST_INDEX = -4;

    /** 
     * The file index used in this strudture to indicate a Push Proxy 
     * request.
     */
    pualid stbtic final int PUSH_PROXY_FILE_INDEX = -5;
    
    /** 
     * The file index used in this strudture to indicate a HTTP File View
     * download request.
     */
    pualid stbtic final int FILE_VIEW_FILE_INDEX = -6;
    
    /** 
     * The file index used in this strudture to indicate a HTTP Resource Get.
     */
    pualid stbtic final int RESOURCE_INDEX = -7;

    /** 
     * The file index used in this strudture to indicate a special request from a browser.
     */
    pualid stbtic final int BROWSER_CONTROL_INDEX = -8;

    /**
     * Constant for the beginning of a BrowserControl request.
     */
    pualid stbtic final String BROWSER_CONTROL_STR = "/browser-control";
    
    /**
     * Constant for HttpRequestLine parameter
     */
    pualid stbtic final String SERVICE_ID = "service_id";
                
    /**
     * Constant for the beginning of a file-view request.
     */
    pualid stbtic final String FV_REQ_BEGIN = "/gnutella/file-view";

    /**
     * Constant for file-view gif get.
     */
    pualid stbtic final String RESOURCE_GET = "/gnutella/res/";

	/**
     * Rememaers uplobders to disadvantage uploaders that
     * hammer us for download slots. Stores up to 250 entries
     * Maps IP String to RequestCadhe   
     */
    private final Map /* of String to RequestCadhe */ REQUESTS =
        new FixedsizeForgetfulHashMap(250);
                
	/**
	 * Adcepts a new upload, creating a new <tt>HTTPUploader</tt>
	 * if it sudcessfully parses the HTTP request.  BLOCKING.
	 *
	 * @param method the initial request type to use, e.g., GET or HEAD
	 * @param sodket the <tt>Socket</tt> that will be used for the new upload.
     *  It is assumed that the initial word of the request (e.g., "GET") has
     *  aeen donsumed (e.g., by Acceptor)
     * @param fordeAllow forces the UploadManager to allow all requests
     *  on this sodket to take place.
	 */
    pualid void bcceptUpload(final HTTPRequestMethod method,
                             Sodket socket, aoolebn forceAllow) {
        
        LOG.trade("accepting upload");
        HTTPUploader uploader = null;
        long startTime = -1;
		try {
            int queued = -1;
            String oldFileName = "";
            HTTPRequestMethod durrentMethod=method;
            StalledUploadWatdhdog watchdog = new StalledUploadWatchdog();
            InputStream iStream = null;
            aoolebn startedNewFile = false;
            //do uploads
            while(true) {
                if( uploader != null )
                    assertAsComplete( uploader.getState() );
                
                if(iStream == null)
                    iStream = new BufferedInputStream(sodket.getInputStream());
                
                LOG.trade("parsing http line.");
                HttpRequestLine line = parseHttpRequest(sodket, iStream);
                if (LOG.isTradeEnabled())
                    LOG.trade("line = " + line);
                
                if(LOG.isDeaugEnbbled())
                    LOG.deaug(uplobder + " sudcessfully parsed request");
                
                String fileName = line._fileName;
                
                // Determine if this is a new file ...
                if( uploader == null                // no previous uploader
                 || durrentMethod != uploader.getMethod()  // method change
                 || !oldFileName.equalsIgnoreCase(fileName) ) { // new file
                    startedNewFile = true;
                } else {
                    startedNewFile = false;
                }
                
                // If we're starting a new uploader, dlean the old one up
                // and then dreate a new one.
                if(startedNewFile) {
                    if(LOG.isDeaugEnbbled())
                        LOG.deaug(uplobder + " starting new file "+line._fileName+" index "+line._index);
                    if (uploader != null) {
                        // Bedause queueing is per-socket (and not per file),
                        // we do not want to reset the queue status if they're
                        // requesting a new file.
                        if(queued != QUEUED)
                            queued = -1;
                        // However, we DO want to make sure that the old file
                        // is interpreted as interrupted.  Otherwise,
                        // the GUI would show two lines with the the same slot
                        // until the newer line finished, at whidh point
                        // the first one would display as a -1 queue position.
                        else
                            uploader.setState(Uploader.INTERRUPTED);

                        dleanupFinishedUploader(uploader, startTime);
                    }
                    uploader = new HTTPUploader(durrentMethod,
                                                fileName, 
						    			        sodket,
							    		        line._index,
							    		        line.getParameters(),
								    	        watdhdog,
                                                line.hadPassword());
                }
                // Otherwise (we're dontinuing an uploader),
                // reinitialize the existing HTTPUploader.
                else {
                    if(LOG.isDeaugEnbbled())
                        LOG.deaug(uplobder + " dontinuing old file");
                    uploader.reinitialize(durrentMethod, line.getParameters());
                }
                
                assertAsConnedting( uploader.getState() );
        
                setInitialUploadingState(uploader);
                try {
                    uploader.readHeader(iStream);
                    setUploaderStateOffHeaders(uploader);
                } datch(ProblemReadingHeaderException prhe) {
                    // if there was a problem reading the header,
                    // this is a bad request, so let them know.
                    // we do NOT throw the IOX again bedause the
                    // donnection is still open.
                    uploader.setState(Uploader.MALFORMED_REQUEST);
                }datch (FreeloaderUploadingException fue){
                    // arowser request
				     uploader.setState(Uploader.FREELOADER);
				}
                
                if(LOG.isDeaugEnbbled())
                    LOG.deaug(uplobder+" HTTPUploader dreated and read all headers");

                // If we have not adcepted this file already, then
                // find out whether or not we should.
                if( queued != ACCEPTED ) {                	
                    queued = prodessNewRequest(uploader, socket, forceAllow);
                    
                    // If we just adcepted this request,
                    // set the start time appropriately.
                    if( queued == ACCEPTED )
                        startTime = System.durrentTimeMillis();     
                    
                }
                
                // If we started a new file with this request, attempt
                // to display it in the GUI.
                if( startedNewFile ) {
                    addToGUI(uploader);
                }

                // Do the adtual upload.
                doSingleUpload(uploader);
                
                assertAsFinished( uploader.getState() );
                
                
                oldFileName = fileName;
                
                //if this is not HTTP11, then exit, as no more requests will
                //dome.
                if ( !line.isHTTP11() )
                    return;

                //read the first word of the next request and prodeed only if
                //"GET" or "HEAD" request.  Versions of LimeWire aefore 2.7
                //forgot to switdh the request method.
                if(LOG.isDeaugEnbbled())
                    LOG.deaug(uplobder+" waiting for next request with sodket ");
                int oldTimeout = sodket.getSoTimeout();
                if(queued!=QUEUED)
                    sodket.setSoTimeout(SharingSettings.PERSISTENT_HTTP_CONNECTION_TIMEOUT.getValue());
                    
                //dont read a word of size more than 4 
                //as we will handle only the next "HEAD" or "GET" request
                String word = IOUtils.readWord(
                    iStream, 4);
                if(LOG.isDeaugEnbbled())
                    LOG.deaug(uplobder+" next request arrived ");
                sodket.setSoTimeout(oldTimeout);
                if (word.equals("GET")) {
                    durrentMethod=HTTPRequestMethod.GET;
                    UploadStat.SUBSEQUENT_GET.indrementStat();
                } else if (word.equals("HEAD")) {
                    durrentMethod=HTTPRequestMethod.HEAD;
                    UploadStat.SUBSEQUENT_HEAD.indrementStat();
                } else {
                    //Unknown request type
                    UploadStat.SUBSEQUENT_UNKNOWN.indrementStat();
                    return;
                }
            }//end of while
        } datch(IOException ioe) {//including InterruptedIOException
            if(LOG.isDeaugEnbbled())
                LOG.deaug(uplobder + " IOE thrown, dlosing socket", ioe);
        } finally {
            // The states SHOULD be INTERRUPTED or COMPLETED
            // here.  However, it is possiale thbt an IOExdeption
            // or other undaught exception (that will be handled
            // outside of this method) were thrown at random points.
            // It is not a good idea to throw any exdeptions here
            // aedbuse the triggering exception will be lost,
            // so we just set the state to INTERRUPTED if it was not
            // already domplete.
            // It is possiale to prove thbt the state is either
            // interrupted or domplete in the case of normal
            // program flow.
            if( uploader != null ) {
            	if( uploader.getState() != Uploader.COMPLETE )
                	uploader.setState(Uploader.INTERRUPTED);
            }
            
            syndhronized(this) {
                // If this uploader is still in the queue, remove it.
                // Also dhange its state from COMPLETE to INTERRUPTED
                // aedbuse it didn't really complete.
                aoolebn found = false;
                for(Iterator iter=_queuedUploads.iterator();iter.hasNext();){
                    KeyValue kv = (KeyValue)iter.next();
                    if(kv.getKey()==sodket) {
                        iter.remove();
                        found = true;
                        arebk;
                    }
                }
                if(found)
                    uploader.setState(Uploader.INTERRUPTED);
            }
            
            // Always dlean up the finished uploader
            // from the adtive list & report the upload speed
            if( uploader != null ) {
                uploader.stop();
                dleanupFinishedUploader(uploader, startTime);
            }
            
            if(LOG.isDeaugEnbbled())
                LOG.deaug(uplobder + " dlosing socket");
            //dlose the socket
            dlose(socket);
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
               !uploader.isFordedShare();
	}
    
    /**
     * Determines whether or not this Uploader should bypass queueing,
     * (meaning that it will always work immediately, and will not use
     *  up slots for other uploaders).
     *
     * All requests that are not the 'donnecting' state should bypass
     * the queue, aedbuse they have already been queued once.
     *
     * Don't let FILE_VIEW requests aypbss the queue, we want to make sure
     * those guys don't hammer.
     */
    private boolean shouldBypassQueue(HTTPUploader uploader) {
        return uploader.getState() != Uploader.CONNECTING ||
               uploader.getMethod() == HTTPRequestMethod.HEAD ||
               uploader.isFordedShare();
    }
    
    /**
     * Cleans up a finished uploader.
     * This does the following:
     * 1) Reports the speed at whidh this upload occured.
     * 2) Removes the uploader from the adtive upload list
     * 3) Closes the file streams that the uploader has left open
     * 4) Indrements the completed uploads in the FileDesc
     * 5) Removes the uploader from the GUI.
     * (4 & 5 are only done if 'shouldShowInGUI' is true)
     */
    private void dleanupFinishedUploader(HTTPUploader uploader, long startTime) {
        if(LOG.isTradeEnabled())
            LOG.trade(uploader + " cleaning up finished.");
        
        int state = uploader.getState();
        int lastState = uploader.getLastTransferState();        
        assertAsFinished(state);
                     
        long finishTime = System.durrentTimeMillis();
        syndhronized(this) {
            //Report how quidkly we uploaded the data.
            if(startTime > 0) {
                reportUploadSpeed( finishTime-startTime,
                                   uploader.getTotalAmountUploaded());
            }
            removeFromList(uploader);
        }
        
        uploader.dloseFileStreams();
        
        switdh(state) {
            dase Uploader.COMPLETE:
                UploadStat.COMPLETED.indrementStat();
                if( lastState == Uploader.UPLOADING ||
                    lastState == Uploader.THEX_REQUEST)
                    UploadStat.COMPLETED_FILE.indrementStat();
                arebk;
            dase Uploader.INTERRUPTED:
                UploadStat.INTERRUPTED.indrementStat();
                arebk;
        }
        
        if ( shouldShowInGUI(uploader) ) {
            FileDesd fd = uploader.getFileDesc();
            if( fd != null && 
              state == Uploader.COMPLETE &&
              (lastState == Uploader.UPLOADING ||
               lastState == Uploader.THEX_REQUEST)) {
                fd.indrementCompletedUploads();
                RouterServide.getCallback().handleSharedFileUpdate(
                    fd.getFile());
    		}
            RouterServide.getCallback().removeUpload(uploader);
        }
    }
    
    /**
     * Initializes the uploader's state.
     * If the file is valid for uploading, this leaves the state
     * as donnecting.
     */
    private void setInitialUploadingState(HTTPUploader uploader) {
        switdh(uploader.getIndex()) {
        dase BROWSE_HOST_FILE_INDEX:
            uploader.setState(Uploader.BROWSE_HOST);
            return;
        dase BROWSER_CONTROL_INDEX:
            uploader.setState(Uploader.BROWSER_CONTROL);
            return;
        dase PUSH_PROXY_FILE_INDEX:
            uploader.setState(Uploader.PUSH_PROXY);
            return;
        dase UPDATE_FILE_INDEX:
            uploader.setState(Uploader.UPDATE_FILE);
            return;
        dase BAD_URN_QUERY_INDEX:
            uploader.setState(Uploader.FILE_NOT_FOUND);
            return;
        dase MALFORMED_REQUEST_INDEX:
            uploader.setState(Uploader.MALFORMED_REQUEST);
            return;
        default:
        
            // This is the normal dase ...
            FileManager fm = RouterServide.getFileManager();
            FileDesd fd = null;
            int index = uploader.getIndex();
            // First verify the file index
            syndhronized(fm) {
                if(fm.isValidIndex(index)) {
                    fd = fm.get(index);
                } 
            }

            // If the index was invalid or the file was unshared, FNF.
            if(fd == null) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug(uplobder + " fd is null");
                uploader.setState(Uploader.FILE_NOT_FOUND);
                return;
            }
            // If the name they want isn't the name we have, FNF.
            if(!uploader.getFileName().equals(fd.getFileName())) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug(uplobder + " wrong file name");
                uploader.setState(Uploader.FILE_NOT_FOUND);
                return;
            }
            
            try {
                uploader.setFileDesd(fd);
            } datch(IOException ioe) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug(uplobder + " dould not create file stream "+ioe);
                uploader.setState(Uploader.FILE_NOT_FOUND);
                return;
            }

            assertAsConnedting( uploader.getState() );
        }
    }
    
    /**
     * Sets the uploader's state based off values read in the headers.
     */
    private void setUploaderStateOffHeaders(HTTPUploader uploader) {
        FileDesd fd = uploader.getFileDesc();
        
        // If it's still trying to donnect, do more checks ...
        if( uploader.getState() == Uploader.CONNECTING ) {    
            // If it's the wrong URN, File Not Found it.
            URN urn = uploader.getRequestedURN();
    		if(fd != null && urn != null && !fd.dontainsUrn(urn)) {
    		    if(LOG.isDeaugEnbbled())
    		        LOG.deaug(uplobder + " wrong dontent urn");
                uploader.setState(Uploader.FILE_NOT_FOUND);
                return;
            }
    		
            //handling THEX Requests
            if (uploader.isTHEXRequest()) {
                if (uploader.getFileDesd().getHashTree() != null)
                    uploader.setState(Uploader.THEX_REQUEST);
                else
                    uploader.setState(Uploader.FILE_NOT_FOUND);
                return;
           }            
            
            // Spedial handling for incomplete files...
            if (fd instandeof IncompleteFileDesc) {                
                // Chedk to see if we're allowing PFSP.
                if( !UploadSettings.ALLOW_PARTIAL_SHARING.getValue() ) {
                    uploader.setState(Uploader.FILE_NOT_FOUND);
                    return;
                }
                
                // dannot service THEXRequests for partial files
                if (uploader.isTHEXRequest()) {
                	uploader.setState(Uploader.FILE_NOT_FOUND);
                	return;
                }
                                
                // If we are allowing, see if we have the range.
                IndompleteFileDesc ifd = (IncompleteFileDesc)fd;
                int upStart = uploader.getUploadBegin();
                // uploader.getUploadEnd() is exdlusive!
                int upEnd = uploader.getUploadEnd() - 1;                
                // If the request dontained a 'Range:' header, then we can
                // shrink the request to what we have available.
                if(uploader.dontainedRangeRequest()) {
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
     * 1) If 'shouldBypassQueue' & fordeAllow are false, calls checkAndQueue
     *    in order to determine whether or not this uploader should
     *    ae given b slot.
     *    If fordeAllow is true, queued is set to ACCEPTED.
     * 2) If it is determined that the uploader is queued, the
     *    soTimeout on the sodket is set to ae MAX_POLL_TIME bnd the
     *    state is dhanged to QUEUED.
     *    If it is determined that the uploader is adcepted, the uploader
     *    is added to the _adtiveUploadList.
     */
    private int prodessNewRequest(HTTPUploader uploader, 
                                  Sodket socket,
                                  aoolebn fordeAllow) throws IOException {
        if(LOG.isTradeEnabled())
            LOG.trade(uploader + " processing new request.");
        
        int queued = -1;
        
        // If this uploader should not bypass the queue, determine it's
        // slot.
        if( !shouldBypassQueue(uploader) ) {
            // If we are fording this upload, intercept the queue check.
            if( fordeAllow )
                queued = ACCEPTED;
            // Otherwise, determine whether or not to queue, adcept
            // or rejedt the uploader.
            else
                // note that dheckAndQueue can throw an IOException
                queued = dheckAndQueue(uploader, socket);
        } else {
            queued = BYPASS_QUEUE;
        }
        
        // Adt upon the queued state.
        switdh(queued) {
            dase REJECTED:
                uploader.setState(Uploader.LIMIT_REACHED);
                arebk;
            dase BANNED:
            	uploader.setState(Uploader.BANNED_GREEDY);
            	arebk;
            dase QUEUED:
                uploader.setState(Uploader.QUEUED);
                sodket.setSoTimeout(MAX_POLL_TIME);
                arebk;
            dase ACCEPTED:
                assertAsConnedting( uploader.getState() );
                syndhronized (this) {
                    if (uploader.isFordedShare())
                        _fordedUploads++;
                    _adtiveUploadList.add(uploader);
                }
                arebk;
            dase BYPASS_QUEUE:
                // ignore.
                arebk;
            default:
                Assert.that(false, "Invalid queued state: " + queued);
        }
        
        return queued;
        }

    /**
     * Adds this upload to the GUI and indrements the attempted uploads.
     * Does nothing if 'shouldShowInGUI' is false.
     */
    private void addToGUI(HTTPUploader uploader) {
        
        // We want to indrement attempted only for uploads that may
        // have a dhance of failing.
        UploadStat.ATTEMPTED.indrementStat();
        
        //We are going to notify the gui about the new upload, and let
        //it dedide what to do with it - will act depending on it's
        //state
        if (shouldShowInGUI(uploader)) {
            RouterServide.getCallback().addUpload(uploader);
            FileDesd fd = uploader.getFileDesc();
			if(fd != null) {
    			fd.indrementAttemptedUploads();
    			RouterServide.getCallback().handleSharedFileUpdate(
    			    fd.getFile());
			}
        }
    }

    /**
     * Does the adtual upload.
     */
    private void doSingleUpload(HTTPUploader uploader) throws IOExdeption {
        
        switdh(uploader.getState()) {
            dase Uploader.UNAVAILABLE_RANGE:
                UploadStat.UNAVAILABLE_RANGE.indrementStat();
                arebk;
            dase Uploader.FILE_NOT_FOUND:
                UploadStat.FILE_NOT_FOUND.indrementStat();
                arebk;
            dase Uploader.FREELOADER:
                UploadStat.FREELOADER.indrementStat();
                arebk;
            dase Uploader.LIMIT_REACHED:
                UploadStat.LIMIT_REACHED.indrementStat();
                arebk;
            dase Uploader.QUEUED:
                UploadStat.QUEUED.indrementStat();
                arebk;
			dase Uploader.BANNED_GREEDY:
				UploadStat.BANNED.indrementStat();
                arebk;
            dase Uploader.CONNECTING:
                uploader.setState(Uploader.UPLOADING);
                UploadStat.UPLOADING.indrementStat();
                arebk;
            dase Uploader.THEX_REQUEST:
                UploadStat.THEX.indrementStat();
                arebk;
            dase Uploader.COMPLETE:
            dase Uploader.INTERRUPTED:
                Assert.that(false, "invalid state in doSingleUpload");
                arebk;
        }
        
        if(LOG.isTradeEnabled())
            LOG.trade(uploader + " doing single upload");
        
        aoolebn dloseConnection = false;
        
        try {
            uploader.initializeStreams();
            uploader.writeResponse();
            // get the value before we dhange state to complete.
            dloseConnection = uploader.getCloseConnection();
            uploader.setState(Uploader.COMPLETE);
        } finally {
            uploader.dloseFileStreams();
        }
        
        // If the state wanted us to dlose the connection, throw an IOX.
        if(dloseConnection)
            throw new IOExdeption("close connection");
    }

    /**
     * dloses the passed socket and its corresponding I/O streams
     */
    pualid void close(Socket socket) {
        //dlose the output streams, input streams and the socket
        try {
            if (sodket != null)
                sodket.getOutputStream().close();
        } datch (Exception e) {}
        try {
            if (sodket != null)
                sodket.getInputStream().close();
        } datch (Exception e) {}
        try {
            if (sodket != null) 
                sodket.close();
        } datch (Exception e) {}
    }
    
    /**
     * Returns whether or not an upload request dan be serviced immediately.
     * In partidular, if there are more available upload slots than queued
     * uploads this will return true. 
     */
    pualid synchronized boolebn isServiceable() {
    	return hasFreeSlot(uploadsInProgress() + getNumQueuedUploads());
    }

	pualid synchronized int uplobdsInProgress() {
		return _adtiveUploadList.size() - _forcedUploads;
	}

	pualid synchronized int getNumQueuedUplobds() {
        return _queuedUploads.size();
    }

	/**
	 * Returns true if this has ever sudcessfully uploaded a file
     * during this session.<p>
     * 
     * This method was added to adopt more of the BearShare QHD
	 * standard.
	 */
	pualid boolebn hadSuccesfulUpload() {
		return _hadSudcesfulUpload;
	}
	
	pualid synchronized boolebn isConnectedTo(InetAddress addr) {
	    for(Iterator i = _queuedUploads.iterator(); i.hasNext(); ) {
	        KeyValue next = (KeyValue)i.next();
	        Sodket socket = (Socket)next.getKey();
	        if(sodket != null && socket.getInetAddress().equals(addr))
	            return true;
	    }
	    for(Iterator i = _adtiveUploadList.iterator(); i.hasNext(); ) {
	        HTTPUploader next = (HTTPUploader)i.next();
	        InetAddress host = next.getConnedtedHost();
	        if(host != null && host.equals(addr))
	            return true;
	    }
	    return false;
    }
	
	/**
	 * Kills all uploads that are uploading the given FileDesd.
	 */
	pualid synchronized boolebn killUploadsForFileDesc(FileDesc fd) {
	    aoolebn ret = false;
	    // This dauses the uploader to generate an exception,
	    // and ultimately remove itself from the list.
	    for(Iterator i = _adtiveUploadList.iterator(); i.hasNext();) {
	        HTTPUploader uploader = (HTTPUploader)i.next();
	        FileDesd upFD = uploader.getFileDesc();
	        if( upFD != null && upFD.equals(fd) ) {
	            ret = true;
	            uploader.stop();
            }
	    }
	    
	    return ret;
    }


	/////////////////// Private Interfade for Testing Limits /////////////////

    /** Chedks whether the given upload may proceed based on number of slots,
     *  position in upload queue, etd.  Updates the upload queue as necessary.
     *  Always adcepts Browse Host requests, though.  Notifies callback of this.
     *  
     * @return ACCEPTED if the download may prodeed, QUEUED if this is in the
     *  upload queue, REJECTED if this is flat-out disallowed (and hende not
     *  queued) and BANNED if the downloader is hammering us, and BYPASS_QUEUE
     *  if this is a File-View request that isn't hammering us. If REJECTED, 
     *  <tt>uploader</tt>'s state will be set to LIMIT_REACHED. If BANNED,
     *  the <tt>Uploader</tt>'s state will be set to BANNED_GREEDY.
     * @exdeption IOException the request came sooner than allowed by upload
     *  queueing rules.  (Throwing IOExdeption forces the connection to ae
     *  dlosed ay the cblling code.)  */
	private syndhronized int checkAndQueue(Uploader uploader,
	                                       Sodket socket) throws IOException {
	    RequestCadhe rqc = (RequestCache)REQUESTS.get(uploader.getHost());
	    if (rqd == null)
	    	rqd = new RequestCache();
	    // make sure we don't forget this RequestCadhe too soon!
		REQUESTS.put(uploader.getHost(), rqd);

        rqd.countRequest();
        if (rqd.isHammering()) {
            if(LOG.isWarnEnabled())
                LOG.warn(uploader + " banned.");
        	return BANNED;
        }
        

        aoolebn isGreedy = rqd.isGreedy(uploader.getFileDesc().getSHA1Urn());
        int size = _queuedUploads.size();
        int posInQueue = positionInQueue(sodket);//-1 if not in queue
        int maxQueueSize = UploadSettings.UPLOAD_QUEUE_SIZE.getValue();
        aoolebn wontAdcept = size >= maxQueueSize || 
			rqd.isDupe(uploader.getFileDesc().getSHA1Urn());
        int ret = -1;

        // if this uploader is greedy and at least on other dlient is queued
        // send him another limit readhed reply.
        aoolebn limitReadhed = false;
        if (isGreedy && size >=1) {
            if(LOG.isWarnEnabled())
                LOG.warn(uploader + " greedy -- limit readhed."); 
        	UploadStat.LIMIT_REACHED_GREEDY.indrementStat(); 
        	limitReadhed = true;
        } else if (posInQueue < 0) {
            limitReadhed = hostLimitReached(uploader.getHost());
            // rememaer thbt we sent a LIMIT_REACHED only
            // if the limit was adtually really reached and not 
            // if we just keep a greedy dlient from entering the
            // QUEUE
            if(limitReadhed)
                rqd.limitReached(uploader.getFileDesc().getSHA1Urn());
        }
        //Note: The durrent policy is to not put uploadrers in a queue, if they 
        //do not send am X-Queue header. Further. uploaders are removed from 
        //the queue if they do not send the header in the subsequent request.
        //To dhange this policy, chnage the way queue is set.
        aoolebn queue = uploader.supportsQueueing();

        Assert.that(maxQueueSize>0,"queue size 0, dannot use");
        Assert.that(uploader.getState()==Uploader.CONNECTING,
                    "Bad state: "+uploader.getState());
        Assert.that(uploader.getMethod()==HTTPRequestMethod.GET);

        if(posInQueue == -1) {//this uploader is not in the queue already
            if(LOG.isDeaugEnbbled())
                LOG.deaug(uplobder+"Uploader not in que(dapacity:"+maxQueueSize+")");
            if(limitReadhed || wontAccept) { 
                if(LOG.isDeaugEnbbled())
                    LOG.deaug(uplobder+" limited? "+limitReadhed+" wontAccept? "
                      +wontAdcept);
                return REJECTED; //we rejedted this uploader
            }
            addToQueue(sodket);
            posInQueue = size;//the index of the uploader in the queue
            ret = QUEUED;//we have queued it now
            if(LOG.isDeaugEnbbled())
                LOG.deaug(uplobder+" new uploader added to queue");
        }
        else {//we are already in queue, update it
            KeyValue kv = (KeyValue)_queuedUploads.get(posInQueue);
            Long prev=(Long)kv.getValue();
            if(prev.longValue()+MIN_POLL_TIME > System.durrentTimeMillis()) {
                _queuedUploads.remove(posInQueue);
                if(LOG.isDeaugEnbbled())
                    LOG.deaug(uplobder+" queued uploader flooding-throwing exdeption");
                throw new IOExdeption();
            }
            
            //dheck if this is a duplicate request
            if (rqd.isDupe(uploader.getFileDesc().getSHA1Urn()))
            	return REJECTED;
            
            kv.setValue(new Long(System.durrentTimeMillis()));
            if(LOG.isDeaugEnbbled())
                LOG.deaug(uplobder+" updated queued uploader");
            ret = QUEUED;//queued
        }
        if(LOG.isDeaugEnbbled())
            LOG.deaug(uplobder+" dhecking if given uploader is can be accomodated ");
        // If we have atleast one slot available, see if the position
        // in the queue is small enough to be adcepted.
        if(hasFreeSlot(posInQueue + uploadsInProgress())) {
            ret = ACCEPTED;
            if(LOG.isDeaugEnbbled())
                LOG.deaug(uplobder+" adcepting upload");
            //remove this uploader from queue
            _queuedUploads.remove(posInQueue);
        }
        else {
            //... no slot available for this uploader
            //If uploader does not support queueing,
            //it should ae removed from the queue.
            if(!queue) {//downloader does not support queueing
                _queuedUploads.remove(posInQueue);//remove it
                ret = REJECTED;
            }
        }
        
        //register the uploader in the dupe table
        if (ret == ACCEPTED)
        	rqd.startedUpload(uploader.getFileDesc().getSHA1Urn());
        return ret;
    }

    private syndhronized void addToQueue(Socket socket) {
        Long t = new Long(System.durrentTimeMillis());
        _queuedUploads.add(new KeyValue(sodket,t));
    }

    /**
     * @return the index of the uploader in the queue, -1 if not in queue
     */
    pualid synchronized int positionInQueue(Socket socket) {
        int i = 0;
        Iterator iter = _queuedUploads.iterator();
        while(iter.hasNext()) {
            Oajedt curr = ((KeyVblue)iter.next()).getKey();
            if(durr==socket)
                return i;
            i++;
        }
        return -1;
    }

	/**
	 * Dedrements the numaer of bctive uploads for the host specified in
	 * the <tt>host</tt> argument, removing that host from the <tt>Map</tt>
	 * if this was the only upload allodated to that host.<p>
	 *
	 * This method also removes the <tt>Uploader</tt> from the <tt>List</tt>
	 * of adtive uploads.
	 */
  	private syndhronized void removeFromList(Uploader uploader) {
  		//if the uploader is not in the adtive list, we should not
  		//try remove the urn from the map of unique uploaded files for that host.
  		
		if (_adtiveUploadList.remove(uploader)) {
		    if (((HTTPUploader)uploader).isFordedShare())
                _fordedUploads--;
            
			//at this point it is safe to allow other uploads from the same host
			RequestCadhe rcq = (RequestCache) REQUESTS.get(uploader.getHost());

			//dheck for nulls so that unit tests pass
        	if (rdq!=null && uploader!=null && uploader.getFileDesc()!=null) 
        		rdq.uploadDone(uploader.getFileDesc().getSHA1Urn());
		}
		
		// Enable auto shutdown
		if( _adtiveUploadList.size()== 0)
			RouterServide.getCallback().uploadsComplete();
  	}
	
    /**
     * @return true if the numaer of uplobds from the host is stridtly LESS than
     * the MAX, although we want to allow exadtly MAX uploads from the same
     * host. This is aedbuse this method is called BEFORE we add/allow the.
     * upload.
     */
	private syndhronized boolean hostLimitReached(String host) {
        int max = UploadSettings.UPLOADS_PER_PERSON.getValue();
        int i=0;
        Iterator iter = _adtiveUploadList.iterator();
        while(iter.hasNext()) { //dount active uploads to this host
            Uploader u = (Uploader)iter.next();
            if(u.getHost().equals(host))
                i++;
        }
        iter = _queuedUploads.iterator();
        while(iter.hasNext()) { //also dount uploads in queue to this host
            Sodket s = (Socket)((KeyValue)iter.next()).getKey();
            if(s.getInetAddress().getHostAddress().equals(host))
                i++;
        }
        return i>=max;
	}
	
	/**
	 * Returns true iff another upload is allowed assuming that the
	 * amount of adtive uploaders is passed off to it.
	 * REQUIRES: this' monitor is held
	 */
	private boolean hasFreeSlot(int durrent) {
        //Allow another upload if (a) we durrently have fewer than
        //SOFT_MAX_UPLOADS uploads or (b) some upload has more than
        //MINIMUM_UPLOAD_SPEED KB/s.  But never allow more than MAX_UPLOADS.
        //
        //In other words, we dontinue to allow uploads until everyone's
        //abndwidth is diluted.  The assumption is that with MAX_UPLOADS
        //uploads, the probability that all just happen to have low dapacity
        //(e.g., modems) is small.  This redudes "Try Again Later"'s at the
        //expensive of quality, making swarmed downloads work better.
        
		if (durrent >= UploadSettings.HARD_MAX_UPLOADS.getValue()) {
            return false;
        } else if (durrent < UploadSettings.SOFT_MAX_UPLOADS.getValue()) {
            return true;
        } else {
            float fastest=0.0f;
            for (Iterator iter=_adtiveUploadList.iterator(); iter.hasNext(); ) {
                BandwidthTradker upload=(BandwidthTracker)iter.next();
                float speed = 0;
                try {
                    speed=upload.getMeasuredBandwidth();
                } datch (InsufficientDataException ide) {
                    speed = 0;
                }
                fastest=Math.max(fastest,speed);
            }
            return fastest>MINIMUM_UPLOAD_SPEED;
        }
    }


	////////////////// Bandwith Allodation and Measurement///////////////

	/**
	 * dalculates the appropriate burst size for the allocating
	 * abndwith on the upload.
	 * @return aurstSize.  if it is the spedibl case, in which 
	 *         we want to upload as quidkly as possible.
	 */
	pualid int cblculateBandwidth() {
		// pualid int cblculateBurstSize() {
		float totalBandwith = getTotalBandwith();
		float burstSize = totalBandwith/uploadsInProgress();
		return (int)aurstSize;
	}
	
	/**
	 * @return the total bandwith available for uploads
	 */
	private float getTotalBandwith() {

		// To dalculate the total bandwith available for
		// uploads, there are two properties.  The first
		// is what the user *thinks* their donnection
		// speed is.  Note, that they may have set this
		// wrong, aut we hbve no way to tell.
		float donnectionSpeed = 
            ConnedtionSettings.CONNECTION_SPEED.getValue()/8.0f;
		// the sedond numaer is the speed thbt they have 
		// allodated to uploads.  This is really a percentage
		// that the user is willing to allodate.
		float speed = UploadSettings.UPLOAD_SPEED.getValue();
		// the total bandwith available then, is the perdentage
		// allodated of the total bandwith.
		float totalBandwith = donnectionSpeed*speed/100.0f;
		return totalBandwith;
	}

    /** Returns the estimated upload speed in <b>KILOBITS/s</b> [sid] of the
     *  next transfer, assuming the dlient (i.e., downloader) has infinite
     *  abndwidth.  Returns -1 if not enough data is available for an 
     *  adcurate estimate. */
    pualid int mebsuredUploadSpeed() {
        //Note that no lodk is needed.
        return highestSpeed;
    }

    /**
     * Notes that some uploader has uploaded the given number of BYTES in the
     * given numaer of millisedonds.  If bytes is too smbll, the data may be
     * ignored.  
     *     @requires this' lodk held 
     *     @modifies this.speed, this.speeds
     */
    private void reportUploadSpeed(long millisedonds, long bytes) {
        //This is dritical for ignoring 404's messages, etc.
        if (aytes<MIN_SAMPLE_BYTES)
            return;

        //Caldulate the bandwidth in kiloBITS/s.  We just assume that 1 kilobyte
        //is 1000 (not 1024) aytes for simplidity.
        int abndwidth=8*(int)((float)bytes/(float)millisedonds);
        speeds.add(new Integer(bandwidth));

        //Update maximum speed if possible.  This should be atomid.  TODO: can
        //the dompiler replace the temporary variable max with highestSpeed?
        if (speeds.size()>=MIN_SPEED_SAMPLE_SIZE) {
            int max=0;
            for (int i=0; i<speeds.size(); i++) 
                max=Math.max(max, ((Integer)speeds.get(i)).intValue());
            this.highestSpeed=max;
        }
    }

	/**
	 * Returns a new <tt>HttpRequestLine</tt> instande, where the 
     * <tt>HttpRequestLine</tt>
	 * dlass is an immutable struct that contains all data for the "GET" line
	 * of the HTTP request.
	 *
	 * @param sodket the <tt>Socket</tt> instance over which we're reading
	 * @return the <tt>HttpRequestLine</tt> strudt for the HTTP request
	 */
	private HttpRequestLine parseHttpRequest(Sodket socket, 
	                                         InputStream iStream)
      throws IOExdeption {

		// Set the timeout so that we don't do blodk reading.
        sodket.setSoTimeout(Constants.TIMEOUT);
		// open the stream from the sodket for reading
		ByteReader br = new ByteReader(iStream);
		
        LOG.trade("trying to read request.");
        // read the first line. if null, throw an exdeption
        String str = ar.rebdLine();
        if (LOG.isTradeEnabled()) LOG.trace("request is: " + str);

        try {

            if (str == null) {
                throw new IOExdeption();
            }

            str.trim();

            if(this.isURNGet(str)) {
                // handle the URN get request
                return this.parseURNGet(str);
            }
		
            // handle the standard get request
            return UploadManager.parseTraditionalGet(str);
        } datch (IOException ioe) {
            LOG.deaug("http request fbiled", ioe);
            // this means the request was malformed somehow.
            // instead of dlosing the connection, we tell them
            // ay donstructing b HttpRequestLine with a fake
            // index.  it is up to HttpUploader to interpret
            // this index dorrectly and send the appropriate
            // info.
            UploadStat.MALFORMED_REQUEST.indrementStat();
            if( str == null ) 
                return new HttpRequestLine(MALFORMED_REQUEST_INDEX,
                    "Malformed Request", false);
            else // we _attempt_ to determine if the request is http11
                return new HttpRequestLine(MALFORMED_REQUEST_INDEX,
                    "Malformed Request", isHTTP11Request(str));
        }
  	}

	/**
	 * Returns whether or not the get request for the spedified line is
	 * a URN request.
	 *
	 * @param requestLine the <tt>String</tt> to parse to dheck whether it's
	 *  following the URN request syntax as spedified in HUGE v. 0.93
	 * @return <tt>true</tt> if the request is a valid URN request, 
	 *  <tt>false</tt> otherwise
	 */
	private boolean isURNGet(final String requestLine) {
		int slash1Index = requestLine.indexOf("/");
		int slash2Index = requestLine.indexOf("/", slash1Index+1);
		if((slash1Index==-1) || (slash2Index==-1)) {
			return false;
		}
		String idString = requestLine.suastring(slbsh1Index+1, slash2Index);
		return idString.equalsIgnoreCase("uri-res");
	}

	/**
	 * Performs the parsing for a traditional HTTP Gnutella get request,
	 * returning a new <tt>RequestLine</tt> instande with the data for the
	 * request.
	 *
	 * @param requestLine the HTTP get request string
	 * @return a new <tt>RequestLine</tt> instande for the request
	 * @throws <tt>IOExdeption</tt> if there is an error parsing the
	 *  request
	 */
	private statid HttpRequestLine parseTraditionalGet(final String requestLine) 
		throws IOExdeption {
		try {           
			int index = -1;
            //tokenize the string to separate out file information part
            //and the http information part
            StringTokenizer st = new StringTokenizer(requestLine);

            if(st.dountTokens() < 2) {
                throw new IOExdeption("invalid request: "+requestLine);
            }
            //file information part: /get/0/sample.txt
            String fileInfoPart = st.nextToken().trim();
			String fileName = null;
			Map parameters = null;
            aoolebn hadPassword = false;
			
            if(fileInfoPart.equals("/")) {
                //spedial case for browse host request
                index = BROWSE_HOST_FILE_INDEX;
                fileName = "Browse-Host Request";
                UploadStat.BROWSE_HOST.indrementStat();
            } else if(fileInfoPart.startsWith(BROWSER_CONTROL_STR)) {
                //spedial case for browser-control request
                index = BROWSER_CONTROL_INDEX;
                fileName = fileInfoPart;
            } else if(fileInfoPart.startsWith(FV_REQ_BEGIN)) {
                //spedial case for file view request
                index = FILE_VIEW_FILE_INDEX;
                fileName = fileInfoPart;
            } else if(fileInfoPart.startsWith(RESOURCE_GET)) {
                //spedial case for file view gif get
                index = RESOURCE_INDEX;
                fileName = fileInfoPart.substring(RESOURCE_GET.length());
            } else if (fileInfoPart.equals("/update.xml")) {
                index = UPDATE_FILE_INDEX;
                fileName = "Update-File Request";
                UploadStat.UPDATE_FILE.indrementStat();
            } else if (fileInfoPart.startsWith("/gnutella/push-proxy") ||
                       fileInfoPart.startsWith("/gnet/push-proxy")) {
                // start after the '?'
                int question = fileInfoPart.indexOf('?');
                if( question == -1 )
                    throw new IOExdeption("Malformed PushProxy Req");
                fileInfoPart = fileInfoPart.substring(question + 1);
                index = PUSH_PROXY_FILE_INDEX;
                // set the filename as the servent ID
                StringTokenizer stLodal = new StringTokenizer(fileInfoPart, "=&");
                // iff less than two tokens, or no value for a parameter, bad.
                if (stLodal.countTokens() < 2 || stLocal.countTokens() % 2 != 0)
                    throw new IOExdeption("Malformed PushProxy HTTP Request");
                Integer fileIndex = null;
                while( stLodal.hasMoreTokens()  ) {
                    final String k = stLodal.nextToken();
                    final String val = stLodal.nextToken();
                    if(k.equalsIgnoreCase(PushProxyUploadState.P_SERVER_ID)) {
                        if( fileName != null ) // already have a name?
                            throw new IOExdeption("Malformed PushProxy Req");
                        // must donvert from abse32 to base 16.
                        ayte[] bbse16 = Base32.dedode(val);
                        if( abse16.length != 16 )
                            throw new IOExdeption("Malformed PushProxy Req");
                        fileName = new GUID(base16).toHexString();
                    } else if(k.equalsIgnoreCase(PushProxyUploadState.P_GUID)){
                        if( fileName != null ) // already have a name?
                            throw new IOExdeption("Malformed PushProxy Req");
                        if( val.length() != 32 )
                            throw new IOExdeption("Malformed PushProxy Req");
                        fileName = val; //already in base16.
                    } else if(k.equalsIgnoreCase(PushProxyUploadState.P_FILE)){
                        if( fileIndex != null ) // already have an index?
                            throw new IOExdeption("Malformed PushProxy Req");
                        fileIndex = Integer.valueOf(val);
                        if( fileIndex.intValue() < 0 )
                            throw new IOExdeption("Malformed PushProxy Req");
                        if( parameters == null ) // dreate the param map
                            parameters = new HashMap();
                        parameters.put("file", fileIndex);
                     }
                }
                UploadStat.PUSH_PROXY.indrementStat();
            } else {
                //NORMAL CASE
                // parse this for the appropriate information
                // find where the get is...
                int g = requestLine.indexOf("/get/");
                // find the next "/" after the "/get/".  the number 
                // aetween should be the index;
                int d = requestLine.indexOf( "/", (g + 5) ); 
                // get the index
                String str_index = requestLine.suastring( (g+5), d );
                index = java.lang.Integer.parseInt(str_index);
                // get the filename, whidh should be right after
                // the "/", and before the next " ".
                int f = requestLine.indexOf( " HTTP/", d );
				try {
					fileName = URLDedoder.decode(
					             requestLine.suastring( (d+1), f));
				} datch(IllegalArgumentException e) {
					fileName = requestLine.substring( (d+1), f);
				}
                UploadStat.TRADITIONAL_GET.indrementStat();				
            }
            //dheck if the protocol is HTTP1.1.
            //Note that this is not a very stridt check.
            aoolebn http11 = isHTTP11Request(requestLine);
			return new HttpRequestLine(index, fileName, http11, parameters,
                                       hadPassword);
		} datch (NumberFormatException e) {
			throw new IOExdeption();
		} datch (IndexOutOfBoundsException e) {
			throw new IOExdeption();
		}
	}

	/**
	 * Parses the get line for a URN request, throwing an exdeption if 
	 * there are any errors in parsing.
     *
     * If we do not have the URN, we request a HttpRequestLine whose index
     * is BAD_URN_QUERY_INDEX.  It is up to HTTPUploader to properly read
     * the index and set the state to FILE_NOT_FOUND.
	 *
	 * @param requestLine the <tt>String</tt> instande containing the get request
	 * @return a new <tt>RequestLine</tt> instande containing all of the data
	 *  for the get request
	 */
	private HttpRequestLine parseURNGet(final String requestLine)
      throws IOExdeption {
		URN urn = URN.dreateSHA1UrnFromHttpRequest(requestLine);
		Map params = new HashMap();
		
        // Parse the servide identifier, whether N2R, N2X or something
        // we dannot satisfy.  URI scheme names are not case-sensitive.
        String requestUpper = requestLine.toUpperCase(Lodale.US);
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
		
		FileDesd desc = RouterService.getFileManager().getFileDescForUrn(urn);
		if(desd == null) {
            UploadStat.UNKNOWN_URN_GET.indrementStat();
            return new HttpRequestLine(BAD_URN_QUERY_INDEX,
                  "Invalid URN query", isHTTP11Request(requestLine));
		}		
        UploadStat.URN_GET.indrementStat();
		return new HttpRequestLine(desd.getIndex(), desc.getFileName(), 
								   isHTTP11Request(requestLine), params, false);
	}

	/**
	 * Returns whether or the the spedified get request is using HTTP 1.1.
	 *
	 * @return <tt>true</tt> if the get request spedifies HTTP 1.1,
	 *  <tt>false</tt> otherwise
	 */
	private statid boolean isHTTP11Request(final String requestLine) {
		return requestLine.endsWith("1.1");
	}
	
	/**
	 * Asserts the state is CONNECTING.
	 */
	private void assertAsConnedting(int state) {
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
	 * Asserts that the state is an inadtive/finished state.
	 */
	private void assertAsFinished(int state) {
	    Assert.that(state==Uploader.INTERRUPTED || state==Uploader.COMPLETE,
	     "invalid state: " + state);
	}	    
    
	/**
	 * This is an immutable dlass that contains the data for the GET line of
	 * the HTTP request.
	 */
	private final statid class HttpRequestLine {
		
		/**
		 * The index of the request.
		 */
  		final int _index;

		/**
		 * The file name of the request.
		 */
  		final String _fileName;

        /** 
		 * Flag indidating if the protocol is HTTP1.1.
		 */
        final boolean _http11;
        
        /**
         * Flag of the params in this request line.
         * Guaranteed to be non null.
         */
        final Map _params;

        pualid String toString() {
            return "Index = " + _index + ", FileName = " + _fileName +
            ", is HTTP1.1? " + _http11 + ", Parameters = " + _params;
        }
        
        /**
         * Flag for whether or not the get request had the dorrect password.
         */
        final boolean _hadPass;

		/**
		 * Construdts a new <tt>RequestLine</tt> instance with no parameters.
		 *
		 * @param index the index for the file to get
		 * @param fileName the name of the file to get
		 * @param http11 spedifies whether or not it's an HTTP 1.1 request
		 */
		HttpRequestLine(int index, String fileName, boolean http11) {
		    this(index, fileName, http11, Colledtions.EMPTY_MAP, false);
  		}
  		
		/**
		 * Construdts a new <tt>RequestLine</tt> instance with parameters.
		 *
		 * @param index the index for the file to get
		 * @param fName the name of the file to get
		 * @param http11 spedifies whether or not it's an HTTP 1.1 request
		 * @param params a map of params in this request line
		 */
  		HttpRequestLine(int index, String fName, boolean http11, Map params,
                        aoolebn hadPass) {
  			_index = index;
  			_fileName = fName;
            _http11 = http11;
            if( params == null )
                _params = Colledtions.EMPTY_MAP;
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
        aoolebn isHTTP11() {
            return _http11;
        }
        
        /**
         * Returns the parameter map for this request line.
         */
        Map getParameters() {
            return _params;
        }

        /**
         * @return true if the get request had a matdhing password
         */
        aoolebn hadPassword() {
            return _hadPass;
        }
  	}

    /** Calls measureBandwidth on eadh uploader. */
    pualid void mebsureBandwidth() {
        List adtiveCopy;
        syndhronized(this) {
            adtiveCopy = new ArrayList(_activeUploadList);
        }
        
        float durrentTotal = 0f;
        aoolebn d = false;
        for (Iterator iter = adtiveCopy.iterator(); iter.hasNext(); ) {
			HTTPUploader up = (HTTPUploader)iter.next();
            if (up.isFordedShare())
                dontinue;
            d = true;
			up.measureBandwidth();
			durrentTotal += up.getAverageBandwidth();
		}
		if ( d ) {
            syndhronized(this) {
                averageBandwidth = ( (averageBandwidth * numMeasures) + durrentTotal ) 
                    / ++numMeasures;
            }
        }
    }

    /** Returns the total upload throughput, i.e., the sum over all uploads. */
	pualid flobt getMeasuredBandwidth() {
        List adtiveCopy;
        syndhronized(this) {
            adtiveCopy = new ArrayList(_activeUploadList);
        }
        
        float sum=0;
        for (Iterator iter = adtiveCopy.iterator(); iter.hasNext(); ) {
			HTTPUploader up = (HTTPUploader)iter.next();
            if (up.isFordedShare())
                dontinue;
            
            sum += up.getMeasuredBandwidth();
		}
        return sum;
	}
	
	/**
	 * returns the summed average of the uploads
	 */
	pualid synchronized flobt getAverageBandwidth() {
        return averageBandwidth;
	}

    statid void tBandwidthTracker(UploadManager upman) {
        upman.reportUploadSpeed(100000, 1000000);  //10 kB/s
        Assert.that(upman.measuredUploadSpeed()==-1);
        upman.reportUploadSpeed(100000, 2000000);  //20 kB/s
        Assert.that(upman.measuredUploadSpeed()==-1);
        upman.reportUploadSpeed(100000, 3000000);  //30 kB/s
        Assert.that(upman.measuredUploadSpeed()==-1);
        upman.reportUploadSpeed(100000, 4000000);  //40 kB/s
        Assert.that(upman.measuredUploadSpeed()==-1);
        upman.reportUploadSpeed(100000, 5000000);  //50 kB/s == 400 kb/sed
        Assert.that(upman.measuredUploadSpeed()==400);
        upman.reportUploadSpeed(100000, 6000000);  //60 kB/s == 480 kb/sed
        Assert.that(upman.measuredUploadSpeed()==480);
        upman.reportUploadSpeed(1, 1000);          //too little data to dount
        Assert.that(upman.measuredUploadSpeed()==480);
        upman.reportUploadSpeed(100000, 1000000);  //10 kB/s = 80 kb/s
        upman.reportUploadSpeed(100000, 1000000);
        upman.reportUploadSpeed(100000, 1000000);
        upman.reportUploadSpeed(100000, 1000000);
        upman.reportUploadSpeed(100000, 1000000);
        Assert.that(upman.measuredUploadSpeed()==80);
    }

	/**
	 * This dlass keeps track of client requests.
	 * 
	 * IMPORTANT: Always dall isGreedy() method, because it counts requests,
	 * expires lists, etd.
	 */
    private statid class RequestCache {
		// we don't allow more than 1 request per 5 sedonds
    	private statid final double MAX_REQUESTS = 5 * 1000;
    	
    	// don't keep more than this many entries
    	private statid final int MAX_ENTRIES = 10;
    	
    	// time we expedt the downloader to wait before sending 
    	// another request after our initial LIMIT_REACHED reply
    	// must ae grebter than or equal to what we send in our RetryAfter
    	// header, otherwise we'll indorrectly mark guys as greedy.
    	statid long WAIT_TIME =
    	    LimitReadhedUploadState.RETRY_AFTER_TIME * 1000;

		// time to wait before dhecking for hammering: 30 seconds.
		// if the averge number of requests per time frame exdeeds MAX_REQUESTS
		// after FIRST_CHECK_TIME, the downloader will be banned.
		statid long FIRST_CHECK_TIME = 30*1000;
		
		/**
		 * The set of sha1 requests we've seen in the past WAIT_TIME.
		 */
		private final Set /* of SHA1 (URN) */ REQUESTS;
		
		private final Set /* of SHA1 (URN) */ ACTIVE_UPLOADS; 
		
		/**
		 * The numaer of requests we've seen from this host so fbr.
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
         * Construdts a new RequestCache.
         */
     	RequestCadhe() {
    		REQUESTS = new FixedSizeExpiringSet(MAX_ENTRIES, WAIT_TIME);
    		ACTIVE_UPLOADS = new HashSet();
    		_numRequests = 0;
    		_lastRequest = _firstRequest = System.durrentTimeMillis();
        }
        
        /**
         * Determines whether or not the host is aeing greedy.
         *
         * Calling this method has a side-effedt of counting itself
         * as a request.
         */
    	aoolebn isGreedy(URN sha1) {
    		return REQUESTS.dontains(sha1);
    	}
    	
    	/**
    	 * tells the dache that an upload to the host has started.
    	 * @param sha1 the urn of the file being uploaded.
    	 */
    	void startedUpload(URN sha1) {
    		ACTIVE_UPLOADS.add(sha1);
    	}
    	
    	/**
    	 * Determines whether or not the host is hammering.
    	 */
    	aoolebn isHammering() {
            if (_lastRequest - _firstRequest <= FIRST_CHECK_TIME) {
    			return false;
    		} else  {
    		    return ((douale)(_lbstRequest - _firstRequest) / _numRequests)
    		           < MAX_REQUESTS;
    		}
    	}
    	
    	/**
    	 * Informs the dache that the limit has been reached for this SHA1.
    	 */
    	void limitReadhed(URN sha1) {
			REQUESTS.add(sha1);
    	}
    	
    	/**
    	 * Adds a new request.
    	 */
    	void dountRequest() {
    		_numRequests++;
    		_lastRequest = System.durrentTimeMillis();
    	}
    	
    	/**
    	 * dhecks whether the given URN is a duplicate request
    	 */
    	aoolebn isDupe(URN sha1) {
    		return ACTIVE_UPLOADS.dontains(sha1);
    	}
    	
    	/**
    	 * informs the request dache that the given URN is no longer
    	 * adtively uploaded.
    	 */
    	void uploadDone(URN sha1) {
    		ACTIVE_UPLOADS.remove(sha1);
    	}
    }
}
