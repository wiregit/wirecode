package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import com.sun.java.util.collections.*;
import java.util.StringTokenizer;

import com.limegroup.gnutella.connection.BIOMessageReader;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.settings.*;

/** Handles all stuff necessary for browsing of networks hosts. 
    Has a instance component, one per browse host, and a static Map of instances
    that is used to coordinate between replies to PushRequests.
 */
public class BrowseHostHandler {

    private static final long EXPIRE_TIME = 9000; // 9 seconds

    private static final int SPECIAL_INDEX = 0;

    /** Map from serventID to BrowseHostHandler instance.
     */
    private static Map _pushedHosts = new HashMap();

    /** The ActivityCallBack instance.  Used for talking to GUI.
     */
    private ActivityCallback _callback = null;

    /** The GUID to be used for incoming QRs from the Browse Request.
     */
    private GUID _guid = null;

    /** The GUID of the servent to send a Push to.  May be null if no push is
     * needed.
     */
    private GUID _serventID = null;

    static {
        Expirer expirer = new Expirer();
        RouterService.schedule(expirer, 0, 5000);// every 5 seconds
    }

    /**
     * @param callback A instance of a ActivityCallback, so I can notify it of
     * incoming QReps...
     * @param router A instance of a MessageRouter, so I can route messages if
     * needs be.
     * @param guid The GUID you have associated on the front end with the
     * results of this Browse Host request.
     * @param serventID May be null, non-null if I need to push
     */
    public  BrowseHostHandler(ActivityCallback callback, 
                              GUID guid, GUID serventID) {
        _callback = callback;
        _guid = guid;
        _serventID = serventID;
    }

    /** 
     * Browses the files on the specified host and port.
     *
     * @param host The IP of the host you want to browse.
     * @param port The port of the host you want to browse.
     * @param proxies the <tt>Set</tt> of push proxies to try
     */
    public void browseHost(String host, int port, Set proxies) {
        // flow of operation:
        // 1. check if you need to push.
        //   a. if so, just send a Push out.
        //   b. if not, try direct connect.  If it doesn't work, send a push.
        int shouldPush = needsPush(host);
        
        boolean shouldTryPush = false;
        switch (shouldPush) {
        case 0: // false
            try {
                // simply try connecting and getting results....
                Socket socket = Sockets.connect(host, port, 0);
                browseExchange(socket);
            }
            catch (IOException ioe) {
                // try pushing for fun.... (if we have the guid of the servent)
                shouldTryPush = true;
            }
            if (!shouldTryPush) 
                break;
        case 1: // true
            // if we're trying to push & we don't have a servent guid, it fails
            if ( _serventID == null ) {
                _callback.browseHostFailed(_guid);
            } 
            else {
                RemoteFileDesc fakeRFD = 
                    new RemoteFileDesc(host, port, SPECIAL_INDEX, "fake", 0, 
                                       _serventID.bytes(), 0, false, 0, false,
                                       null, null,false,false,"",0l, proxies);
                // register with the map so i get notified about a response to my
                // Push.
                synchronized (_pushedHosts) {
                    _pushedHosts.put(_serventID, new PushRequestDetails(this));
                }

                // send the Push after registering in case you get a response 
                // really quickly.  reuse code in DM cuz that works well
                if (!RouterService.getDownloadManager().sendPush(fakeRFD)) {
                    // didn't work, unregister yourself...
                    synchronized (_pushedHosts) {
                        _pushedHosts.remove(_serventID);
                    }
                    _callback.browseHostFailed(_guid);
                }
            }
            break;
        }
    }


    private void browseExchange(Socket socket) throws IOException {
        debug("BHH.browseExchange(): entered.");
        // first write the request...
        final String LF = "\r\n";
        String str = null;
        OutputStream oStream = socket.getOutputStream();
        debug("BHH.browseExchange(): got output stream.");

        // ask for the browse results..
        str = "GET / HTTP/1.1" + LF;
        oStream.write(str.getBytes());
        str = "Host: " + NetworkUtils.ip2string(RouterService.getAddress()) + 
              ":" + RouterService.getPort() + LF;
        oStream.write(str.getBytes());
        str = "User-Agent: " + CommonUtils.getVendor() + LF;
        oStream.write(str.getBytes());
        str = "Accept: " + Constants.QUERYREPLY_MIME_TYPE + LF;
        oStream.write(str.getBytes());
        str = "Content-Length: 0" + LF;
        oStream.write(str.getBytes());
        str = "Connection: close" + LF;
        oStream.write(str.getBytes());
        str = LF;
        oStream.write(str.getBytes());
        oStream.flush();
        debug("BHH.browseExchange(): wrote request A-OK.");
        
        // get the results...
        InputStream in = socket.getInputStream();
        debug("BHH.browseExchange(): got input stream.");

        // first check the HTTP code, encoding, etc...
        ByteReader br = new ByteReader(in);
        debug("BHH.browseExchange(): trying to get HTTP code....");
        int code = parseHTTPCode(br.readLine());
        if ((code < 200) || (code >= 300))
            throw new IOException();
        debug("BHH.browseExchange(): HTTP Response is " + code);

        // now confirm the content-type, the encoding, etc...
        boolean readingHTTP = true;
        String currLine = null;
        while (readingHTTP) {
            currLine = br.readLine();
            debug("BHH.browseExchange(): currLine = " + currLine);
            if ((currLine == null) || currLine.equals("")) {
                // start processing queries...
                readingHTTP = false;
            }
            else if (indexOfIgnoreCase(currLine, "Server") > -1)
                ; // just skip, who cares?
            else if (indexOfIgnoreCase(currLine, "Content-Type") > -1) {
                // make sure it is QRs....
                if (indexOfIgnoreCase(currLine, 
                                      Constants.QUERYREPLY_MIME_TYPE) < 0)
                    throw new IOException();
            }
            else if (indexOfIgnoreCase(currLine, "Content-Encoding") > -1) {
                throw new IOException();  //  decompress currently not supported
            }
        }
        debug("BHH.browseExchange(): read HTTP seemingly OK.");

        // ok, everything checks out, proceed and read QRs...
        Message m = null;
        while(true) {
            try {
                m = null;
                m = BIOMessageReader.read(in);
            }
            catch (BadPacketException bpe) {
            }
            catch (IOException bpe) {
                // thrown when stream is closed
            }
            if(m == null) 
                //we are finished reading the stream
                return;
            else {
                if(m instanceof QueryReply) {
                    debug("BHH.browseExchange(): read a QR");        
                    QueryReply reply = (QueryReply)m;
                    reply.setGUID(_guid);
					
					RouterService.getSearchResultHandler().handleQueryReply(reply);
                }
            }
        }        
    }


