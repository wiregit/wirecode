package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import com.sun.java.util.collections.*;
import java.util.StringTokenizer;
import com.limegroup.gnutella.util.*;

/** Handles all stuff necessary for browsing of networks hosts. 
    Has a instance component, one per browse host, and a static Map of instances
    that is used to coordinate between replies to PushRequests.
 */
public class BrowseHostHandler {

    private static final int SPECIAL_INDEX = 4141414;

    /** Map from serventID to BrowseHostHandler instance.
     */
    private static Map _pushedHosts = new HashMap();

    /** The ActivityCallBack instance.  Used for talking to GUI.
     */
    private ActivityCallback _callback = null;

    /** The MessageRouter instance.  Used for sending a Push.
     */
    private MessageRouter _router = null;

    /** The Acceptor instance.  Used for getting system props...
     */
    private Acceptor _acceptor = null;

    /** The GUID to be used for incoming QRs from the Browse Request.
     */
    private GUID _guid = null;

    /** The GUID of the servent to send a Push to.  May be null if no push is
     * needed.
     */
    private GUID _serventID = null;

    /**
     * @param callback A instance of a ActivityCallback, so I can notify it of
     * incoming QReps...
     * @param router A instance of a MessageRouter, so I can route messages if
     * needs be.
     * @param acceptor A instance of the Acceptor, need it for accepting :)
     * @param guid The GUID you have associated on the front end with the
     * results of this Browse Host request.
     * @param serventID May be null, non-null if I need to push
     */
    public  BrowseHostHandler(ActivityCallback callback, MessageRouter router,
                              Acceptor acceptor, GUID guid, GUID serventID) {
        _callback = callback;
        _router = router;
        _acceptor = acceptor;
        _guid = guid;
        _serventID = serventID;
    }

    /** 
     * @param host The IP of the host you want to browse.
     * @param port The port of the host you want to browse.
     */
    public void browseHost(String host, int port) throws IOException {
        
        // should we push?
        if (_serventID != null && false) {
            PushRequest pr = new PushRequest(GUID.makeGuid(),
                                             SettingsManager.instance().getTTL(),
                                             _serventID.bytes(), 
                                             SPECIAL_INDEX,
                                             _acceptor.getAddress(),
                                             _acceptor.getPort());
            _router.sendPushRequest(pr);
            // register with the map so i get notified about a response to my
            // Push.
            synchronized (_pushedHosts) {
                _pushedHosts.put(_serventID, this);
            }
        }
        else {
            try {
                // simply try connecting and getting results....
                Socket socket = new Socket(host, port);
                browseExchange(socket);
            }
            catch (UnknownHostException uhe) {
                throw new IOException();
            }
        }
    }


    private void browseExchange(Socket socket) throws IOException {
        debug("BHH.browseExchange(): entered.");
        // first write the request...
        final String LF = "\r\n";
        String str = null;
        OutputStream oStream = socket.getOutputStream();

        // ask for the browse results..
        str = "GET / HTTP/1.1" + LF;
        oStream.write(str.getBytes());
        str = "Host: " + _acceptor.getAddress() + ":" + _acceptor.getPort() + LF;
        oStream.write(str.getBytes());
        str = "User-Agent: " + CommonUtils.getVendor() + LF;
        oStream.write(str.getBytes());
        str = "Accept: " + Constants.QUERYREPLY_MIME_TYPE + LF;
        oStream.write(str.getBytes());
        str = "Content-Length: 0" + LF;
        oStream.write(str.getBytes());
        str = "Connection: close" + LF;
        oStream.write(str.getBytes());
        oStream.flush();
        debug("BHH.browseExchange(): wrote request A-OK.");
        
        // get the results...
        InputStream in = socket.getInputStream();

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
        while (readingHTTP && in.markSupported()) {
            in.mark(100);
            currLine = br.readLine();
            debug("BHH.browseExchange(): currLine = " + currLine);
            if (indexOfIgnoreCase(currLine, "User-Agent") > -1)
                ; // just skip, who cares?
            else if (indexOfIgnoreCase(currLine, "Content-Type") > -1) {
                // make sure it is QRs....
                if (indexOfIgnoreCase(currLine, 
                                      Constants.QUERYREPLY_MIME_TYPE) < 0)
                    throw new IOException();
            }
            else if (indexOfIgnoreCase(currLine, "Content-Encoding") > -1) {
                // make sure it is QRs....
                throw new IOException();  //  decompress currently not supported
            }
            else {
                // start processing queries...
                in.reset();
                readingHTTP = false;
            }
        }
        debug("BHH.browseExchange(): read HTTP seemingly OK.");

        // ok, everything checks out, proceed...
        Message m = null;
        while(true) {
            try {
                m = null;
                m = Message.read(in);
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
                    QueryReply queryReply = (QueryReply)m;
                    m.setGUID(_guid);
                    if (_callback != null)
                        _callback.handleQueryReply(queryReply);
                }
            }
        }        
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



    public static void handlePush(int index, GUID serventID, Socket socket) 
        throws IOException {

        if (index == SPECIAL_INDEX)
            ; // you'd hope, but not necessary...

        BrowseHostHandler handler = null;
        synchronized (_pushedHosts) {
            handler = (BrowseHostHandler) _pushedHosts.remove(serventID);
        }
        if (handler != null) 
            handler.browseExchange(socket);
    }


    private final static boolean debugOn = true;
    private final static void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
    private final static void debug(Exception out) {
        if (debugOn)
            out.printStackTrace();
    }

}
