package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.http.*;
import java.io.*;
import java.net.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.statistics.*;

/**
 * An implementaiton of the UploadState interface
 * when the request is to PushProxy
 */
public final class PushProxyUploadState implements HTTPMessage {
    
    private static final Log LOG = LogFactory.getLog(PushProxyUploadState.class);
	
    public static final String P_SERVER_ID = "ServerId";
    public static final String P_GUID = "guid";
    public static final String P_FILE = "file";
    
    private final HTTPUploader _uploader;

	private final ByteArrayOutputStream BAOS = 
		new ByteArrayOutputStream();
    
    public PushProxyUploadState(HTTPUploader uploader) {
    	LOG.debug("creating push proxy upload state");
		this._uploader = uploader;
    }
        
	public void writeMessageHeaders(OutputStream ostream) throws IOException {
		LOG.debug("writing headers");

        byte[] clientGUID  = GUID.fromHexString(_uploader.getFileName());
        InetAddress hostAddress = _uploader.getNodeAddress();
        int    hostPort    = _uploader.getNodePort();
        
        if ( clientGUID.length != 16 ||
             hostAddress == null ||
             !NetworkUtils.isValidPort(hostPort) ||
             !NetworkUtils.isValidAddress(hostAddress)) {
            // send back a 400
            String str = "HTTP/1.1 400 Push Proxy: Bad Request\r\n\r\n";
            ostream.write(str.getBytes());
            ostream.flush();
            debug("PPUS.doUpload(): unknown host.");
            UploadStat.PUSH_PROXY_REQ_BAD.incrementStat();
            return;
        }
        
        Map params = _uploader.getParameters();
        int fileIndex = 0; // default to 0.
        Object index = params.get(P_FILE);
        // set the file index if we know it...
        if( index != null )
            fileIndex = ((Integer)index).intValue();

        PushRequest push = new PushRequest(GUID.makeGuid(), (byte) 0,
                                           clientGUID, fileIndex, 
                                           hostAddress.getAddress(), hostPort);
        try {
            RouterService.getMessageRouter().sendPushRequest(push);
            
        }
        catch (IOException ioe) {
            // send back a 410
            String str="HTTP/1.1 410 Push Proxy: Servent not connected\r\n\r\n";
            ostream.write(str.getBytes());
            ostream.flush();
            debug("PPUS.doUpload(): push failed.");
            debug(ioe);
            UploadStat.PUSH_PROXY_REQ_FAILED.incrementStat();
            return;
        }
        
        UploadStat.PUSH_PROXY_REQ_SUCCESS.incrementStat();

        String str;
		str = "HTTP/1.1 202 Push Proxy: Message Sent\r\n";
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
		LOG.debug("writing body");
        ostream.write(BAOS.toByteArray());
        _uploader.setAmountUploaded(BAOS.size());
        debug("PPUS.doUpload(): returning.");
	}
	
	public boolean getCloseConnection() {
	    return false;
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