    /** Returns 1 iff rfd should be attempted by push download, either 
     *  because it is a private address or was unreachable in the past. 
     *  Returns 0 otherwise....
     */
    private static int needsPush(String host) {
        //Return true if rfd is private or unreachable
        try {
            if (ConnectionSettings.LOCAL_IS_PRIVATE.getValue() && 
                NetworkUtils.isPrivateAddress(host))
                return 1;
        } catch(UnknownHostException e) {
            // don't know, assume it's public
            return 0;
        }

        return 0;
    }


	/**
	 * a helper method to compare two strings 
	 * ignoring their case.
	 */ 
	private int indexOfIgnoreCase(String str, String section) {
		// convert both strings to lower case
		String aaa = str.toLowerCase();
		String bbb = section.toLowerCase();
		// then look for the index...
		return aaa.indexOf(bbb);
	}

    /**
     * Returns the HTTP response code from the given string, throwing
     * an exception if it couldn't be parsed.
     *
     * @param str an HTTP response string, e.g., "HTTP 200 OK \r\n"
     * @exception IOException a problem occurred
     */
    private static int parseHTTPCode(String str) throws IOException {		
        if (str == null)
            return -1; // hopefully this won't happen, but if so just error...
		StringTokenizer tokenizer = new StringTokenizer(str, " ");		
		String token;

		// just a safety
		if (! tokenizer.hasMoreTokens() )
			throw new IOException();

		token = tokenizer.nextToken();
		
		// the first token should contain HTTP
		if (token.toUpperCase().indexOf("HTTP") < 0 )
			throw new IOException();
		
		// the next token should be a number
		// just a safety
		if (! tokenizer.hasMoreTokens() )
			throw new IOException();

		token = tokenizer.nextToken();
		
		String num = token.trim();
		try {
            debug("BHH.parseHTTPCode(): returning " + num);
			return java.lang.Integer.parseInt(num);
		} catch (NumberFormatException e) {
			throw new IOException();
		}

    }



    /** @return true if the Push was handled by me.
     */
    public static boolean handlePush(int index, GUID serventID, Socket socket) 
        throws IOException {
        boolean retVal = false;
        debug("BHH.handlePush(): entered.");
        if (index == SPECIAL_INDEX)
            ; // you'd hope, but not necessary...

        PushRequestDetails prd = null;
        synchronized (_pushedHosts) {
            prd = (PushRequestDetails) _pushedHosts.remove(serventID);
        }
        if (prd != null) {
            prd.bhh.browseExchange(socket);
            retVal = true;
        }
        else
            debug("BHH.handlePush(): no matching BHH.");

        debug("BHH.handlePush(): returning.");
        return retVal;
    }

    /** Can be run to invalidate pushes that we are waiting for....
     */
    private static class Expirer implements Runnable {
        public void run() {
            try {
                Iterator keys = null;
                Set toRemove = new HashSet();
                synchronized (_pushedHosts) {
                    keys = _pushedHosts.keySet().iterator();
                    while (keys.hasNext()) {
                        Object currKey = keys.next();
                        PushRequestDetails currPRD = null;
                        currPRD = (PushRequestDetails) _pushedHosts.get(currKey);
                        if ((currPRD != null) && (currPRD.isExpired())) {
                            debug("Expirer.run(): expiring a badboy.");
                            toRemove.add(currKey);
                            currPRD.bhh._callback.browseHostFailed(currPRD.bhh._guid);
                        }
                    }
                    // done iterating through _pushedHosts, remove the keys now...
                    keys = toRemove.iterator();
                    while (keys.hasNext())
                        _pushedHosts.remove(keys.next());
                }
            } catch(Throwable t) {
                ErrorService.error(t);
            }
        }
    }

    private static class PushRequestDetails { 
        private BrowseHostHandler bhh;
        private long timeStamp;
        
        public PushRequestDetails(BrowseHostHandler bhh) {
            timeStamp = System.currentTimeMillis();
            this.bhh = bhh;
        }

        public boolean isExpired() {
            return ((System.currentTimeMillis() - timeStamp) > EXPIRE_TIME);
        }
    }

    private final static boolean debugOn = false;
    private final static void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
    private final static void debug(Exception out) {
        if (debugOn)
            out.printStackTrace();
    }

}
