package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.http.*;
import java.io.*;
import java.net.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * An implementaiton of the UploadState interface
 * when the request is to browse the host
 * @author Anurag Singla
 */
public final class PushProxyUploadState implements HTTPMessage {
    
    private final HTTPUploader _uploader;

	private final ByteArrayOutputStream BAOS = 
		new ByteArrayOutputStream();
    
    public PushProxyUploadState(HTTPUploader uploader) {
		this._uploader = uploader;
    }
        
	public void writeMessageHeaders(OutputStream ostream) throws IOException {

        String clientGUID  = _uploader.getFileName();
        String hostAddress = _uploader.getNodeAddress().trim();
        int    hostPort    = _uploader.getNodePort();

        try {
            InetAddress addr = InetAddress.getByName(hostAddress);
            PushRequest push = new PushRequest(GUID.makeGuid(), (byte) 0,
                                               GUID.fromHexString(clientGUID),
                                               0, addr.getAddress(), hostPort);
            RouterService.getMessageRouter().sendPushRequest(push);
            
        }
        catch (UnknownHostException uhe) {
            // send back a 400
            String str = "HTTP/1.1 400 PushProxy:Bad Request\r\n";
            ostream.write(str.getBytes());
            ostream.flush();
            debug("PPUS.doUpload(): unknown host.");
            debug(uhe);
            return;
        }
        catch (IllegalArgumentException iae) {
            // send back a 400
            String str = "HTTP/1.1 400 PushProxy:Bad Request\r\n";
            ostream.write(str.getBytes());
            ostream.flush();
            debug("PPUS.doUpload(): bad client guid.");
            debug(iae);
            return;
        }
        catch (IOException ioe) {
            // send back a 410
            String str = "HTTP/1.1 410 PushProxy:Servent not connected\r\n";
            ostream.write(str.getBytes());
            ostream.flush();
            debug("PPUS.doUpload(): push failed.");
            debug(ioe);
            return;
        }
        

        // first see if the MessageRouter is still connected to this guy....
     
        String str;
		str = "HTTP/1.1 202 PushProxy:Message Sent\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
		str = "Content-Type: " + Constants.QUERYREPLY_MIME_TYPE + "\r\n";
		ostream.write(str.getBytes());
	    str = "Content-Length: " + BAOS.size() + "\r\n";
		ostream.write(str.getBytes());
		str = "\r\n";
        ostream.write(str.getBytes());
	}

	public void writeMessageBody(OutputStream ostream) throws IOException {
        ostream.write(BAOS.toByteArray());
        _uploader.setAmountUploaded(BAOS.size());
        _uploader.setState(_uploader.COMPLETE);
        debug("PPUS.doUpload(): returning.");
	}

    /**
     * Tells if the upload state doesnt allow the connection to receive
     * another request on the same connection. This state doesnt allow
     * next request
     * @return always true
     */
    public boolean getCloseConnection() {
        return true;
    }

    private final static boolean debugOn = false;
    private final void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
    private final void debug(Exception out) {
        if (debugOn)
            out.printStackTrace();
    }

    
}
