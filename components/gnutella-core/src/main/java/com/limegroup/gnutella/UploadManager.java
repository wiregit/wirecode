package com.limegroup.gnutella;

import com.limegroup.gnutella.uploader.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.statistics.UploadStat;
import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;
import java.util.Date;
import com.limegroup.gnutella.util.URLDecoder;
import com.limegroup.gnutella.util.IOUtils;
import java.util.StringTokenizer;

/**
 * This class parses HTTP requests and delegates to <tt>HTTPUploader</tt>
 * to handle individual uploads.
 *
 * The state of HTTPUploader is maintained by this class.
 * HTTPUploader's state follows the following pattern:
 *                                                           \ /
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
public final class UploadManager implements BandwidthTracker {

    /** An enumeration of return values for queue checking. */
    private final int BYPASS_QUEUE = -1;
    private final int REJECTED = 0;    
    private final int QUEUED = 1;
    private final int ACCEPTED = 2;
    /** The min and max allowed times (in milliseconds) between requests by
     *  queued hosts. */
    public static final int MIN_POLL_TIME = 45000; //45 sec, same as Shareaza
    public static final int MAX_POLL_TIME = 120000; //120 sec, same as Shareaza

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
        debug(" accepting upload");
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

                HttpRequestLine line = parseHttpRequest(socket, iStream);

                debug(uploader + " successfully parsed request");
                
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
                    debug(uploader + " starting new file");
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
								    	        watchdog);
                }
                // Otherwise (we're continuing an uploader),
                // reinitialize the existing HTTPUploader.
                else {
                    debug(uploader + " continuing old file");
                    uploader.reinitialize(currentMethod);
                }
                
                assertAsConnecting( uploader.getState() );
        
                setInitialUploadingState(uploader);
                try {
                    uploader.readHeader(iStream);
                } catch(IOException ioe) {
                    uploader.setState(Uploader.INTERRUPTED);
                    throw ioe;
                }
                setUploaderStateOffHeaders(uploader);
                
                debug(uploader+" HTTPUploader created and read all headers");

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
                debug(uploader+" waiting for next request with socket ");
                int oldTimeout = socket.getSoTimeout();
                if(queued!=QUEUED)
                    socket.setSoTimeout(SettingsManager.instance().
                                        getPersistentHTTPConnectionTimeout());
                //dont read a word of size more than 4 
                //as we will handle only the next "HEAD" or "GET" request
                String word = IOUtils.readWord(
                    iStream, 4);
                debug(uploader+" next request arrived ");
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
            debug(uploader + " IOE thrown, closing socket");
        } finally {
            if( uploader != null )
                assertAsFinished( uploader.getState() );
            
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
            
            debug(uploader + " closing socket");
            //close the socket
            close(socket);
        }
    }
    
    /**
     * Determines whether or no this Uploader should be shown
     * in the GUI.
     */
    private boolean shouldShowInGUI(Uploader uploader) {
        return uploader.getIndex() != BROWSE_HOST_FILE_INDEX &&
               uploader.getIndex() != PUSH_PROXY_FILE_INDEX &&
               uploader.getIndex() != UPDATE_FILE_INDEX &&
               uploader.getIndex() != MALFORMED_REQUEST_INDEX &&
               uploader.getIndex() != BAD_URN_QUERY_INDEX &&
               uploader.getMethod() != HTTPRequestMethod.HEAD;
	}
    
    /**
     * Determines whether or not this Uploader should bypass queueing,
     * (meaning that it will always work immediately, and will not use
     *  up slots for other uploaders).
     *
     * All requests that are not the 'connecting' state should bypass
     * the queue, because they have already been queued once.
     */
    private boolean shouldBypassQueue(Uploader uploader) {
        return uploader.getState() != Uploader.CONNECTING ||
               uploader.getMethod() == HTTPRequestMethod.HEAD;
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
        debug(uploader + " cleaning up finished.");
        
        int state = uploader.getState();
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
                break;
            case Uploader.INTERRUPTED:
                UploadStat.INTERRUPTED.incrementStat();
                break;
        }
        
        if ( shouldShowInGUI(uploader) ) {
            FileDesc fd = uploader.getFileDesc();
            if( fd != null && state == Uploader.COMPLETE ) {
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
            if(fm.isValidIndex(index)) {
                fd = fm.get(index);
            } 
            // If the index was invalid or the file was unshared, FNF.
            if(fd == null) {
                uploader.setState(Uploader.FILE_NOT_FOUND);
                return;
            }
            // If the name they want isn't the name we have, FNF.
            if(!uploader.getFileName().equals(fd.getName())) {
                uploader.setState(Uploader.FILE_NOT_FOUND);
                return;
            }
            
            try {
                uploader.setFileDesc(fd);
            } catch(IOException ioe) {
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
                uploader.setState(Uploader.FILE_NOT_FOUND);
                return;
            }
            
            // If they requested an incomplete file, determine
            // if we have the correct range.  If not, change
            // state appropriately.
            if (fd instanceof IncompleteFileDesc) {
                IncompleteFileDesc ifd = (IncompleteFileDesc)fd;
                int upStart = uploader.getUploadBegin();
                int upEnd = uploader.getUploadEnd();
                if ( !ifd.isRangeSatisfiable(upStart, upEnd) ) {
                    uploader.setState(Uploader.UNAVAILABLE_RANGE);
                }
                return;
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
        debug(uploader + " processing new request.");
        
        int queued = -1;
        
        // If this uploader should not bypass the queue, determine it's
        // slot.
        if( !shouldBypassQueue(uploader) ) {
            // If we are forcing this upload, intercept the queue check.
            if( forceAllow )
                queued = ACCEPTED;
            // Otherwise, determine whether or not to queue, accept
            // or reject the uploader.
            else {
                // note that checkAndQueue can throw an IOException
                try {
                    queued = checkAndQueue(uploader, socket);
                } catch(IOException ioe) {
                    uploader.setState(Uploader.INTERRUPTED);
                    throw ioe;
                }
                Assert.that(queued != -1);
            
            }
        } else {
            queued = BYPASS_QUEUE;
        }
        
        // Act upon the queued state.
        switch(queued) {
            case REJECTED:
                uploader.setState(Uploader.LIMIT_REACHED);
                break;
            case QUEUED:
                uploader.setState(Uploader.QUEUED);
                socket.setSoTimeout(MAX_POLL_TIME);
                break;
            case ACCEPTED:
                assertAsConnecting( uploader.getState() );
                synchronized (this) {
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
    private void addToGUI(Uploader uploader) {
        
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
            case Uploader.CONNECTING:
                uploader.setState(Uploader.UPLOADING);
                UploadStat.UPLOADING.incrementStat();
                break;
            case Uploader.COMPLETE:
            case Uploader.INTERRUPTED:
                Assert.that(false, "invalid state in doSingleUpload");
                break;
        }
        
        debug(uploader + " doing single upload");
        
        try {
            uploader.initializeStreams();
            uploader.writeResponse();
            uploader.setState(Uploader.COMPLETE);
        } catch(IOException failed) {
            uploader.setState(Uploader.INTERRUPTED);
            throw failed;
        } finally {
            uploader.closeFileStreams();
        }
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
     * Returns whether or not there are currently upload slots available,
     * not taking the queue into account.  In particular, if there are 
     * no upload slots, but there are queue slots, this will still
     * return <tt>true</tt>.
     *
     * @return <tt>true</tt> if there are no upload slots available
     */
	public synchronized boolean isBusy() {
		// return true if Limewire is shutting down
		if (RouterService.getIsShuttingDown())
		    return true;
		
		// testTotalUploadLimit returns true is there are
		// slots available, false otherwise.
		return !testTotalUploadLimit();
	}

    /**
     * Returns whether or not the upload queue is full.
     *
     * @return <tt>true</tt> if the upload queue is full, otherwise
     *  <tt>false</tt>
     */
    public synchronized boolean isQueueFull() {
		// return true if Limewire is shutting down
		if (RouterService.getIsShuttingDown())
		    return true;
		
		// testTotalUploadLimit returns true is there are
		// slots available, false otherwise.
		return 
            (_queuedUploads.size() >=
             UploadSettings.UPLOAD_QUEUE_SIZE.getValue());
    }

	public synchronized int uploadsInProgress() {
		return _activeUploadList.size();
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


	/////////////////// Private Interface for Testing Limits /////////////////

    /** Checks whether the given upload may proceed based on number of slots,
     *  position in upload queue, etc.  Updates the upload queue as necessary.
     *  Always accepts Browse Host requests, though.  Notifies callback of this.
     *  
     * @return ACCEPTED if the download may proceed, QUEUED if this is in the
     *  upload queue, or REJECTED if this is flat-out disallowed (and hence not
     *  queued).  If REJECTED, <tt>uploader</tt>'s state will be set to
     *  LIMIT_REACHED.  
     * @exception IOException the request came sooner than allowed by upload
     *  queueing rules.  (Throwing IOException forces the connection to be
     *  closed by the calling code.)  */
	private synchronized int checkAndQueue(Uploader uploader,
	                                       Socket socket) throws IOException {
        boolean limitReached = hostLimitReached(uploader.getHost());
        int size = _queuedUploads.size();
        int posInQueue = positionInQueue(socket);//-1 if not in queue
        int maxQueueSize = UploadSettings.UPLOAD_QUEUE_SIZE.getValue();
        boolean wontAccept = size >= maxQueueSize;
        int ret = -1;
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
            debug(uploader+"Uploader not in que(capacity:"+maxQueueSize+")");
            if(limitReached || wontAccept) { 
                debug(uploader+" limited? "+limitReached+" wontAccept? "
                      +wontAccept);
                return REJECTED; //we rejected this uploader
            }
            addToQueue(socket);
            posInQueue = size;//the index of the uploader in the queue
            ret = QUEUED;//we have queued it now
            debug(uploader+" new uploader added to queue");
        }
        else {//we are alreacy in queue, update it
            KeyValue kv = (KeyValue)_queuedUploads.get(posInQueue);
            Long prev=(Long)kv.getValue();
            if(prev.longValue()+MIN_POLL_TIME > System.currentTimeMillis()) {
                _queuedUploads.remove(posInQueue);
                debug(uploader+" queued uploader flooding-throwing exception");
                throw new IOException();
            }
            kv.setValue(new Long(System.currentTimeMillis()));
            debug(uploader+" updated queued uploader");
            ret = QUEUED;//queued
        }
        debug(uploader+" checking if given uploader is can be accomodated ");
        //If uploader can and should be in queue, it is at this point.        
        if(!this.isBusy() && posInQueue==0) {//I have a slot &&  uploader is 1st
            ret = ACCEPTED;
            debug(uploader+" accepting upload");
            //remove this uploader from queue, and get its time
            _queuedUploads.remove(0);
        }
        else {
            //(!busy && posInQueue>0) || (busy && we are somewhere in the queue)
            //In either of these cases, if uploader does not support queueing,
            //it should be removed from the queue.
            if(!queue) {//downloader does not support queueing
                _queuedUploads.remove(posInQueue);//remove it
                ret = REJECTED;
            }
        }
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
		_activeUploadList.remove(uploader);//no effect is not in

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
	 * Returns true iff another upload is allowed.  Note that because this test
	 * relies on the uploadsInProgress() method, it may sometimes be incorrect
	 * if a push request takes a long time to respond.  REQUIRES: this'
     * monitor is held.
     */
	private boolean testTotalUploadLimit() {
        //Allow another upload if (a) we currently have fewer than
        //SOFT_MAX_UPLOADS uploads or (b) some upload has more than
        //MINIMUM_UPLOAD_SPEED KB/s.  But never allow more than MAX_UPLOADS.
        //
        //In other words, we continue to allow uploads until everyone's
        //bandwidth is diluted.  The assumption is that with MAX_UPLOADS
        //uploads, the probability that all just happen to have low capacity
        //(e.g., modems) is small.  This reduces "Try Again Later"'s at the
        //expensive of quality, making swarmed downloads work better.        

		int current = uploadsInProgress();
        SettingsManager settings=SettingsManager.instance();
		if (current >= settings.getMaxUploads()) {
            return false;
        } else if (current < settings.getSoftMaxUploads()) {
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

		SettingsManager manager = SettingsManager.instance();
		// To calculate the total bandwith available for
		// uploads, there are two properties.  The first
		// is what the user *thinks* their connection
		// speed is.  Note, that they may have set this
		// wrong, but we have no way to tell.
		float connectionSpeed = 
            ((float)manager.getConnectionSpeed())/8.f;
		// the second number is the speed that they have 
		// allocated to uploads.  This is really a percentage
		// that the user is willing to allocate.
		float speed = UploadSettings.UPLOAD_SPEED.getValue();
		// the total bandwith available then, is the percentage
		// allocated of the total bandwith.
		float totalBandwith = ((connectionSpeed*((float)speed/100.F)));
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
		
        // read the first line. if null, throw an exception
        String str = br.readLine();
        
        try {

            if (str == null) {
                throw new IOException();
            }

            str.trim();

            if(this.isURNGet(str)) {
                // handle the URN get request
                return this.parseURNGet(str);
            } else if (this.isMalformedURNGet(str)) {
                // handle the malforned URN get request
                return this.parseMalformedURNGet(str);
            }
		
            // handle the standard get request
            return this.parseTraditionalGet(str);
        } catch (IOException ioe) {
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
	 * Returns whether or not the HTTP get request is a traditional 
	 * Gnutella-style HTTP get.
	 *
	 * @return <tt>true</tt> if it is a traditional Gnutella HTTP get,
	 *  <tt>false</tt> otherwise
	 */
	private boolean isTraditionalGet(final String requestLine) {
		return (requestLine.indexOf("/get/") != -1);
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
	 * Returns whether or not the get request for the specified line is
	 * a malformed URN request coming from LimeWire 2.8.6.<p>
	 *
	 * An example malformed request is:
	 * /get/0//uri-res/N2R?urn:sha1:AZUCWY54D63___Z3WPHN7VSVTKZA3YYT HTTP/1.1
	 * (where the /get/0// are the malformations)
	 *
	 * @param requestLine the <tt>String</tt> to parse to check whether it's
	 *  following the URN request syntax as specified in HUGE v. 0.93
	 * @return <tt>true</tt> if the request is a valid URN request, 
	 *  <tt>false</tt> otherwise
	 */
	private boolean isMalformedURNGet(final String requestLine) {
	    // the malformed request will always start with /get/0//
	    if ( requestLine.startsWith("/get/0//") ) {
	        // the valid request starts with the last slash of the malformation
	        return isURNGet(requestLine.substring(7));
        }
	    
	    return false;
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
            //http information part: HTTP/1.0
            String httpInfoPart = st.nextToken().trim();
            
			String fileName;
            if(fileInfoPart.equals("/")) {
                //special case for browse host request
                index = BROWSE_HOST_FILE_INDEX;
                fileName = "Browse-Host Request";
                UploadStat.BROWSE_HOST.incrementStat();
            } else if (fileInfoPart.equals("/update.xml")) {
                index = UPDATE_FILE_INDEX;
                fileName = "Update-File Request";
                UploadStat.UPDATE_FILE.incrementStat();
            } else if (fileInfoPart.startsWith("/gnutella/pushproxy")) {
                index = PUSH_PROXY_FILE_INDEX;
                // set the filename as the servent ID
                StringTokenizer stLocal = new StringTokenizer(fileInfoPart, "=");
                if (stLocal.countTokens() < 2)
                    throw new IOException("Malformed PushProxy HTTP Request");
                // skip first part
                stLocal.nextToken();
                // had better be the client GUID
                fileName = stLocal.nextToken();
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
            //check if the protocol is HTTP1.1. Note that this is not a very 
            //strict check.
            boolean http11 = false;
            if(requestLine.endsWith("1.1"))
                http11 = true;
			return new HttpRequestLine(index, fileName, http11);
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
		FileDesc desc = RouterService.getFileManager().getFileDescForUrn(urn);
		if(desc == null) {
		    UploadStat.UNKNOWN_URN_GET.incrementStat();
            return new HttpRequestLine(BAD_URN_QUERY_INDEX,
                  "Invalid URN query", isHTTP11Request(requestLine));
		}		
		int fileIndex = desc.getIndex();
		String fileName = desc.getName();
		UploadStat.URN_GET.incrementStat();
		return new HttpRequestLine(desc.getIndex(), desc.getName(), 
								   isHTTP11Request(requestLine));
	}
	
	/**
	 * Parses the get line for a malformed URN request, throwing an exception 
	 * if there are any errors in parsing.
	 *
	 * @param requestLine the <tt>String</tt> instance containing the get
	 *        request
	 * @return a new <tt>RequestLine</tt> instance containing all of the data
	 *  for the get request
	 */
	private HttpRequestLine parseMalformedURNGet(final String requestLine)
      throws IOException {
		// this assumes the malformation is a /get/0/ before the /uri-res..
		return parseURNGet(requestLine.substring(7));
	}
	


	/**
	 * Returns whether or the the specified get request is using HTTP 1.1.
	 *
	 * @return <tt>true</tt> if the get request specifies HTTP 1.1,
	 *  <tt>false</tt> otherwise
	 */
	private boolean isHTTP11Request(final String requestLine) {
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
		 * Constructs a new <tt>RequestLine</tt> instance.
		 *
		 * @param index the index for the file to get
		 * @param fileName the name of the file to get
		 * @param http11 specifies whether or not it's an HTTP 1.1 request
		 */
		HttpRequestLine(int index, String fileName, boolean http11) {
  			_index = index;
  			_fileName = fileName;
            _http11 = http11;
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
  	}

    /** Calls measureBandwidth on each uploader. */
    public synchronized void measureBandwidth() {
        float currentTotal = 0f;
        boolean c = false;
        for (Iterator iter = _activeUploadList.iterator(); iter.hasNext(); ) {
            c = true;
			BandwidthTracker bt = (BandwidthTracker)iter.next();
			bt.measureBandwidth();
			currentTotal += bt.getAverageBandwidth();
		}
		if ( c )
		    averageBandwidth = ( (averageBandwidth * numMeasures) + currentTotal ) 
		                    / ++numMeasures;
    }

    /** Returns the total upload throughput, i.e., the sum over all uploads. */
	public synchronized float getMeasuredBandwidth() {
        float sum=0;
        for (Iterator iter = _activeUploadList.iterator(); iter.hasNext(); ) {
			BandwidthTracker bt = (BandwidthTracker)iter.next();
            float curr = 0;
            try {
                curr = bt.getMeasuredBandwidth();
            } catch(InsufficientDataException ide) {
                curr = 0;
            }
			sum+= curr;
		}
        return sum;
	}
	
	/**
	 * returns the summed average of the uploads
	 */
	public synchronized float getAverageBandwidth() {
        return averageBandwidth;
	}	
    
    private final boolean debugOn = false;
    private final boolean log = false;
    PrintWriter writer = null;
    private final void debug(String out) {
        if (debugOn) {
            if(log) {
                if(writer== null) {
                    try {
                        writer=new 
                        PrintWriter(new FileOutputStream("UploadLog.log",true));
                    }catch (IOException ioe) {
                        System.out.println("could not create log file");
                    }
                }
                writer.println(out);
                writer.flush();
            }
            else
                System.out.println(out);
        }
    }
    private final void debug(Exception e) {
        if (debugOn)
            e.printStackTrace();
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
        System.out.println("passed");
    }

}
