package com.limegroup.gnutella.downloader;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.util.BandwidthThrottle;
import com.limegroup.gnutella.util.IntPair;
import com.limegroup.gnutella.util.ThrottledOutputStream;
import com.sun.java.util.collections.Iterator;

public class TestUploader {    
    
    private static final Log LOG = LogFactory.getLog(TestUploader.class);
    
    /** My name, for debugging */
    private final String name;

    /** Number of bytes uploaded */
    private volatile int totalUploaded;
    /** The number of connections received */
    private int connects=0;
    /** The last request sent, e.g., "GET /get/0/file.txt HTTP/1.0" */
    private String request=null;

    /** The throttle rate in kilobytes/sec */
    private volatile float rate;    
    /**The number of bytes this uploader uploads before dying*/
    private volatile int stopAfter;
    /** This is stopped. */
    private boolean stopped;
    /** switch to send incorrect bytes to simulate a bad uploader*/
    private boolean sendCorrupt;

	private AlternateLocationCollection storedAltLocs;
	private AlternateLocationCollection incomingAltLocs;
	private URN                         _sha1;
    private boolean http11 = true;
    private ServerSocket server;
    private boolean busy = false;
    private int retryAfter = -1;
    private int timesBusy = Integer.MAX_VALUE;
    //Note about queue testing: This is how the queuing simulation works: If
    //queue is set, the uploader sends the X-Queue header etc in handleRequest
    //method, and sets values for minPollTime and maxPollTime. In the loop
    //method, if queue was set, when handleRequest returns, queue is set to
    //false and handleRequest is called one more time. This time handleRequest
    //will upload the file normally, but it will also check that the downloader
    //1. Kept the socket open 2. Responsed within the given time range.
    //3. completed the download after coming out of the queue.
    private boolean queue = false;
    private long minPollTime = -1;
    private long maxPollTime = -1;
    final int MIN_POLL = 45000;
    private final int MAX_POLL = 120000;
    private int partial = 0;
    
    boolean unqueue = true;
    int queuePos = 1;

    boolean killedByDownloader = false;
    
    /**
     * The offset for the low chunk.
     */
    private int lowChunkOffset = 0;
    
    /**
     * The offset for the high chunk.
     */
    private int highChunkOffset = 0;
        
    /**
     * The number of requests this uploader has received.
     */
    private int requestsReceived = 0;
    
    /**
     * Whether or not this uploader should respond with HTTP/1.1
     */
    private boolean respondWithHTTP11 = true;
    
    /**
     * The sum of the number of bytes we need to upload across all requests.  If
     * this value is less than totalUploaded and the uploader encountered an
     * IOException in handle request it means the downloader killed the
     * connection.
     */
    int totalAmountToUpload;

    /**
     * <tt>IPFilter</tt> for only allowing local connections.
     */
    private final IPFilter IP_FILTER = IPFilter.instance();


    /** 
     * Creates a TestUploader listening on the given port.  Will upload a
     * special test file to any requesters via HTTP.  Non-blocking; starts
     * another thread to do the listening. 
     */
    public TestUploader(String name, final int port) {

        // ensure that only local machines can connect!!
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {"127.*.*.*"});
        this.name=name;
        reset();
        
        try {
            server = new ServerSocket();
            //Use Java 1.4's option to reuse a socket address.  This is
            //important because some client thread may be using the given port
            //even though no threads are listening on the given socket.
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            LOG.debug("Couldn't bind socket to port "+port+"\n");
            //System.out.println("Couldn't listen on port "+port);
            ErrorService.error(e);
            return;
        }
        
