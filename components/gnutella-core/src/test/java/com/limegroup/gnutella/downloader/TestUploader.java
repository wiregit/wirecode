package com.limegroup.gnutella.downloader;

import java.net.*;
import java.io.*;
import java.util.StringTokenizer;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.uploader.HTTPUploader;
import com.limegroup.gnutella.http.*;

public class TestUploader {    
    /** My name, for debugging */
    private final String name;

    /** Number of bytes uploaded */
    private volatile int totalUploaded;
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
	private URN                         sha1;
    ServerSocket server = null;
    private boolean busy = false;


    /** 
     * Creates a TestUploader listening on the given port.  Will upload a
     * special test file to any requesters via HTTP.  Non-blocking; starts
     * another thread to do the listening. 
     */
    public TestUploader(String name, final int port) {
        this.name=name;
        reset();
        //spawn loop();
        Thread t = new Thread() {
            public void run() {
                loop(port);
            }
        };
        t.setDaemon(true);
        t.start();        
    }

    public void stopThread() {
        try {
            if (server!=null)
                server.close();
        } catch (IOException e) {}
    }

    /** 
     * Resets the rate, amount uploaded, stop byte, etc.
     */
    public void reset() {
	    storedAltLocs   = new AlternateLocationCollection();
	    incomingAltLocs = new AlternateLocationCollection();
        totalUploaded = 0;
        stopAfter = -1;
        rate = 10000;
        stopped = false;
        sendCorrupt = false;
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
    
    public void setBusy(boolean busy) {
        this.busy = busy;
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
     * Get the alternate locations that this uploader has read from headers
     */
    public AlternateLocationCollection getAlternateLocations() {
        return incomingAltLocs;
    }
    
    /** 
     * Get the alternate locations that this uploader has read from headers
     */
    public URN getReportedSHA1() {
        return sha1;
    }
    

    /**
     * Repeatedly accepts connections and handles them.
     */
    private void loop(int port) {
        try {
            server = new ServerSocket(port);
        } catch (IOException ioe) {
            DownloadTest.debug("Couldn't bind socket to port "+port+"\n");
            return;
        }

        while(true) {
            Socket s=null;
            try {
                s = server.accept();
                //spawn thread to handle request
                final Socket mySocket=s;
                Thread runner=new Thread() {
                    public void run() {          
                        try {
                            if (!stopped) {
                                handleRequest(mySocket);
                            }
                        } catch (IOException e) {
                            //e.printStackTrace();
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
                //e.printStackTrace();
                return;  //server socket closed.
            }
        }
    }

    
    private void handleRequest(Socket socket) throws IOException {
        //Find the region of the file to upload.  If a Range request is present,
        //use that.  Otherwise, send the whole file.  Skip all other headers.
        //TODO2: later we should check here for HTTP1.1
        //TODO2: Later we should also check the validity of the requests
        BufferedReader input = 
            new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        BufferedOutputStream output = 
            new BufferedOutputStream(socket.getOutputStream());
        int start = 0;
        int stop = TestFile.length();
        while (true) {
            String line=input.readLine();
            if (line==null)
                throw new IOException("Unexpected close");
            if (line.equals(""))
                break;
			if(HTTPHeaderName.ALT_LOCATION.matchesStartOfString(line)) {
				readAlternateLocations(line, incomingAltLocs);
            }        
			if(HTTPHeaderName.CONTENT_URN.matchesStartOfString(line)) {
				sha1 = readContentUrn(line);
			}

            int i=line.indexOf("Range:");
            Assert.that(i<=0, "Range should be at the beginning or not at all");
            if (i==0) {
                IntPair p = null;
                try {
                    p=parseRange(line);
                } catch (Exception e) { 
                    Assert.that(false, "Bad Range request: \""+line+"\"");
                }
                start=p.a;
                stop=p.b;;
            }
		}
        //System.out.println(System.currentTimeMillis()+" "+name+" "+start+" - "+stop);

        //Send the data.
        send(output, start, stop);
    }

    private void send(OutputStream out, int start, int stop) 
            throws IOException {
        //Write header, stolen from NormalUploadState.writeHeader()
        
		String str = busy?"HTTP 503 Service Unavailable":"HTTP 200 OK \r\n";
		out.write(str.getBytes());
        if (busy) {
            String s = "\r\n"; 
            out.write(s.getBytes());
            out.flush();
            out.close();
            return;
        }
		str = "Content-length:"+ (stop - start) + "\r\n";
		out.write(str.getBytes());	   
		if (start != 0) {
            //Note that HTTP stop values are INCLUSIVE.  Our internal values
            //are EXCLUSIVE.  Hence the -1.
			str = "Content-range: bytes " + start  +
			"-" + (stop-1) + "/" + TestFile.length() + "\r\n"; 
			out.write(str.getBytes());
		}
		if(storedAltLocs.hasAlternateLocations()) 
		    HTTPUtils.writeHeader(HTTPHeaderName.ALT_LOCATION,
		      storedAltLocs, out);
        str = "\r\n";
		out.write(str.getBytes());
        out.flush();

        //Write data at throttled rate.  See NormalUploadState.  TODO: use
        //ThrottledOutputStream
        for (int i=start; i<stop; ) {
            //1 second write cycle
            long startTime=System.currentTimeMillis();
            for (int j=0; j<Math.max(1,(rate*1024)) && i<stop; j++) {
                //if we are above the threshold, simulate an interrupted connection
                if (stopAfter>-1 && totalUploaded>=stopAfter) {
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
            long elapsed=System.currentTimeMillis()-startTime;
            long wait=1000-elapsed;
            if (wait>0)
                try { Thread.sleep(wait); } catch (InterruptedException e) { }
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
	private static void readAlternateLocations(final String altHeader,
											   final AlternateLocationCollector alc) {
		final String alternateLocations = HTTPUtils.extractHeaderValue(altHeader);

		// return if the alternate locations could not be properly extracted
		if(alternateLocations == null) return;
		StringTokenizer st = new StringTokenizer(alternateLocations, ",");

		while(st.hasMoreTokens()) {
			try {
				// note that the trim method removes any CRLF character
				// sequences that may be used if the sender is using
				// continuations.
				AlternateLocation al = 
				    AlternateLocation.createAlternateLocation(st.nextToken().trim());
				alc.addAlternateLocation(al);
			} catch(IOException e) {
				// just return without adding it.
				continue;
			}
		}
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
