package com.limegroup.gnutella;

import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.http.HTTPRequestMethod;

import com.sun.java.util.collections.List;
import com.sun.java.util.collections.LinkedList;
import com.sun.java.util.collections.Iterator;
import java.util.Date;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;


/**
 * Manages state for push upload requests.
 */
public final class PushManager {
    
	/**
	 * The maximum time in SECONDS after an unsuccessful push until we will
     *  try the push again.  This should be larger than the 15+4*30=135 sec
     *  window in which Gnutella resends pushes by default
     */
    private static final int PUSH_INVALIDATE_TIME=60*5;  //5 minutes
    
	/**
     * The list of all files that we've tried unsuccessfully to upload
     * via pushes.  (Here successful means we were able to connect.
     * It does not mean the file was actually transferred.) If we get
     * another push request from one of these hosts (e.g., because the
     * host is firewalled and sends multiple push packets) we will not
     * try again.
     *
     * INVARIANT: for all i>j, ! failedPushes[i].before(failedPushes[j])
     * LOCKING: Obtain this' monitor
	 */
	private List /* of PushRequestedFile */ _failedPushes=
        new LinkedList();
    private List /* of PushRequestedFile */ _attemptingPushes=
        new LinkedList();
    
	/**
	 * Accepts a new push upload.
     * NON-BLOCKING: creates a new thread to transfer the file.
	 * <p>
     * The thread connects to the other side, waits for a GET/HEAD,
     * and delegates to the UploaderManager.acceptUpload method with the
     * socket it created.
     * Essentially, this is a reverse-Acceptor.
     * <p>
	 * @param file the fully qualified pathname of the file to upload
	 * @param host the ip address of the host to upload to
	 * @param port the port over which the transfer will occur
	 * @param index the index of the file in <tt>FileManager</tt>
	 * @param guid the unique identifying client guid of the uploading client
     * @param forceAllow whether or not to force the UploadManager to send
     *  accept this request when it comes back.
	 */
	public void acceptPushUpload(final String file, 
    							 final String host, 
                                 final int port, 
    							 final int index, 
                                 final String guid,
                                 final boolean forceAllow) {
                                    
        if( file == null )
            throw new NullPointerException("null file");
        if( host == null )
            throw new NullPointerException("null host");
        if( !NetworkUtils.isValidPort(port) )
            throw new IllegalArgumentException("invalid port: " + port);
        if( guid == null )
            throw new NullPointerException("null guid");
                                    
        // First validate the information.     
        FileManager fm = RouterService.getFileManager();
        FileDesc fd = null;
        if(fm.isValidIndex(index)) {
            fd = fm.get(index);
        } 
        // If invalid fd, just exit...
        if(fd == null) {
            return;
        }
        // If our filename isn't the same as what they wanted, exit.
        if(!file.equals(fd.getName())) {
            return;
        }

        // Test if we are either currently attempting a push, or we have
        // unsuccessfully attempted a push with this host in the past.
        synchronized(this) {
    		clearFailedPushes();
            if ( !forceAllow && (
                 (! testAttemptingPush(host, index) )  ||
                 (! testFailedPush(host, index) ) ) )
                return;
            insertAttemptingPush(host, index);
        }    

        Thread runner=new Thread("PushUploadThread") {
            public void run() {
                Socket s = null;
                try {
        			// try to create the socket.
        			s = new Socket(host, port);
        			// open a stream for writing to the socket
        			OutputStream ostream = s.getOutputStream();        
        			String giv = "GIV " + index + ":" + guid + "/" + 
        			             file + "\n\n";
        			ostream.write(giv.getBytes());
        			ostream.flush();

                    //read GET or HEAD and delegate appropriately.
                    String word = IOUtils.readWord(
                        s.getInputStream(), 4);
                    if (word.equals("GET")) {
                        UploadStat.PUSHED_GET.incrementStat();
                        RouterService.getUploadManager().acceptUpload(
                            HTTPRequestMethod.GET, s, forceAllow);
                    } else if (word.equals("HEAD")) {
                        UploadStat.PUSHED_HEAD.incrementStat();
                        RouterService.getUploadManager().acceptUpload(
                            HTTPRequestMethod.HEAD, s, forceAllow);
                    } else {
                        UploadStat.PUSHED_UNKNOWN.incrementStat();
                        throw new IOException();
                    }
                } catch(IOException ioe){//connection failed? do book-keeping
                    UploadStat.PUSH_FAILED.incrementStat();
                    synchronized(this) { 
                        insertFailedPush(host, index);  
                    }
                } catch(Throwable e) {
					ErrorService.error(e);
				}
                finally {
                    if( s != null ) {
                        try {
                            s.getInputStream().close();
                        } catch(IOException ioe) {}
                        try {
                            s.getOutputStream().close();
                        } catch(IOException ioe) {}
                        try {
                            s.close();
                        } catch(IOException ioe) {}
                    }
                    // do this here so if the index changes, there is no
                    // confusion
                    synchronized(this) {
                        removeAttemptingPush(host, index);
                    }                    
                }
            }
        };
        runner.start();
	}
	


