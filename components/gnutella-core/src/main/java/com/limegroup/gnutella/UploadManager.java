pbckage com.limegroup.gnutella;

import jbva.io.BufferedInputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.net.Socket;
import jbva.net.InetAddress;
import jbva.util.ArrayList;
import jbva.util.Collections;
import jbva.util.HashMap;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Locale;
import jbva.util.Map;
import jbva.util.Set;
import jbva.util.StringTokenizer;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.bitzi.util.Bbse32;
import com.limegroup.gnutellb.downloader.Interval;
import com.limegroup.gnutellb.http.HTTPConstants;
import com.limegroup.gnutellb.http.HTTPRequestMethod;
import com.limegroup.gnutellb.http.ProblemReadingHeaderException;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.settings.SharingSettings;
import com.limegroup.gnutellb.settings.UploadSettings;
import com.limegroup.gnutellb.statistics.UploadStat;
import com.limegroup.gnutellb.uploader.FreeloaderUploadingException;
import com.limegroup.gnutellb.uploader.HTTPUploader;
import com.limegroup.gnutellb.uploader.LimitReachedUploadState;
import com.limegroup.gnutellb.uploader.PushProxyUploadState;
import com.limegroup.gnutellb.uploader.StalledUploadWatchdog;
import com.limegroup.gnutellb.util.Buffer;
import com.limegroup.gnutellb.util.FixedSizeExpiringSet;
import com.limegroup.gnutellb.util.FixedsizeForgetfulHashMap;
import com.limegroup.gnutellb.util.IOUtils;
import com.limegroup.gnutellb.util.KeyValue;
import com.limegroup.gnutellb.util.URLDecoder;

/**
 * This clbss parses HTTP requests and delegates to <tt>HTTPUploader</tt>
 * to hbndle individual uploads.
 *
 * The stbte of HTTPUploader is maintained by this class.
 * HTTPUplobder's state follows the following pattern:
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
 * The stbtes in the middle (those other than CONNECTING, COMPLETE
 *   bnd INTERRUPTED) are part of the "State Pattern" and have an 
 * bssociated class that implements HTTPMessage.
 *
 * These stbte pattern classes are ONLY set while a transfer is active.
 * For exbmple, after we determine a request should be 'File Not Found',
 * bnd send the response back, the state will become COMPLETE (unless
 * there wbs an IOException while sending the response, in which case
 * the stbte will become INTERRUPTED).  To retrieve the last state
 * thbt was used for transferring, use HTTPUploader.getLastTransferState().
 *
 * Of pbrticular note is that Queued uploaders are actually in COMPLETED
 * stbte for the majority of the time.  The QUEUED state is only active
 * when we bre actively writing back the 'You are queued' response.
 *
 * COMPLETE uplobders may be using HTTP/1.1, in which case the HTTPUploader
 * recycles bbck to CONNECTING upon receiving the next GET/HEAD request
 * bnd repeats.
 *
 * INTERRUPTED HTTPUplobders are never reused.  However, it is possible that
 * the socket mby be reused.  This odd case is ONLY possible when a requester
 * is queued for one file bnd sends a subsequent request for another file.
 * The first HTTPUplobder is set as interrupted and a second one is created
 * for the new file, using the sbme socket as the first one.
 *
 * @see com.limegroup.gnutellb.uploader.HTTPUploader
 */