        //spawn loop();
        Thread t = new Thread() {
            public void run() {
                try {
                    loop(port);
                } catch(Throwable t) {
                    ErrorService.error(t);
                }
            }
        };
        t.setDaemon(true);
        t.start();        
    }

    public void stopThread() {
        try {
            if ( server != null )
                server.close();
        } catch (IOException e) {}
    }

    /** 
     * Resets the rate, amount uploaded, stop byte, etc.
     */
    public void reset() {
	    storedAltLocs   = null;//new AlternateLocationCollection();
	    incomingAltLocs = null;//new AlternateLocationCollection();
        totalUploaded = 0;
        stopAfter = -1;
        rate = 10000;
        stopped = false;
        sendCorrupt = false;
        busy = false;
        retryAfter = -1;
        timesBusy = Integer.MAX_VALUE;
        queue = false;
        partial = 0;
        minPollTime = -1;
        maxPollTime = -1;
        unqueue = true;
        queuePos=1;
        killedByDownloader = false;
        totalAmountToUpload = 0;
        requestsReceived = 0;
        connects = 0;
        lowChunkOffset = 0;
        highChunkOffset = 0;
        respondWithHTTP11 = true;
    }

    public int amountUploaded() {
        return totalUploaded;
    }
    
    /** Sets the upload throttle rate 
     * Note: Even if the rate is set to zero the send method will send atleast 
     * one byte per second, in order to detect socket closes. 
     * @param rate kilobytes/sec. 
     */   
    public void setRate(float rate) {
        this.rate=rate;
    }
    
    /**
     * Determine how many times this test uploader should respond
     * with a busy status.
     */
    public void setTimesBusy(int nTimes) {
        this.timesBusy = nTimes;
    }
    
    /**
     * Determines what should be sent in the Retry-After header
     */
    public void setRetryAfter(int seconds) {
        this.retryAfter = seconds;
    }
    
    /**
     * Sets this test uploader to be busy.
     * Use setTimesBusy to set the number of times this
     * uploader should respond as busy.
     * By default it will respond Integer.MAX_VALUE times.
     */
    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public void setQueue(boolean q) { 
        this.queue = q;
    }

    public void setPartial(boolean part) {
        if(part)
            this.partial = 1;
        else
            this.partial = 0;//reset
    }

    /** Sets whether this should send bad data. */
    public void setCorruption(boolean corrupt) {
        this.sendCorrupt = corrupt;
    }

    /** 
     * Sets the number of  bytes that this should send.  This lets the user
     * simulate a broken upload.
     * @param n the number of  bytes to send, or -1 if no limit 
     */
    public void stopAfter(int n) {
        this.stopAfter = n;
    }

    /** 
     * Store the alternate locations that this uploader knows about.
     * @param alts the alternate locations
     */
    public void setAlternateLocations(AlternateLocationCollection alts) {
        storedAltLocs = alts;
    }
    
    /**
     * Sets the offset for the low chunk.
     */
    public void setLowChunkOffset(int offset) {
        lowChunkOffset = offset;
    }
    
    /**
     * Sets the offset for the high chunk.
     */
    public void setHighChunkOffset(int offset) {
        highChunkOffset = offset;
    }
    
    /**
     * Sets whether or not this uploader will respond with HTTP/1.1
     */
    public void setHTTP11(boolean yes) {
        respondWithHTTP11 = yes;
    }
    
    /** 
     * Get the alternate locations that this uploader has read from headers
     */
    public AlternateLocationCollection getAlternateLocations() {
        return incomingAltLocs;
    }
    
    /** 
     * Get the alternate locations that this uploader has read from headers
     */
    public URN getReportedSHA1() {
        return _sha1;
    }
    
    /** Returns the number of connections this accepted. */
    public int getConnections() {
        return connects;
    }
    
    /**
     * Returns the number of requests this received.
     */
    public int getRequestsReceived() {
        return requestsReceived;
    }
        
    /** Returns the last request sent or null if none. 
     *  @return a request like "GET /get/0/file.txt HTTP/1.1" */
    public String getRequest() {
        return request;
    }

    /**
     * Repeatedly accepts connections and handles them.
     */
    private void loop(int port) {
        Socket socket = null;
        while(true) {
            try {
                socket = server.accept();
                connects++;

                // make sure it's from us
				InetAddress address = socket.getInetAddress();
                if (isBannedIP(address.getHostAddress())) {
                    LOG.debug("Banned address -- closing");
                    server.close();
                    continue;
                }
                LOG.debug("Uploader accepted connection");
                //spawn thread to handle request
                final Socket mySocket = socket;
                Thread runner=new Thread() {
                    public void run() {          
                        try {
                            while(http11 && !stopped) {
                                handleRequest(mySocket);
                                if (queue) { 
                                    mySocket.setSoTimeout(MAX_POLL);
                                    if(unqueue) // second time give slot
                                        queue = false;
                                    handleRequest(mySocket);
                                }
                                mySocket.setSoTimeout(8000);
                            }
                        } catch (IOException e) {
                            if(totalUploaded < totalAmountToUpload)
                                killedByDownloader = true;
                            LOG.debug("Exception in uploader (" + name + ")", e);
                        } catch(Throwable t) {
                            ErrorService.error(t);
                        } finally {
                            try {
                                mySocket.close();
                            } catch (IOException e) {
                                return;
                            }
                        }//end of finally
                    }//end of run
                };
                runner.start();
            } catch (IOException e) {
                LOG.debug("exception in accept", e);
                try { server.close(); } catch (IOException ignore) { }
                return;  //server socket closed.
            }
            //handling next request
        }
    }

    /**
     * Returns whether <tt>ip</tt> is a banned address.
     * @param ip an address in resolved dotted-quad format, e.g., 18.239.0.144
     * @return true iff ip is a banned address.
     */
    public boolean isBannedIP(String ip) {        
        return !IP_FILTER.allow(ip);
    }
    
    private void handleRequest(Socket socket) throws IOException {
        //Find the region of the file to upload.  If a Range request is present,
        //use that.  Otherwise, send the whole file.  Skip all other headers.
        //TODO2: Later we should also check the validity of the requests
        BufferedReader input = 
            new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        ThrottledOutputStream output = new ThrottledOutputStream(
            new BufferedOutputStream(socket.getOutputStream()),
            new BandwidthThrottle(rate*1024));
        int start = 0;
        int stop = TestFile.length();
        boolean firstLine=true;
        AlternateLocationCollection badLocs = null;
        AlternateLocationCollection goodLocs = null;
        while (true) {
            String line=input.readLine();
            if (firstLine) {
                if(line != null && !line.equals("")) {
                    requestsReceived++;
                }
                request=line;
                firstLine=false;
            }
            if (line==null)
                throw new IOException("Unexpected close");
            if (line.equals(""))
                break;

			if(HTTPHeaderName.GNUTELLA_CONTENT_URN.matchesStartOfString(line)) {
				_sha1 = readContentUrn(line);
			}
            
            if(HTTPHeaderName.NALTS.matchesStartOfString(line))
                badLocs = readAlternateLocations(line);
			if(HTTPHeaderName.ALT_LOCATION.matchesStartOfString(line))
				goodLocs = readAlternateLocations(line);

            int i=line.indexOf("Range:");
            Assert.that(i<=0, "Range should be at the beginning or not at all");
            if (i==0) {
                IntPair p = null;
                try {
                    p=parseRange(line);
                } catch (Exception e) { 
                    Assert.that(false, "Bad Range request: \""+line+"\"");
                }
                start=p.a + lowChunkOffset;
                if(start < 0) start = 0;
                stop=p.b + highChunkOffset;
                if(stop > TestFile.length()) stop = TestFile.length();
            }
            
            i = line.indexOf("GET");
            if(i==0)
                http11 = line.indexOf("1.1") > 0;
		}
        if(_sha1!=null) {
            if(incomingAltLocs == null)
                incomingAltLocs = AlternateLocationCollection.create(_sha1);
            if(badLocs!=null) {
                synchronized(badLocs) {
                    Iterator iter = badLocs.iterator();
                    while(iter.hasNext()) 
                        incomingAltLocs.remove((AlternateLocation)iter.next());
                }
            }
            if(goodLocs!=null) {
                synchronized(goodLocs) {
                    Iterator iter = goodLocs.iterator();
                    while(iter.hasNext()) 
                        incomingAltLocs.add((AlternateLocation)iter.next());
                }        
            }
        }
        //System.out.println(System.currentTimeMillis()+" "+name+" "+start+" - "+stop);

    //Send the data.
        send(output, start, stop);
    }

    private void send(OutputStream out, int start, int stop) 
        throws IOException {
        totalAmountToUpload += stop - start;
        //Write header, stolen from NormalUploadState.writeHeader()
        long t0 = System.currentTimeMillis();
        if(minPollTime > 0) 
            Assert.that(t0 > minPollTime,
                        "queued downloader responded too quick by "+
                        (minPollTime-t0)+" mS");
        if(maxPollTime > 0) 
            Assert.that(t0 < maxPollTime,
                        "queued downloader responded too late, by "+
                        (t0-maxPollTime) +" mS");        

        String httpValue = respondWithHTTP11 ? "HTTP/1.1" : "HTTP/1.0";
		String str = httpValue + " " +
            (busy || queue || partial==1 ?
            "503 Service Unavailable\r\n" :
            "200 OK \r\n");
		out.write(str.getBytes());
		
		if(busy && retryAfter != -1) {
		    str = "Retry-After: " + retryAfter + "\r\n";
		    out.write(str.getBytes());
		}

        if(queue) {
            LOG.debug("Upload Queued");
            str = "X-Queue: position="+queuePos+
                ", pollMin=" + MIN_POLL/1000 + 
                ", pollMax=" + MAX_POLL/1000 +
                "\r\n";
            out.write(str.getBytes());
            str = "\r\n";
            out.write(str.getBytes());
            out.flush();//don't close socket
            long t = System.currentTimeMillis();
            minPollTime = t+MIN_POLL;
            maxPollTime = t+MAX_POLL;
            return;
        }
        
        if(partial > 0) {
            switch(partial) {
            case 1:
                str="X-Available-Ranges: ByTes 50000-400000, 500000-600000,800000-900000\r\n";
                break;
            case 2:
                str="X-Available-Ranges: bytes 50000-900000\r\n";
                break;
            default:
                str="X-Available-Ranges: bytes 50000-150000\r\n";
            }
            out.write(str.getBytes());
            out.flush();
            partial++;
            if(partial==2) {//was 1 until last statement
                str="\r\n";
                out.write(str.getBytes());
                out.flush();
                return;
            }
        }
		str = "Content-Length:"+ (stop - start) + "\r\n";
		out.write(str.getBytes());	   
		if (start != 0 || (stop - start != TestFile.length())) {
            //Note that HTTP stop values are INCLUSIVE.  Our internal values
            //are EXCLUSIVE.  Hence the -1.
			str = "Content-range: bytes " + start  +
			"-" + (stop-1) + "/" + TestFile.length() + "\r\n"; 
			out.write(str.getBytes());
		}
		if(storedAltLocs != null && storedAltLocs.hasAlternateLocations()) {

            LOG.debug("Writing alternate location header:\n"+storedAltLocs+"\n");      
            HTTPUtils.writeHeader(HTTPHeaderName.ALT_LOCATION,
                                  storedAltLocs, out);
        } else {
            LOG.debug("Did not write alt locs:\n"+storedAltLocs+"\n");
        }
        str = "\r\n";
		out.write(str.getBytes());
        out.flush();
        if (busy) {
            //turn busy off for the next time if necessary
            if( connects >= timesBusy )
                busy = false;
            out.close();
            return;
        }
        
        //Write data.
        for (int i=start; i<stop; ) {
            //1 second write cycle
            if (stopAfter > -1 && totalUploaded == stopAfter) {
                stopped=true;
                out.flush();
                throw new IOException();
            }
            if(sendCorrupt)
                out.write(TestFile.getByte(i)+(byte)1);
            else
                out.write(TestFile.getByte(i));
            totalUploaded++;
            i++;
        }
        out.flush();
    }

    /**
     * Returns start (inclusive) and stop range (exclusive) from the given Range
     * header.  In other words, "Range; 2-5" will return [2, 6). Extracted from
     * HTTPUploader.
     *
     * @exception IndexOutOfBoundsException parse error
     * @exception NumberFormatException parse error
     * @exception IOException socket closed
     */
    private IntPair parseRange(String str) 
            throws IndexOutOfBoundsException, 
                   NumberFormatException, IOException {
        int start=0;
        int stop=TestFile.length();
        //Set 'sub' to the value after the "bytes=" or "bytes ".  Note
        //that we don't validate the data between "Range:" and the
        //bytes.
        String sub;
        String second;
        try {
            int i=str.indexOf("bytes");    //TODO: use constant
            if (i<0)
                throw new IOException();
            i+=6;                          //TODO: use constant
            sub = str.substring(i);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException();
        }
        // remove the white space
        sub = sub.trim();   
        char c;
        // get the first character
        try {
            c = sub.charAt(0);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException();
        }
        // - n  
        if (c == '-') {  
            // String second;
            try {
                second = sub.substring(1);
            } catch (IndexOutOfBoundsException e) {
                throw new IOException();
            }
            second = second.trim();
            try {
                //A range request for "-3" means return the last 3 bytes
                //of the file.  (LW used to incorrectly return bytes
                //0-3.)  
                start = Math.max(0, TestFile.length()-Integer.parseInt(second));
            } catch (NumberFormatException e) {
                throw new IOException();
            }
        }
        else {                
            // m - n or 0 -
            int dash = sub.indexOf("-");
            String first;
            try {
                first = sub.substring(0, dash);
            } catch (IndexOutOfBoundsException e) {
                throw new IOException();
            }
            first = first.trim();
            try {
                start = java.lang.Integer.parseInt(first);
            } catch (NumberFormatException e) {
                throw new IOException();
            }
            try {
                second = sub.substring(dash+1);
            } catch (IndexOutOfBoundsException e) {
                throw new IOException();
            }
            second = second.trim();
            if (!second.equals("")) 
                try {
                    stop = java.lang.Integer.parseInt(second)+1; //sic
                } catch (NumberFormatException e) {
                    throw new IOException();
                }
        }
        //check that the downloader made a correct range request if we are
        //partial and this is the second iteration. 
        if(partial==3) {//check this before checking partial2 -- we set it there
            Assert.that(start==150000,
                        "downloader picked incorrect start range in iteration");
            Assert.that(stop==250010,
                        "downloader pick incorrect stop range in iteration");
        }
        if(partial==2) {
            Assert.that(start==50000,"invalid start range");
            Assert.that(stop==150010,"invalid stop range was:"+stop);
        }                
        return new IntPair(start, stop);
    }
	/**
	 * Reads alternate location header.  The header can contain only one
	 * alternate location, or it can contain many in the same header.
	 * This method adds them all to the <tt>FileDesc</tt> for this
	 * uploader.  This will not allow more than 20 alternate locations
	 * for a single file.
	 *
	 * @param altHeader the full alternate locations header
	 * @param alc the <tt>AlternateLocationCollector</tt> that read alternate
	 *  locations should be added to
	 */
	private AlternateLocationCollection readAlternateLocations
                                                     (final String altHeader) {
        AlternateLocationCollection alc=null;
		final String alternateLocations=HTTPUtils.extractHeaderValue(altHeader);

		// return if the alternate locations could not be properly extracted
		if(alternateLocations == null) return null;
		StringTokenizer st = new StringTokenizer(alternateLocations, ",");
        if(_sha1!=null)
            alc = AlternateLocationCollection.create(_sha1);
        if (alc==null)
            return null;
		while(st.hasMoreTokens()) {
			try {
				// note that the trim method removes any CRLF character
				// sequences that may be used if the sender is using
				// continuations.
				AlternateLocation al = 
				    AlternateLocation.create(
				        st.nextToken().trim(), _sha1);
				alc.add(al);
			} catch(IOException e) {
                e.printStackTrace();
				// just return without adding it.
				continue;
			}
		}
        return alc;
	}

	/**
	 * This method parses the "X-Gnutella-Content-URN" header, as specified
	 * in HUGE v0.93.  This assigns the requested urn value for this 
	 * upload, which otherwise remains null.
	 *
	 * @param contentUrnStr the string containing the header
	 * @return a new <tt>URN</tt> instance for the request line, or 
	 *  <tt>null</tt> if there was any problem creating it
	 */
	private static URN readContentUrn(final String contentUrnStr) {
		String urnStr = HTTPUtils.extractHeaderValue(contentUrnStr);
		
		// return null if the header value could not be extracted
		if(urnStr == null) return null;
		try {
			return URN.createSHA1Urn(urnStr);
		} catch(IOException e) {
			// this will be thrown if the URN string was invalid for any
			// reason -- just return null
			return null;
		}		
	}
}