    /** @requires caller has this's monitor */
	private void insertFailedPush(String host, int index) {
		_failedPushes.add(new PushedFile(host, index));
	}
	
    /** @requires caller has this's monitor */
	private boolean testFailedPush(String host, int index) {
		PushedFile pf = new PushedFile(host, index);
		PushedFile pfile;
		Iterator iter = _failedPushes.iterator();
		while ( iter.hasNext() ) {
			pfile = (PushedFile)iter.next();
			if ( pf.equals(pfile) ) 
				return false;
		}
		return true;

	}

    /** @requires caller has this's monitor */
	private void insertAttemptingPush(String host, int index) {
		_attemptingPushes.add(new PushedFile(host, index));
	}

    /** @requires caller has this's monitor */
	private boolean testAttemptingPush(String host, int index) {
		PushedFile pf = new PushedFile(host, index);
		PushedFile pfile;
		Iterator iter = _attemptingPushes.iterator();
		while ( iter.hasNext() ) {
			pfile = (PushedFile)iter.next();
			if ( pf.equals(pfile) )
				return false;
		}
		return true;
	}

    /** @requires caller has this's monitor */	
	private void removeAttemptingPush(String host, int index) {
		PushedFile pf = new PushedFile(host, index);
		PushedFile pfile;
		Iterator iter = _attemptingPushes.iterator();
		while ( iter.hasNext() ) {
			pfile = (PushedFile)iter.next();
			if ( pf.equals(pfile) )
				// calling iter.remove() rather than
				// remove on the list, since this will be
				// safer while iterating through the list.
				iter.remove();
		}
	}

    /** @requires caller has this's monitor */
	private void clearFailedPushes() {
		// First remove all files that were pushed more than a few minutes ago
		Date time = new Date();
		time.setTime(time.getTime()-(PUSH_INVALIDATE_TIME*1000));
		Iterator iter = _failedPushes.iterator();
		while (iter.hasNext()) {
			PushedFile pf=(PushedFile)iter.next();
			if (pf.before(time))
				iter.remove();
		}
		
	}

	/**
	 * Keeps track of a push requested file and the host that requested it.
	 */
	private static class PushedFile {
		private final String _host;
        private final int _index;
		private final Date _time;        

		public PushedFile(String host, int index) {
			_host = host;
            _index = index;
			_time = new Date();
		}
		
        /** Returns true iff o is a PushedFile with same _host and _index.
         *  Time doesn't matter. */
		public boolean equals(Object o) {
			if(o == this) return true;
            if (! (o instanceof PushedFile))
                return false;
            PushedFile pf=(PushedFile)o;
			return _index==pf._index && _host.equals(pf._host);
		}
		
		public boolean before(Date time) {
			return _time.before(time);
		}
		
	}
}