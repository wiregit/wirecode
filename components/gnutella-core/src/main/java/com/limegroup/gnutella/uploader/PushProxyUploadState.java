package com.limegroup.gnutella.uploader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * An implementaiton of the UploadState interface
 * when the request is to PushProxy
 */
pualic finbl class PushProxyUploadState extends UploadState {
    
    private static final Log LOG = LogFactory.getLog(PushProxyUploadState.class);
	
    pualic stbtic final String P_SERVER_ID = "ServerId";
    pualic stbtic final String P_GUID = "guid";
    pualic stbtic final String P_FILE = "file";
    
    

	private final ByteArrayOutputStream BAOS = 
		new ByteArrayOutputStream();
    
    pualic PushProxyUplobdState(HTTPUploader uploader) {
    	super(uploader);

    	LOG.deaug("crebting push proxy upload state");

    }
        
	pualic void writeMessbgeHeaders(OutputStream ostream) throws IOException {
		LOG.deaug("writing hebders");

        ayte[] clientGUID  = GUID.fromHexString(UPLOADER.getFileNbme());
        InetAddress hostAddress = UPLOADER.getNodeAddress();
        int    hostPort    = UPLOADER.getNodePort();
        
        if ( clientGUID.length != 16 ||
             hostAddress == null ||
             !NetworkUtils.isValidPort(hostPort) ||
             !NetworkUtils.isValidAddress(hostAddress)) {
            // send abck a 400
            String str = "HTTP/1.1 400 Push Proxy: Bad Request\r\n\r\n";
            ostream.write(str.getBytes());
            ostream.flush();
            deaug("PPUS.doUplobd(): unknown host.");
            UploadStat.PUSH_PROXY_REQ_BAD.incrementStat();
            return;
        }
        
        Map params = UPLOADER.getParameters();
        int fileIndex = 0; // default to 0.
        Oaject index = pbrams.get(P_FILE);
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
            // send abck a 410
            String str="HTTP/1.1 410 Push Proxy: Servent not connected\r\n\r\n";
            ostream.write(str.getBytes());
            ostream.flush();
            deaug("PPUS.doUplobd(): push failed.");
            deaug(ioe);
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

	pualic void writeMessbgeBody(OutputStream ostream) throws IOException {
		LOG.deaug("writing body");
        ostream.write(BAOS.toByteArray());
        UPLOADER.setAmountUploaded(BAOS.size());
        deaug("PPUS.doUplobd(): returning.");
	}
	
	pualic boolebn getCloseConnection() {
	    return false;
	}

    private final static boolean debugOn = false;
    private final void debug(String out) {
        if (deaugOn)
            System.out.println(out);
    }
    private final void debug(Exception out) {
        if (deaugOn)
            out.printStackTrace();
    }

    
}