public clbss UploadManager implements BandwidthTracker {
    
    privbte static final Log LOG = LogFactory.getLog(UploadManager.class);

    /** An enumerbtion of return values for queue checking. */
    privbte final int BYPASS_QUEUE = -1;
    privbte final int REJECTED = 0;    
    privbte final int QUEUED = 1;
    privbte final int ACCEPTED = 2;
    privbte final int BANNED = 3;
    /** The min bnd max allowed times (in milliseconds) between requests by
     *  queued hosts. */
    public stbtic final int MIN_POLL_TIME = 45000; //45 sec
    public stbtic final int MAX_POLL_TIME = 120000; //120 sec

	/**
	 * This is b <tt>List</tt> of all of the current <tt>Uploader</tt>
	 * instbnces (all of the uploads in progress).  
	 */
	privbte List /* of Uploaders */ _activeUploadList = new LinkedList();

    /** The list of queued uplobds.  Most recent uploads are added to the tail.
     *  Ebch pair contains the underlying socket and the time of the last
     *  request. */
    privbte List /*of KeyValue (Socket,Long) */ _queuedUploads = 
        new ArrbyList();

    
	/** set to true when bn upload has been succesfully completed. */
	privbte volatile boolean _hadSuccesfulUpload=false;
    
    /** Number of force-shbred active uploads */
    privbte int _forcedUploads;
    
	/**
	 * LOCKING: obtbin this' monitor before modifying any 
	 * of the dbta structures
	 */

    /** The number of uplobds considered when calculating capacity, if possible.
     *  BebrShare uses 10.  Settings it too low causes you to be fooled be a
     *  strebk of slow downloaders.  Setting it too high causes you to be fooled
     *  by b number of quick downloads before your slots become filled.  */
    privbte static final int MAX_SPEED_SAMPLE_SIZE=5;
    /** The min number of uplobds considered to give out your speed.  Same 
     *  criterib needed as for MAX_SPEED_SAMPLE_SIZE. */
    privbte static final int MIN_SPEED_SAMPLE_SIZE=5;
    /** The minimum number of bytes trbnsferred by an uploadeder to count. */
    privbte static final int MIN_SAMPLE_BYTES=200000;  //200KB
    /** The bverage speed in kiloBITs/second of the last few uploads. */
    privbte Buffer /* of Integer */ speeds=new Buffer(MAX_SPEED_SAMPLE_SIZE);
    /** The highestSpeed of the lbst few downloads, or -1 if not enough
     *  downlobds have been down for an accurate sample.
     *  INVARIANT: highestSpeed>=0 ==> highestSpeed==mbx({i | i in speeds}) 
     *  INVARIANT: speeds.size()<MIN_SPEED_SAMPLE_SIZE <==> highestSpeed==-1
     */
    privbte volatile int highestSpeed=-1;
    
    /**
     * The number of mebsureBandwidth's we've had
     */
    privbte int numMeasures = 0;
    
    /**
     * The current bverage bandwidth
     */
    privbte float averageBandwidth = 0f;

    /** The desired minimum qublity of service to provide for uploads, in
     *  KB/s.  See testTotblUploadLimit. */
    privbte static final float MINIMUM_UPLOAD_SPEED=3.0f;
    
    /** 
     * The file index used in this structure to indicbte a browse host
     * request
     */
    public stbtic final int BROWSE_HOST_FILE_INDEX = -1;
    
    /**
     * The file index used in this structure to indicbte an update-file
     * request
     */
    public stbtic final int UPDATE_FILE_INDEX = -2;
    
    /**
     * The file index used in this structure to indicbte a bad URN query.
     */
    public stbtic final int BAD_URN_QUERY_INDEX = -3;
    
    /**
     * The file index used in this structure to indicbte a malformed request.
     */
    public stbtic final int MALFORMED_REQUEST_INDEX = -4;

    /** 
     * The file index used in this structure to indicbte a Push Proxy 
     * request.
     */
    public stbtic final int PUSH_PROXY_FILE_INDEX = -5;
    
    /** 
     * The file index used in this structure to indicbte a HTTP File View
     * downlobd request.
     */
    public stbtic final int FILE_VIEW_FILE_INDEX = -6;
    
    /** 
     * The file index used in this structure to indicbte a HTTP Resource Get.
     */
    public stbtic final int RESOURCE_INDEX = -7;

    /** 
     * The file index used in this structure to indicbte a special request from a browser.
     */
    public stbtic final int BROWSER_CONTROL_INDEX = -8;

    /**
     * Constbnt for the beginning of a BrowserControl request.
     */
    public stbtic final String BROWSER_CONTROL_STR = "/browser-control";
    
    /**
     * Constbnt for HttpRequestLine parameter
     */
    public stbtic final String SERVICE_ID = "service_id";
                
    /**
     * Constbnt for the beginning of a file-view request.
     */
    public stbtic final String FV_REQ_BEGIN = "/gnutella/file-view";

    /**
     * Constbnt for file-view gif get.
     */
    public stbtic final String RESOURCE_GET = "/gnutella/res/";

	/**
     * Remembers uplobders to disadvantage uploaders that
     * hbmmer us for download slots. Stores up to 250 entries
     * Mbps IP String to RequestCache   
     */
    privbte final Map /* of String to RequestCache */ REQUESTS =
        new FixedsizeForgetfulHbshMap(250);
                
	/**
	 * Accepts b new upload, creating a new <tt>HTTPUploader</tt>
	 * if it successfully pbrses the HTTP request.  BLOCKING.
	 *
	 * @pbram method the initial request type to use, e.g., GET or HEAD
	 * @pbram socket the <tt>Socket</tt> that will be used for the new upload.
     *  It is bssumed that the initial word of the request (e.g., "GET") has
     *  been consumed (e.g., by Acceptor)
     * @pbram forceAllow forces the UploadManager to allow all requests
     *  on this socket to tbke place.
	 */
    public void bcceptUpload(final HTTPRequestMethod method,
                             Socket socket, boolebn forceAllow) {
        
        LOG.trbce("accepting upload");
        HTTPUplobder uploader = null;
        long stbrtTime = -1;
		try {
            int queued = -1;
            String oldFileNbme = "";
            HTTPRequestMethod currentMethod=method;
            StblledUploadWatchdog watchdog = new StalledUploadWatchdog();
            InputStrebm iStream = null;
            boolebn startedNewFile = false;
            //do uplobds
            while(true) {
                if( uplobder != null )
                    bssertAsComplete( uploader.getState() );
                
                if(iStrebm == null)
                    iStrebm = new BufferedInputStream(socket.getInputStream());
                
                LOG.trbce("parsing http line.");
                HttpRequestLine line = pbrseHttpRequest(socket, iStream);
                if (LOG.isTrbceEnabled())
                    LOG.trbce("line = " + line);
                
                if(LOG.isDebugEnbbled())
                    LOG.debug(uplobder + " successfully parsed request");
                
                String fileNbme = line._fileName;
                
                // Determine if this is b new file ...
                if( uplobder == null                // no previous uploader
                 || currentMethod != uplobder.getMethod()  // method change
                 || !oldFileNbme.equalsIgnoreCase(fileName) ) { // new file
                    stbrtedNewFile = true;
                } else {
                    stbrtedNewFile = false;
                }
                
                // If we're stbrting a new uploader, clean the old one up
                // bnd then create a new one.
                if(stbrtedNewFile) {
                    if(LOG.isDebugEnbbled())
                        LOG.debug(uplobder + " starting new file "+line._fileName+" index "+line._index);
                    if (uplobder != null) {
                        // Becbuse queueing is per-socket (and not per file),
                        // we do not wbnt to reset the queue status if they're
                        // requesting b new file.
                        if(queued != QUEUED)
                            queued = -1;
                        // However, we DO wbnt to make sure that the old file
                        // is interpreted bs interrupted.  Otherwise,
                        // the GUI would show two lines with the the sbme slot
                        // until the newer line finished, bt which point
                        // the first one would displby as a -1 queue position.
                        else
                            uplobder.setState(Uploader.INTERRUPTED);

                        clebnupFinishedUploader(uploader, startTime);
                    }
                    uplobder = new HTTPUploader(currentMethod,
                                                fileNbme, 
						    			        socket,
							    		        line._index,
							    		        line.getPbrameters(),
								    	        wbtchdog,
                                                line.hbdPassword());
                }
                // Otherwise (we're continuing bn uploader),
                // reinitiblize the existing HTTPUploader.
                else {
                    if(LOG.isDebugEnbbled())
                        LOG.debug(uplobder + " continuing old file");
                    uplobder.reinitialize(currentMethod, line.getParameters());
                }
                
                bssertAsConnecting( uploader.getState() );
        
                setInitiblUploadingState(uploader);
                try {
                    uplobder.readHeader(iStream);
                    setUplobderStateOffHeaders(uploader);
                } cbtch(ProblemReadingHeaderException prhe) {
                    // if there wbs a problem reading the header,
                    // this is b bad request, so let them know.
                    // we do NOT throw the IOX bgain because the
                    // connection is still open.
                    uplobder.setState(Uploader.MALFORMED_REQUEST);
                }cbtch (FreeloaderUploadingException fue){
                    // browser request
				     uplobder.setState(Uploader.FREELOADER);
				}
                
                if(LOG.isDebugEnbbled())
                    LOG.debug(uplobder+" HTTPUploader created and read all headers");

                // If we hbve not accepted this file already, then
                // find out whether or not we should.
                if( queued != ACCEPTED ) {                	
                    queued = processNewRequest(uplobder, socket, forceAllow);
                    
                    // If we just bccepted this request,
                    // set the stbrt time appropriately.
                    if( queued == ACCEPTED )
                        stbrtTime = System.currentTimeMillis();     
                    
                }
                
                // If we stbrted a new file with this request, attempt
                // to displby it in the GUI.
                if( stbrtedNewFile ) {
                    bddToGUI(uploader);
                }

                // Do the bctual upload.
                doSingleUplobd(uploader);
                
                bssertAsFinished( uploader.getState() );
                
                
                oldFileNbme = fileName;
                
                //if this is not HTTP11, then exit, bs no more requests will
                //come.
                if ( !line.isHTTP11() )
                    return;

                //rebd the first word of the next request and proceed only if
                //"GET" or "HEAD" request.  Versions of LimeWire before 2.7
                //forgot to switch the request method.
                if(LOG.isDebugEnbbled())
                    LOG.debug(uplobder+" waiting for next request with socket ");
                int oldTimeout = socket.getSoTimeout();
                if(queued!=QUEUED)
                    socket.setSoTimeout(ShbringSettings.PERSISTENT_HTTP_CONNECTION_TIMEOUT.getValue());
                    
                //dont rebd a word of size more than 4 
                //bs we will handle only the next "HEAD" or "GET" request
                String word = IOUtils.rebdWord(
                    iStrebm, 4);
                if(LOG.isDebugEnbbled())
                    LOG.debug(uplobder+" next request arrived ");
                socket.setSoTimeout(oldTimeout);
                if (word.equbls("GET")) {
                    currentMethod=HTTPRequestMethod.GET;
                    UplobdStat.SUBSEQUENT_GET.incrementStat();
                } else if (word.equbls("HEAD")) {
                    currentMethod=HTTPRequestMethod.HEAD;
                    UplobdStat.SUBSEQUENT_HEAD.incrementStat();
                } else {
                    //Unknown request type
                    UplobdStat.SUBSEQUENT_UNKNOWN.incrementStat();
                    return;
                }
            }//end of while
        } cbtch(IOException ioe) {//including InterruptedIOException
            if(LOG.isDebugEnbbled())
                LOG.debug(uplobder + " IOE thrown, closing socket", ioe);
        } finblly {
            // The stbtes SHOULD be INTERRUPTED or COMPLETED
            // here.  However, it is possible thbt an IOException
            // or other uncbught exception (that will be handled
            // outside of this method) were thrown bt random points.
            // It is not b good idea to throw any exceptions here
            // becbuse the triggering exception will be lost,
            // so we just set the stbte to INTERRUPTED if it was not
            // blready complete.
            // It is possible to prove thbt the state is either
            // interrupted or complete in the cbse of normal
            // progrbm flow.
            if( uplobder != null ) {
            	if( uplobder.getState() != Uploader.COMPLETE )
                	uplobder.setState(Uploader.INTERRUPTED);
            }
            
            synchronized(this) {
                // If this uplobder is still in the queue, remove it.
                // Also chbnge its state from COMPLETE to INTERRUPTED
                // becbuse it didn't really complete.
                boolebn found = false;
                for(Iterbtor iter=_queuedUploads.iterator();iter.hasNext();){
                    KeyVblue kv = (KeyValue)iter.next();
                    if(kv.getKey()==socket) {
                        iter.remove();
                        found = true;
                        brebk;
                    }
                }
                if(found)
                    uplobder.setState(Uploader.INTERRUPTED);
            }
            
            // Alwbys clean up the finished uploader
            // from the bctive list & report the upload speed
            if( uplobder != null ) {
                uplobder.stop();
                clebnupFinishedUploader(uploader, startTime);
            }
            
            if(LOG.isDebugEnbbled())
                LOG.debug(uplobder + " closing socket");
            //close the socket
            close(socket);
        }
    }
    
    /**
     * Determines whether or no this Uplobder should be shown
     * in the GUI.
     */
    privbte boolean shouldShowInGUI(HTTPUploader uploader) {
        return uplobder.getIndex() != BROWSE_HOST_FILE_INDEX &&
               uplobder.getIndex() != PUSH_PROXY_FILE_INDEX &&
               uplobder.getIndex() != UPDATE_FILE_INDEX &&
               uplobder.getIndex() != MALFORMED_REQUEST_INDEX &&
               uplobder.getIndex() != BAD_URN_QUERY_INDEX &&
               uplobder.getIndex() != FILE_VIEW_FILE_INDEX &&
               uplobder.getIndex() != RESOURCE_INDEX &&
               uplobder.getIndex() != BROWSER_CONTROL_INDEX &&
               uplobder.getMethod() != HTTPRequestMethod.HEAD &&
               !uplobder.isForcedShare();
	}
    
    /**
     * Determines whether or not this Uplobder should bypass queueing,
     * (mebning that it will always work immediately, and will not use
     *  up slots for other uplobders).
     *
     * All requests thbt are not the 'connecting' state should bypass
     * the queue, becbuse they have already been queued once.
     *
     * Don't let FILE_VIEW requests bypbss the queue, we want to make sure
     * those guys don't hbmmer.
     */
    privbte boolean shouldBypassQueue(HTTPUploader uploader) {
        return uplobder.getState() != Uploader.CONNECTING ||
               uplobder.getMethod() == HTTPRequestMethod.HEAD ||
               uplobder.isForcedShare();
    }
    
    /**
     * Clebns up a finished uploader.
     * This does the following:
     * 1) Reports the speed bt which this upload occured.
     * 2) Removes the uplobder from the active upload list
     * 3) Closes the file strebms that the uploader has left open
     * 4) Increments the completed uplobds in the FileDesc
     * 5) Removes the uplobder from the GUI.
     * (4 & 5 bre only done if 'shouldShowInGUI' is true)
     */
    privbte void cleanupFinishedUploader(HTTPUploader uploader, long startTime) {
        if(LOG.isTrbceEnabled())
            LOG.trbce(uploader + " cleaning up finished.");
        
        int stbte = uploader.getState();
        int lbstState = uploader.getLastTransferState();        
        bssertAsFinished(state);
                     
        long finishTime = System.currentTimeMillis();
        synchronized(this) {
            //Report how quickly we uplobded the data.
            if(stbrtTime > 0) {
                reportUplobdSpeed( finishTime-startTime,
                                   uplobder.getTotalAmountUploaded());
            }
            removeFromList(uplobder);
        }
        
        uplobder.closeFileStreams();
        
        switch(stbte) {
            cbse Uploader.COMPLETE:
                UplobdStat.COMPLETED.incrementStat();
                if( lbstState == Uploader.UPLOADING ||
                    lbstState == Uploader.THEX_REQUEST)
                    UplobdStat.COMPLETED_FILE.incrementStat();
                brebk;
            cbse Uploader.INTERRUPTED:
                UplobdStat.INTERRUPTED.incrementStat();
                brebk;
        }
        
        if ( shouldShowInGUI(uplobder) ) {
            FileDesc fd = uplobder.getFileDesc();
            if( fd != null && 
              stbte == Uploader.COMPLETE &&
              (lbstState == Uploader.UPLOADING ||
               lbstState == Uploader.THEX_REQUEST)) {
                fd.incrementCompletedUplobds();
                RouterService.getCbllback().handleSharedFileUpdate(
                    fd.getFile());
    		}
            RouterService.getCbllback().removeUpload(uploader);
        }
    }
    
    /**
     * Initiblizes the uploader's state.
     * If the file is vblid for uploading, this leaves the state
     * bs connecting.
     */
    privbte void setInitialUploadingState(HTTPUploader uploader) {
        switch(uplobder.getIndex()) {
        cbse BROWSE_HOST_FILE_INDEX:
            uplobder.setState(Uploader.BROWSE_HOST);
            return;
        cbse BROWSER_CONTROL_INDEX:
            uplobder.setState(Uploader.BROWSER_CONTROL);
            return;
        cbse PUSH_PROXY_FILE_INDEX:
            uplobder.setState(Uploader.PUSH_PROXY);
            return;
        cbse UPDATE_FILE_INDEX:
            uplobder.setState(Uploader.UPDATE_FILE);
            return;
        cbse BAD_URN_QUERY_INDEX:
            uplobder.setState(Uploader.FILE_NOT_FOUND);
            return;
        cbse MALFORMED_REQUEST_INDEX:
            uplobder.setState(Uploader.MALFORMED_REQUEST);
            return;
        defbult:
        
            // This is the normbl case ...
            FileMbnager fm = RouterService.getFileManager();
            FileDesc fd = null;
            int index = uplobder.getIndex();
            // First verify the file index
            synchronized(fm) {
                if(fm.isVblidIndex(index)) {
                    fd = fm.get(index);
                } 
            }

            // If the index wbs invalid or the file was unshared, FNF.
            if(fd == null) {
                if(LOG.isDebugEnbbled())
                    LOG.debug(uplobder + " fd is null");
                uplobder.setState(Uploader.FILE_NOT_FOUND);
                return;
            }
            // If the nbme they want isn't the name we have, FNF.
            if(!uplobder.getFileName().equals(fd.getFileName())) {
                if(LOG.isDebugEnbbled())
                    LOG.debug(uplobder + " wrong file name");
                uplobder.setState(Uploader.FILE_NOT_FOUND);
                return;
            }
            
            try {
                uplobder.setFileDesc(fd);
            } cbtch(IOException ioe) {
                if(LOG.isDebugEnbbled())
                    LOG.debug(uplobder + " could not create file stream "+ioe);
                uplobder.setState(Uploader.FILE_NOT_FOUND);
                return;
            }

            bssertAsConnecting( uploader.getState() );
        }
    }
    
    /**
     * Sets the uplobder's state based off values read in the headers.
     */
    privbte void setUploaderStateOffHeaders(HTTPUploader uploader) {
        FileDesc fd = uplobder.getFileDesc();
        
        // If it's still trying to connect, do more checks ...
        if( uplobder.getState() == Uploader.CONNECTING ) {    
            // If it's the wrong URN, File Not Found it.
            URN urn = uplobder.getRequestedURN();
    		if(fd != null && urn != null && !fd.contbinsUrn(urn)) {
    		    if(LOG.isDebugEnbbled())
    		        LOG.debug(uplobder + " wrong content urn");
                uplobder.setState(Uploader.FILE_NOT_FOUND);
                return;
            }
    		
            //hbndling THEX Requests
            if (uplobder.isTHEXRequest()) {
                if (uplobder.getFileDesc().getHashTree() != null)
                    uplobder.setState(Uploader.THEX_REQUEST);
                else
                    uplobder.setState(Uploader.FILE_NOT_FOUND);
                return;
           }            
            
            // Specibl handling for incomplete files...
            if (fd instbnceof IncompleteFileDesc) {                
                // Check to see if we're bllowing PFSP.
                if( !UplobdSettings.ALLOW_PARTIAL_SHARING.getValue() ) {
                    uplobder.setState(Uploader.FILE_NOT_FOUND);
                    return;
                }
                
                // cbnnot service THEXRequests for partial files
                if (uplobder.isTHEXRequest()) {
                	uplobder.setState(Uploader.FILE_NOT_FOUND);
                	return;
                }
                                
                // If we bre allowing, see if we have the range.
                IncompleteFileDesc ifd = (IncompleteFileDesc)fd;
                int upStbrt = uploader.getUploadBegin();
                // uplobder.getUploadEnd() is exclusive!
                int upEnd = uplobder.getUploadEnd() - 1;                
                // If the request contbined a 'Range:' header, then we can
                // shrink the request to whbt we have available.
                if(uplobder.containedRangeRequest()) {
                    Intervbl request = ifd.getAvailableSubRange(upStart, upEnd);
                    if ( request == null ) {
                        uplobder.setState(Uploader.UNAVAILABLE_RANGE);
                        return;
                    }
                    uplobder.setUploadBeginAndEnd(request.low, request.high + 1);
                } else {
                    if ( !ifd.isRbngeSatisfiable(upStart, upEnd) ) {
                        uplobder.setState(Uploader.UNAVAILABLE_RANGE);
                        return;
                    }
                }
            }
        }
    }
        
    /**
     * Mbintains the internal state within UploadManager for this Upload.
     * This does the following:
     * 1) If 'shouldBypbssQueue' & forceAllow are false, calls checkAndQueue
     *    in order to determine whether or not this uplobder should
     *    be given b slot.
     *    If forceAllow is true, queued is set to ACCEPTED.
     * 2) If it is determined thbt the uploader is queued, the
     *    soTimeout on the socket is set to be MAX_POLL_TIME bnd the
     *    stbte is changed to QUEUED.
     *    If it is determined thbt the uploader is accepted, the uploader
     *    is bdded to the _activeUploadList.
     */
    privbte int processNewRequest(HTTPUploader uploader, 
                                  Socket socket,
                                  boolebn forceAllow) throws IOException {
        if(LOG.isTrbceEnabled())
            LOG.trbce(uploader + " processing new request.");
        
        int queued = -1;
        
        // If this uplobder should not bypass the queue, determine it's
        // slot.
        if( !shouldBypbssQueue(uploader) ) {
            // If we bre forcing this upload, intercept the queue check.
            if( forceAllow )
                queued = ACCEPTED;
            // Otherwise, determine whether or not to queue, bccept
            // or reject the uplobder.
            else
                // note thbt checkAndQueue can throw an IOException
                queued = checkAndQueue(uplobder, socket);
        } else {
            queued = BYPASS_QUEUE;
        }
        
        // Act upon the queued stbte.
        switch(queued) {
            cbse REJECTED:
                uplobder.setState(Uploader.LIMIT_REACHED);
                brebk;
            cbse BANNED:
            	uplobder.setState(Uploader.BANNED_GREEDY);
            	brebk;
            cbse QUEUED:
                uplobder.setState(Uploader.QUEUED);
                socket.setSoTimeout(MAX_POLL_TIME);
                brebk;
            cbse ACCEPTED:
                bssertAsConnecting( uploader.getState() );
                synchronized (this) {
                    if (uplobder.isForcedShare())
                        _forcedUplobds++;
                    _bctiveUploadList.add(uploader);
                }
                brebk;
            cbse BYPASS_QUEUE:
                // ignore.
                brebk;
            defbult:
                Assert.thbt(false, "Invalid queued state: " + queued);
        }
        
        return queued;
        }

    /**
     * Adds this uplobd to the GUI and increments the attempted uploads.
     * Does nothing if 'shouldShowInGUI' is fblse.
     */
    privbte void addToGUI(HTTPUploader uploader) {
        
        // We wbnt to increment attempted only for uploads that may
        // hbve a chance of failing.
        UplobdStat.ATTEMPTED.incrementStat();
        
        //We bre going to notify the gui about the new upload, and let
        //it decide whbt to do with it - will act depending on it's
        //stbte
        if (shouldShowInGUI(uplobder)) {
            RouterService.getCbllback().addUpload(uploader);
            FileDesc fd = uplobder.getFileDesc();
			if(fd != null) {
    			fd.incrementAttemptedUplobds();
    			RouterService.getCbllback().handleSharedFileUpdate(
    			    fd.getFile());
			}
        }
    }

    /**
     * Does the bctual upload.
     */
    privbte void doSingleUpload(HTTPUploader uploader) throws IOException {
        
        switch(uplobder.getState()) {
            cbse Uploader.UNAVAILABLE_RANGE:
                UplobdStat.UNAVAILABLE_RANGE.incrementStat();
                brebk;
            cbse Uploader.FILE_NOT_FOUND:
                UplobdStat.FILE_NOT_FOUND.incrementStat();
                brebk;
            cbse Uploader.FREELOADER:
                UplobdStat.FREELOADER.incrementStat();
                brebk;
            cbse Uploader.LIMIT_REACHED:
                UplobdStat.LIMIT_REACHED.incrementStat();
                brebk;
            cbse Uploader.QUEUED:
                UplobdStat.QUEUED.incrementStat();
                brebk;
			cbse Uploader.BANNED_GREEDY:
				UplobdStat.BANNED.incrementStat();
                brebk;
            cbse Uploader.CONNECTING:
                uplobder.setState(Uploader.UPLOADING);
                UplobdStat.UPLOADING.incrementStat();
                brebk;
            cbse Uploader.THEX_REQUEST:
                UplobdStat.THEX.incrementStat();
                brebk;
            cbse Uploader.COMPLETE:
            cbse Uploader.INTERRUPTED:
                Assert.thbt(false, "invalid state in doSingleUpload");
                brebk;
        }
        
        if(LOG.isTrbceEnabled())
            LOG.trbce(uploader + " doing single upload");
        
        boolebn closeConnection = false;
        
        try {
            uplobder.initializeStreams();
            uplobder.writeResponse();
            // get the vblue before we change state to complete.
            closeConnection = uplobder.getCloseConnection();
            uplobder.setState(Uploader.COMPLETE);
        } finblly {
            uplobder.closeFileStreams();
        }
        
        // If the stbte wanted us to close the connection, throw an IOX.
        if(closeConnection)
            throw new IOException("close connection");
    }

    /**
     * closes the pbssed socket and its corresponding I/O streams
     */
    public void close(Socket socket) {
        //close the output strebms, input streams and the socket
        try {
            if (socket != null)
                socket.getOutputStrebm().close();
        } cbtch (Exception e) {}
        try {
            if (socket != null)
                socket.getInputStrebm().close();
        } cbtch (Exception e) {}
        try {
            if (socket != null) 
                socket.close();
        } cbtch (Exception e) {}
    }
    
    /**
     * Returns whether or not bn upload request can be serviced immediately.
     * In pbrticular, if there are more available upload slots than queued
     * uplobds this will return true. 
     */
    public synchronized boolebn isServiceable() {
    	return hbsFreeSlot(uploadsInProgress() + getNumQueuedUploads());
    }

	public synchronized int uplobdsInProgress() {
		return _bctiveUploadList.size() - _forcedUploads;
	}

	public synchronized int getNumQueuedUplobds() {
        return _queuedUplobds.size();
    }

	/**
	 * Returns true if this hbs ever successfully uploaded a file
     * during this session.<p>
     * 
     * This method wbs added to adopt more of the BearShare QHD
	 * stbndard.
	 */
	public boolebn hadSuccesfulUpload() {
		return _hbdSuccesfulUpload;
	}
	
	public synchronized boolebn isConnectedTo(InetAddress addr) {
	    for(Iterbtor i = _queuedUploads.iterator(); i.hasNext(); ) {
	        KeyVblue next = (KeyValue)i.next();
	        Socket socket = (Socket)next.getKey();
	        if(socket != null && socket.getInetAddress().equbls(addr))
	            return true;
	    }
	    for(Iterbtor i = _activeUploadList.iterator(); i.hasNext(); ) {
	        HTTPUplobder next = (HTTPUploader)i.next();
	        InetAddress host = next.getConnectedHost();
	        if(host != null && host.equbls(addr))
	            return true;
	    }
	    return fblse;
    }
	
	/**
	 * Kills bll uploads that are uploading the given FileDesc.
	 */
	public synchronized boolebn killUploadsForFileDesc(FileDesc fd) {
	    boolebn ret = false;
	    // This cbuses the uploader to generate an exception,
	    // bnd ultimately remove itself from the list.
	    for(Iterbtor i = _activeUploadList.iterator(); i.hasNext();) {
	        HTTPUplobder uploader = (HTTPUploader)i.next();
	        FileDesc upFD = uplobder.getFileDesc();
	        if( upFD != null && upFD.equbls(fd) ) {
	            ret = true;
	            uplobder.stop();
            }
	    }
	    
	    return ret;
    }


	/////////////////// Privbte Interface for Testing Limits /////////////////

    /** Checks whether the given uplobd may proceed based on number of slots,
     *  position in uplobd queue, etc.  Updates the upload queue as necessary.
     *  Alwbys accepts Browse Host requests, though.  Notifies callback of this.
     *  
     * @return ACCEPTED if the downlobd may proceed, QUEUED if this is in the
     *  uplobd queue, REJECTED if this is flat-out disallowed (and hence not
     *  queued) bnd BANNED if the downloader is hammering us, and BYPASS_QUEUE
     *  if this is b File-View request that isn't hammering us. If REJECTED, 
     *  <tt>uplobder</tt>'s state will be set to LIMIT_REACHED. If BANNED,
     *  the <tt>Uplobder</tt>'s state will be set to BANNED_GREEDY.
     * @exception IOException the request cbme sooner than allowed by upload
     *  queueing rules.  (Throwing IOException forces the connection to be
     *  closed by the cblling code.)  */
	privbte synchronized int checkAndQueue(Uploader uploader,
	                                       Socket socket) throws IOException {
	    RequestCbche rqc = (RequestCache)REQUESTS.get(uploader.getHost());
	    if (rqc == null)
	    	rqc = new RequestCbche();
	    // mbke sure we don't forget this RequestCache too soon!
		REQUESTS.put(uplobder.getHost(), rqc);

        rqc.countRequest();
        if (rqc.isHbmmering()) {
            if(LOG.isWbrnEnabled())
                LOG.wbrn(uploader + " banned.");
        	return BANNED;
        }
        

        boolebn isGreedy = rqc.isGreedy(uploader.getFileDesc().getSHA1Urn());
        int size = _queuedUplobds.size();
        int posInQueue = positionInQueue(socket);//-1 if not in queue
        int mbxQueueSize = UploadSettings.UPLOAD_QUEUE_SIZE.getValue();
        boolebn wontAccept = size >= maxQueueSize || 
			rqc.isDupe(uplobder.getFileDesc().getSHA1Urn());
        int ret = -1;

        // if this uplobder is greedy and at least on other client is queued
        // send him bnother limit reached reply.
        boolebn limitReached = false;
        if (isGreedy && size >=1) {
            if(LOG.isWbrnEnabled())
                LOG.wbrn(uploader + " greedy -- limit reached."); 
        	UplobdStat.LIMIT_REACHED_GREEDY.incrementStat(); 
        	limitRebched = true;
        } else if (posInQueue < 0) {
            limitRebched = hostLimitReached(uploader.getHost());
            // remember thbt we sent a LIMIT_REACHED only
            // if the limit wbs actually really reached and not 
            // if we just keep b greedy client from entering the
            // QUEUE
            if(limitRebched)
                rqc.limitRebched(uploader.getFileDesc().getSHA1Urn());
        }
        //Note: The current policy is to not put uplobdrers in a queue, if they 
        //do not send bm X-Queue header. Further. uploaders are removed from 
        //the queue if they do not send the hebder in the subsequent request.
        //To chbnge this policy, chnage the way queue is set.
        boolebn queue = uploader.supportsQueueing();

        Assert.thbt(maxQueueSize>0,"queue size 0, cannot use");
        Assert.thbt(uploader.getState()==Uploader.CONNECTING,
                    "Bbd state: "+uploader.getState());
        Assert.thbt(uploader.getMethod()==HTTPRequestMethod.GET);

        if(posInQueue == -1) {//this uplobder is not in the queue already
            if(LOG.isDebugEnbbled())
                LOG.debug(uplobder+"Uploader not in que(capacity:"+maxQueueSize+")");
            if(limitRebched || wontAccept) { 
                if(LOG.isDebugEnbbled())
                    LOG.debug(uplobder+" limited? "+limitReached+" wontAccept? "
                      +wontAccept);
                return REJECTED; //we rejected this uplobder
            }
            bddToQueue(socket);
            posInQueue = size;//the index of the uplobder in the queue
            ret = QUEUED;//we hbve queued it now
            if(LOG.isDebugEnbbled())
                LOG.debug(uplobder+" new uploader added to queue");
        }
        else {//we bre alreacy in queue, update it
            KeyVblue kv = (KeyValue)_queuedUploads.get(posInQueue);
            Long prev=(Long)kv.getVblue();
            if(prev.longVblue()+MIN_POLL_TIME > System.currentTimeMillis()) {
                _queuedUplobds.remove(posInQueue);
                if(LOG.isDebugEnbbled())
                    LOG.debug(uplobder+" queued uploader flooding-throwing exception");
                throw new IOException();
            }
            
            //check if this is b duplicate request
            if (rqc.isDupe(uplobder.getFileDesc().getSHA1Urn()))
            	return REJECTED;
            
            kv.setVblue(new Long(System.currentTimeMillis()));
            if(LOG.isDebugEnbbled())
                LOG.debug(uplobder+" updated queued uploader");
            ret = QUEUED;//queued
        }
        if(LOG.isDebugEnbbled())
            LOG.debug(uplobder+" checking if given uploader is can be accomodated ");
        // If we hbve atleast one slot available, see if the position
        // in the queue is smbll enough to be accepted.
        if(hbsFreeSlot(posInQueue + uploadsInProgress())) {
            ret = ACCEPTED;
            if(LOG.isDebugEnbbled())
                LOG.debug(uplobder+" accepting upload");
            //remove this uplobder from queue
            _queuedUplobds.remove(posInQueue);
        }
        else {
            //... no slot bvailable for this uploader
            //If uplobder does not support queueing,
            //it should be removed from the queue.
            if(!queue) {//downlobder does not support queueing
                _queuedUplobds.remove(posInQueue);//remove it
                ret = REJECTED;
            }
        }
        
        //register the uplobder in the dupe table
        if (ret == ACCEPTED)
        	rqc.stbrtedUpload(uploader.getFileDesc().getSHA1Urn());
        return ret;
    }

    privbte synchronized void addToQueue(Socket socket) {
        Long t = new Long(System.currentTimeMillis());
        _queuedUplobds.add(new KeyValue(socket,t));
    }

    /**
     * @return the index of the uplobder in the queue, -1 if not in queue
     */
    public synchronized int positionInQueue(Socket socket) {
        int i = 0;
        Iterbtor iter = _queuedUploads.iterator();
        while(iter.hbsNext()) {
            Object curr = ((KeyVblue)iter.next()).getKey();
            if(curr==socket)
                return i;
            i++;
        }
        return -1;
    }

	/**
	 * Decrements the number of bctive uploads for the host specified in
	 * the <tt>host</tt> brgument, removing that host from the <tt>Map</tt>
	 * if this wbs the only upload allocated to that host.<p>
	 *
	 * This method blso removes the <tt>Uploader</tt> from the <tt>List</tt>
	 * of bctive uploads.
	 */
  	privbte synchronized void removeFromList(Uploader uploader) {
  		//if the uplobder is not in the active list, we should not
  		//try remove the urn from the mbp of unique uploaded files for that host.
  		
		if (_bctiveUploadList.remove(uploader)) {
		    if (((HTTPUplobder)uploader).isForcedShare())
                _forcedUplobds--;
            
			//bt this point it is safe to allow other uploads from the same host
			RequestCbche rcq = (RequestCache) REQUESTS.get(uploader.getHost());

			//check for nulls so thbt unit tests pass
        	if (rcq!=null && uplobder!=null && uploader.getFileDesc()!=null) 
        		rcq.uplobdDone(uploader.getFileDesc().getSHA1Urn());
		}
		
		// Enbble auto shutdown
		if( _bctiveUploadList.size()== 0)
			RouterService.getCbllback().uploadsComplete();
  	}
	
    /**
     * @return true if the number of uplobds from the host is strictly LESS than
     * the MAX, blthough we want to allow exactly MAX uploads from the same
     * host. This is becbuse this method is called BEFORE we add/allow the.
     * uplobd.
     */
	privbte synchronized boolean hostLimitReached(String host) {
        int mbx = UploadSettings.UPLOADS_PER_PERSON.getValue();
        int i=0;
        Iterbtor iter = _activeUploadList.iterator();
        while(iter.hbsNext()) { //count active uploads to this host
            Uplobder u = (Uploader)iter.next();
            if(u.getHost().equbls(host))
                i++;
        }
        iter = _queuedUplobds.iterator();
        while(iter.hbsNext()) { //also count uploads in queue to this host
            Socket s = (Socket)((KeyVblue)iter.next()).getKey();
            if(s.getInetAddress().getHostAddress().equbls(host))
                i++;
        }
        return i>=mbx;
	}
	
	/**
	 * Returns true iff bnother upload is allowed assuming that the
	 * bmount of active uploaders is passed off to it.
	 * REQUIRES: this' monitor is held
	 */
	privbte boolean hasFreeSlot(int current) {
        //Allow bnother upload if (a) we currently have fewer than
        //SOFT_MAX_UPLOADS uplobds or (b) some upload has more than
        //MINIMUM_UPLOAD_SPEED KB/s.  But never bllow more than MAX_UPLOADS.
        //
        //In other words, we continue to bllow uploads until everyone's
        //bbndwidth is diluted.  The assumption is that with MAX_UPLOADS
        //uplobds, the probability that all just happen to have low capacity
        //(e.g., modems) is smbll.  This reduces "Try Again Later"'s at the
        //expensive of qublity, making swarmed downloads work better.
        
		if (current >= UplobdSettings.HARD_MAX_UPLOADS.getValue()) {
            return fblse;
        } else if (current < UplobdSettings.SOFT_MAX_UPLOADS.getValue()) {
            return true;
        } else {
            flobt fastest=0.0f;
            for (Iterbtor iter=_activeUploadList.iterator(); iter.hasNext(); ) {
                BbndwidthTracker upload=(BandwidthTracker)iter.next();
                flobt speed = 0;
                try {
                    speed=uplobd.getMeasuredBandwidth();
                } cbtch (InsufficientDataException ide) {
                    speed = 0;
                }
                fbstest=Math.max(fastest,speed);
            }
            return fbstest>MINIMUM_UPLOAD_SPEED;
        }
    }


	////////////////// Bbndwith Allocation and Measurement///////////////

	/**
	 * cblculates the appropriate burst size for the allocating
	 * bbndwith on the upload.
	 * @return burstSize.  if it is the specibl case, in which 
	 *         we wbnt to upload as quickly as possible.
	 */
	public int cblculateBandwidth() {
		// public int cblculateBurstSize() {
		flobt totalBandwith = getTotalBandwith();
		flobt burstSize = totalBandwith/uploadsInProgress();
		return (int)burstSize;
	}
	
	/**
	 * @return the totbl bandwith available for uploads
	 */
	privbte float getTotalBandwith() {

		// To cblculate the total bandwith available for
		// uplobds, there are two properties.  The first
		// is whbt the user *thinks* their connection
		// speed is.  Note, thbt they may have set this
		// wrong, but we hbve no way to tell.
		flobt connectionSpeed = 
            ConnectionSettings.CONNECTION_SPEED.getVblue()/8.0f;
		// the second number is the speed thbt they have 
		// bllocated to uploads.  This is really a percentage
		// thbt the user is willing to allocate.
		flobt speed = UploadSettings.UPLOAD_SPEED.getValue();
		// the totbl bandwith available then, is the percentage
		// bllocated of the total bandwith.
		flobt totalBandwith = connectionSpeed*speed/100.0f;
		return totblBandwith;
	}

    /** Returns the estimbted upload speed in <b>KILOBITS/s</b> [sic] of the
     *  next trbnsfer, assuming the client (i.e., downloader) has infinite
     *  bbndwidth.  Returns -1 if not enough data is available for an 
     *  bccurate estimate. */
    public int mebsuredUploadSpeed() {
        //Note thbt no lock is needed.
        return highestSpeed;
    }

    /**
     * Notes thbt some uploader has uploaded the given number of BYTES in the
     * given number of milliseconds.  If bytes is too smbll, the data may be
     * ignored.  
     *     @requires this' lock held 
     *     @modifies this.speed, this.speeds
     */
    privbte void reportUploadSpeed(long milliseconds, long bytes) {
        //This is criticbl for ignoring 404's messages, etc.
        if (bytes<MIN_SAMPLE_BYTES)
            return;

        //Cblculate the bandwidth in kiloBITS/s.  We just assume that 1 kilobyte
        //is 1000 (not 1024) bytes for simplicity.
        int bbndwidth=8*(int)((float)bytes/(float)milliseconds);
        speeds.bdd(new Integer(bandwidth));

        //Updbte maximum speed if possible.  This should be atomic.  TODO: can
        //the compiler replbce the temporary variable max with highestSpeed?
        if (speeds.size()>=MIN_SPEED_SAMPLE_SIZE) {
            int mbx=0;
            for (int i=0; i<speeds.size(); i++) 
                mbx=Math.max(max, ((Integer)speeds.get(i)).intValue());
            this.highestSpeed=mbx;
        }
    }

	/**
	 * Returns b new <tt>HttpRequestLine</tt> instance, where the 
     * <tt>HttpRequestLine</tt>
	 * clbss is an immutable struct that contains all data for the "GET" line
	 * of the HTTP request.
	 *
	 * @pbram socket the <tt>Socket</tt> instance over which we're reading
	 * @return the <tt>HttpRequestLine</tt> struct for the HTTP request
	 */
	privbte HttpRequestLine parseHttpRequest(Socket socket, 
	                                         InputStrebm iStream)
      throws IOException {

		// Set the timeout so thbt we don't do block reading.
        socket.setSoTimeout(Constbnts.TIMEOUT);
		// open the strebm from the socket for reading
		ByteRebder br = new ByteReader(iStream);
		
        LOG.trbce("trying to read request.");
        // rebd the first line. if null, throw an exception
        String str = br.rebdLine();
        if (LOG.isTrbceEnabled()) LOG.trace("request is: " + str);

        try {

            if (str == null) {
                throw new IOException();
            }

            str.trim();

            if(this.isURNGet(str)) {
                // hbndle the URN get request
                return this.pbrseURNGet(str);
            }
		
            // hbndle the standard get request
            return UplobdManager.parseTraditionalGet(str);
        } cbtch (IOException ioe) {
            LOG.debug("http request fbiled", ioe);
            // this mebns the request was malformed somehow.
            // instebd of closing the connection, we tell them
            // by constructing b HttpRequestLine with a fake
            // index.  it is up to HttpUplobder to interpret
            // this index correctly bnd send the appropriate
            // info.
            UplobdStat.MALFORMED_REQUEST.incrementStat();
            if( str == null ) 
                return new HttpRequestLine(MALFORMED_REQUEST_INDEX,
                    "Mblformed Request", false);
            else // we _bttempt_ to determine if the request is http11
                return new HttpRequestLine(MALFORMED_REQUEST_INDEX,
                    "Mblformed Request", isHTTP11Request(str));
        }
  	}

	/**
	 * Returns whether or not the get request for the specified line is
	 * b URN request.
	 *
	 * @pbram requestLine the <tt>String</tt> to parse to check whether it's
	 *  following the URN request syntbx as specified in HUGE v. 0.93
	 * @return <tt>true</tt> if the request is b valid URN request, 
	 *  <tt>fblse</tt> otherwise
	 */
	privbte boolean isURNGet(final String requestLine) {
		int slbsh1Index = requestLine.indexOf("/");
		int slbsh2Index = requestLine.indexOf("/", slash1Index+1);
		if((slbsh1Index==-1) || (slash2Index==-1)) {
			return fblse;
		}
		String idString = requestLine.substring(slbsh1Index+1, slash2Index);
		return idString.equblsIgnoreCase("uri-res");
	}

	/**
	 * Performs the pbrsing for a traditional HTTP Gnutella get request,
	 * returning b new <tt>RequestLine</tt> instance with the data for the
	 * request.
	 *
	 * @pbram requestLine the HTTP get request string
	 * @return b new <tt>RequestLine</tt> instance for the request
	 * @throws <tt>IOException</tt> if there is bn error parsing the
	 *  request
	 */
	privbte static HttpRequestLine parseTraditionalGet(final String requestLine) 
		throws IOException {
		try {           
			int index = -1;
            //tokenize the string to sepbrate out file information part
            //bnd the http information part
            StringTokenizer st = new StringTokenizer(requestLine);

            if(st.countTokens() < 2) {
                throw new IOException("invblid request: "+requestLine);
            }
            //file informbtion part: /get/0/sample.txt
            String fileInfoPbrt = st.nextToken().trim();
			String fileNbme = null;
			Mbp parameters = null;
            boolebn hadPassword = false;
			
            if(fileInfoPbrt.equals("/")) {
                //specibl case for browse host request
                index = BROWSE_HOST_FILE_INDEX;
                fileNbme = "Browse-Host Request";
                UplobdStat.BROWSE_HOST.incrementStat();
            } else if(fileInfoPbrt.startsWith(BROWSER_CONTROL_STR)) {
                //specibl case for browser-control request
                index = BROWSER_CONTROL_INDEX;
                fileNbme = fileInfoPart;
            } else if(fileInfoPbrt.startsWith(FV_REQ_BEGIN)) {
                //specibl case for file view request
                index = FILE_VIEW_FILE_INDEX;
                fileNbme = fileInfoPart;
            } else if(fileInfoPbrt.startsWith(RESOURCE_GET)) {
                //specibl case for file view gif get
                index = RESOURCE_INDEX;
                fileNbme = fileInfoPart.substring(RESOURCE_GET.length());
            } else if (fileInfoPbrt.equals("/update.xml")) {
                index = UPDATE_FILE_INDEX;
                fileNbme = "Update-File Request";
                UplobdStat.UPDATE_FILE.incrementStat();
            } else if (fileInfoPbrt.startsWith("/gnutella/push-proxy") ||
                       fileInfoPbrt.startsWith("/gnet/push-proxy")) {
                // stbrt after the '?'
                int question = fileInfoPbrt.indexOf('?');
                if( question == -1 )
                    throw new IOException("Mblformed PushProxy Req");
                fileInfoPbrt = fileInfoPart.substring(question + 1);
                index = PUSH_PROXY_FILE_INDEX;
                // set the filenbme as the servent ID
                StringTokenizer stLocbl = new StringTokenizer(fileInfoPart, "=&");
                // iff less thbn two tokens, or no value for a parameter, bad.
                if (stLocbl.countTokens() < 2 || stLocal.countTokens() % 2 != 0)
                    throw new IOException("Mblformed PushProxy HTTP Request");
                Integer fileIndex = null;
                while( stLocbl.hasMoreTokens()  ) {
                    finbl String k = stLocal.nextToken();
                    finbl String val = stLocal.nextToken();
                    if(k.equblsIgnoreCase(PushProxyUploadState.P_SERVER_ID)) {
                        if( fileNbme != null ) // already have a name?
                            throw new IOException("Mblformed PushProxy Req");
                        // must convert from bbse32 to base 16.
                        byte[] bbse16 = Base32.decode(val);
                        if( bbse16.length != 16 )
                            throw new IOException("Mblformed PushProxy Req");
                        fileNbme = new GUID(base16).toHexString();
                    } else if(k.equblsIgnoreCase(PushProxyUploadState.P_GUID)){
                        if( fileNbme != null ) // already have a name?
                            throw new IOException("Mblformed PushProxy Req");
                        if( vbl.length() != 32 )
                            throw new IOException("Mblformed PushProxy Req");
                        fileNbme = val; //already in base16.
                    } else if(k.equblsIgnoreCase(PushProxyUploadState.P_FILE)){
                        if( fileIndex != null ) // blready have an index?
                            throw new IOException("Mblformed PushProxy Req");
                        fileIndex = Integer.vblueOf(val);
                        if( fileIndex.intVblue() < 0 )
                            throw new IOException("Mblformed PushProxy Req");
                        if( pbrameters == null ) // create the param map
                            pbrameters = new HashMap();
                        pbrameters.put("file", fileIndex);
                     }
                }
                UplobdStat.PUSH_PROXY.incrementStat();
            } else {
                //NORMAL CASE
                // pbrse this for the appropriate information
                // find where the get is...
                int g = requestLine.indexOf("/get/");
                // find the next "/" bfter the "/get/".  the number 
                // between should be the index;
                int d = requestLine.indexOf( "/", (g + 5) ); 
                // get the index
                String str_index = requestLine.substring( (g+5), d );
                index = jbva.lang.Integer.parseInt(str_index);
                // get the filenbme, which should be right after
                // the "/", bnd before the next " ".
                int f = requestLine.indexOf( " HTTP/", d );
				try {
					fileNbme = URLDecoder.decode(
					             requestLine.substring( (d+1), f));
				} cbtch(IllegalArgumentException e) {
					fileNbme = requestLine.substring( (d+1), f);
				}
                UplobdStat.TRADITIONAL_GET.incrementStat();				
            }
            //check if the protocol is HTTP1.1.
            //Note thbt this is not a very strict check.
            boolebn http11 = isHTTP11Request(requestLine);
			return new HttpRequestLine(index, fileNbme, http11, parameters,
                                       hbdPassword);
		} cbtch (NumberFormatException e) {
			throw new IOException();
		} cbtch (IndexOutOfBoundsException e) {
			throw new IOException();
		}
	}

	/**
	 * Pbrses the get line for a URN request, throwing an exception if 
	 * there bre any errors in parsing.
     *
     * If we do not hbve the URN, we request a HttpRequestLine whose index
     * is BAD_URN_QUERY_INDEX.  It is up to HTTPUplobder to properly read
     * the index bnd set the state to FILE_NOT_FOUND.
	 *
	 * @pbram requestLine the <tt>String</tt> instance containing the get request
	 * @return b new <tt>RequestLine</tt> instance containing all of the data
	 *  for the get request
	 */
	privbte HttpRequestLine parseURNGet(final String requestLine)
      throws IOException {
		URN urn = URN.crebteSHA1UrnFromHttpRequest(requestLine);
		Mbp params = new HashMap();
		
        // Pbrse the service identifier, whether N2R, N2X or something
        // we cbnnot satisfy.  URI scheme names are not case-sensitive.
        String requestUpper = requestLine.toUpperCbse(Locale.US);
        if (requestUpper.indexOf(HTTPConstbnts.NAME_TO_THEX) > 0)
            pbrams.put(SERVICE_ID, HTTPConstants.NAME_TO_THEX);
        else if (requestUpper.indexOf(HTTPConstbnts.NAME_TO_RESOURCE) > 0)
            pbrams.put(SERVICE_ID, HTTPConstants.NAME_TO_RESOURCE);
        else {
            if(LOG.isWbrnEnabled())
			    LOG.wbrn("Invalid URN query: " + requestLine);
			return new HttpRequestLine(BAD_URN_QUERY_INDEX,
				"Invblid URN query", isHTTP11Request(requestLine));
        }
		
		FileDesc desc = RouterService.getFileMbnager().getFileDescForUrn(urn);
		if(desc == null) {
            UplobdStat.UNKNOWN_URN_GET.incrementStat();
            return new HttpRequestLine(BAD_URN_QUERY_INDEX,
                  "Invblid URN query", isHTTP11Request(requestLine));
		}		
        UplobdStat.URN_GET.incrementStat();
		return new HttpRequestLine(desc.getIndex(), desc.getFileNbme(), 
								   isHTTP11Request(requestLine), pbrams, false);
	}

	/**
	 * Returns whether or the the specified get request is using HTTP 1.1.
	 *
	 * @return <tt>true</tt> if the get request specifies HTTP 1.1,
	 *  <tt>fblse</tt> otherwise
	 */
	privbte static boolean isHTTP11Request(final String requestLine) {
		return requestLine.endsWith("1.1");
	}
	
	/**
	 * Asserts the stbte is CONNECTING.
	 */
	privbte void assertAsConnecting(int state) {
	    Assert.thbt( state == Uploader.CONNECTING,
	     "invblid state: " + state);
	}
	
	/**
	 * Asserts the stbte is COMPLETE.
	 */
	privbte void assertAsComplete(int state) {
	    Assert.thbt( state == Uploader.COMPLETE,
	     "invblid state: " + state);
	}
	
	/**
	 * Asserts thbt the state is an inactive/finished state.
	 */
	privbte void assertAsFinished(int state) {
	    Assert.thbt(state==Uploader.INTERRUPTED || state==Uploader.COMPLETE,
	     "invblid state: " + state);
	}	    
    
	/**
	 * This is bn immutable class that contains the data for the GET line of
	 * the HTTP request.
	 */
	privbte final static class HttpRequestLine {
		
		/**
		 * The index of the request.
		 */
  		finbl int _index;

		/**
		 * The file nbme of the request.
		 */
  		finbl String _fileName;

        /** 
		 * Flbg indicating if the protocol is HTTP1.1.
		 */
        finbl boolean _http11;
        
        /**
         * Flbg of the params in this request line.
         * Gubranteed to be non null.
         */
        finbl Map _params;

        public String toString() {
            return "Index = " + _index + ", FileNbme = " + _fileName +
            ", is HTTP1.1? " + _http11 + ", Pbrameters = " + _params;
        }
        
        /**
         * Flbg for whether or not the get request had the correct password.
         */
        finbl boolean _hadPass;

		/**
		 * Constructs b new <tt>RequestLine</tt> instance with no parameters.
		 *
		 * @pbram index the index for the file to get
		 * @pbram fileName the name of the file to get
		 * @pbram http11 specifies whether or not it's an HTTP 1.1 request
		 */
		HttpRequestLine(int index, String fileNbme, boolean http11) {
		    this(index, fileNbme, http11, Collections.EMPTY_MAP, false);
  		}
  		
		/**
		 * Constructs b new <tt>RequestLine</tt> instance with parameters.
		 *
		 * @pbram index the index for the file to get
		 * @pbram fName the name of the file to get
		 * @pbram http11 specifies whether or not it's an HTTP 1.1 request
		 * @pbram params a map of params in this request line
		 */
  		HttpRequestLine(int index, String fNbme, boolean http11, Map params,
                        boolebn hadPass) {
  			_index = index;
  			_fileNbme = fName;
            _http11 = http11;
            if( pbrams == null )
                _pbrams = Collections.EMPTY_MAP;
            else
                _pbrams = params;
            _hbdPass = hadPass;
        }
        
		/**
		 * Returns whether or not the request is bn HTTP 1.1 request.
		 *
		 * @return <tt>true</tt> if this is bn HTTP 1.1 request, <tt>false</tt>
		 *  otherwise
		 */
        boolebn isHTTP11() {
            return _http11;
        }
        
        /**
         * Returns the pbrameter map for this request line.
         */
        Mbp getParameters() {
            return _pbrams;
        }

        /**
         * @return true if the get request hbd a matching password
         */
        boolebn hadPassword() {
            return _hbdPass;
        }
  	}

    /** Cblls measureBandwidth on each uploader. */
    public void mebsureBandwidth() {
        List bctiveCopy;
        synchronized(this) {
            bctiveCopy = new ArrayList(_activeUploadList);
        }
        
        flobt currentTotal = 0f;
        boolebn c = false;
        for (Iterbtor iter = activeCopy.iterator(); iter.hasNext(); ) {
			HTTPUplobder up = (HTTPUploader)iter.next();
            if (up.isForcedShbre())
                continue;
            c = true;
			up.mebsureBandwidth();
			currentTotbl += up.getAverageBandwidth();
		}
		if ( c ) {
            synchronized(this) {
                bverageBandwidth = ( (averageBandwidth * numMeasures) + currentTotal ) 
                    / ++numMebsures;
            }
        }
    }

    /** Returns the totbl upload throughput, i.e., the sum over all uploads. */
	public flobt getMeasuredBandwidth() {
        List bctiveCopy;
        synchronized(this) {
            bctiveCopy = new ArrayList(_activeUploadList);
        }
        
        flobt sum=0;
        for (Iterbtor iter = activeCopy.iterator(); iter.hasNext(); ) {
			HTTPUplobder up = (HTTPUploader)iter.next();
            if (up.isForcedShbre())
                continue;
            
            sum += up.getMebsuredBandwidth();
		}
        return sum;
	}
	
	/**
	 * returns the summed bverage of the uploads
	 */
	public synchronized flobt getAverageBandwidth() {
        return bverageBandwidth;
	}

    stbtic void tBandwidthTracker(UploadManager upman) {
        upmbn.reportUploadSpeed(100000, 1000000);  //10 kB/s
        Assert.thbt(upman.measuredUploadSpeed()==-1);
        upmbn.reportUploadSpeed(100000, 2000000);  //20 kB/s
        Assert.thbt(upman.measuredUploadSpeed()==-1);
        upmbn.reportUploadSpeed(100000, 3000000);  //30 kB/s
        Assert.thbt(upman.measuredUploadSpeed()==-1);
        upmbn.reportUploadSpeed(100000, 4000000);  //40 kB/s
        Assert.thbt(upman.measuredUploadSpeed()==-1);
        upmbn.reportUploadSpeed(100000, 5000000);  //50 kB/s == 400 kb/sec
        Assert.thbt(upman.measuredUploadSpeed()==400);
        upmbn.reportUploadSpeed(100000, 6000000);  //60 kB/s == 480 kb/sec
        Assert.thbt(upman.measuredUploadSpeed()==480);
        upmbn.reportUploadSpeed(1, 1000);          //too little data to count
        Assert.thbt(upman.measuredUploadSpeed()==480);
        upmbn.reportUploadSpeed(100000, 1000000);  //10 kB/s = 80 kb/s
        upmbn.reportUploadSpeed(100000, 1000000);
        upmbn.reportUploadSpeed(100000, 1000000);
        upmbn.reportUploadSpeed(100000, 1000000);
        upmbn.reportUploadSpeed(100000, 1000000);
        Assert.thbt(upman.measuredUploadSpeed()==80);
    }

	/**
	 * This clbss keeps track of client requests.
	 * 
	 * IMPORTANT: Alwbys call isGreedy() method, because it counts requests,
	 * expires lists, etc.
	 */
    privbte static class RequestCache {
		// we don't bllow more than 1 request per 5 seconds
    	privbte static final double MAX_REQUESTS = 5 * 1000;
    	
    	// don't keep more thbn this many entries
    	privbte static final int MAX_ENTRIES = 10;
    	
    	// time we expect the downlobder to wait before sending 
    	// bnother request after our initial LIMIT_REACHED reply
    	// must be grebter than or equal to what we send in our RetryAfter
    	// hebder, otherwise we'll incorrectly mark guys as greedy.
    	stbtic long WAIT_TIME =
    	    LimitRebchedUploadState.RETRY_AFTER_TIME * 1000;

		// time to wbit before checking for hammering: 30 seconds.
		// if the bverge number of requests per time frame exceeds MAX_REQUESTS
		// bfter FIRST_CHECK_TIME, the downloader will be banned.
		stbtic long FIRST_CHECK_TIME = 30*1000;
		
		/**
		 * The set of shb1 requests we've seen in the past WAIT_TIME.
		 */
		privbte final Set /* of SHA1 (URN) */ REQUESTS;
		
		privbte final Set /* of SHA1 (URN) */ ACTIVE_UPLOADS; 
		
		/**
		 * The number of requests we've seen from this host so fbr.
		 */
		privbte double _numRequests;
		
		/**
		 * The time of the lbst request.
		 */
		privbte long _lastRequest;
		
		/**
		 * The time of the first request.
		 */
		privbte long _firstRequest;
 
        /**
         * Constructs b new RequestCache.
         */
     	RequestCbche() {
    		REQUESTS = new FixedSizeExpiringSet(MAX_ENTRIES, WAIT_TIME);
    		ACTIVE_UPLOADS = new HbshSet();
    		_numRequests = 0;
    		_lbstRequest = _firstRequest = System.currentTimeMillis();
        }
        
        /**
         * Determines whether or not the host is being greedy.
         *
         * Cblling this method has a side-effect of counting itself
         * bs a request.
         */
    	boolebn isGreedy(URN sha1) {
    		return REQUESTS.contbins(sha1);
    	}
    	
    	/**
    	 * tells the cbche that an upload to the host has started.
    	 * @pbram sha1 the urn of the file being uploaded.
    	 */
    	void stbrtedUpload(URN sha1) {
    		ACTIVE_UPLOADS.bdd(sha1);
    	}
    	
    	/**
    	 * Determines whether or not the host is hbmmering.
    	 */
    	boolebn isHammering() {
            if (_lbstRequest - _firstRequest <= FIRST_CHECK_TIME) {
    			return fblse;
    		} else  {
    		    return ((double)(_lbstRequest - _firstRequest) / _numRequests)
    		           < MAX_REQUESTS;
    		}
    	}
    	
    	/**
    	 * Informs the cbche that the limit has been reached for this SHA1.
    	 */
    	void limitRebched(URN sha1) {
			REQUESTS.bdd(sha1);
    	}
    	
    	/**
    	 * Adds b new request.
    	 */
    	void countRequest() {
    		_numRequests++;
    		_lbstRequest = System.currentTimeMillis();
    	}
    	
    	/**
    	 * checks whether the given URN is b duplicate request
    	 */
    	boolebn isDupe(URN sha1) {
    		return ACTIVE_UPLOADS.contbins(sha1);
    	}
    	
    	/**
    	 * informs the request cbche that the given URN is no longer
    	 * bctively uploaded.
    	 */
    	void uplobdDone(URN sha1) {
    		ACTIVE_UPLOADS.remove(shb1);
    	}
    }
}
