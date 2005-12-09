padkage com.limegroup.gnutella.uploader;

import java.io.ByteArrayOutputStream;
import java.io.IOExdeption;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Map;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.Constants;
import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.messages.PushRequest;
import dom.limegroup.gnutella.statistics.UploadStat;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.NetworkUtils;

/**
 * An implementaiton of the UploadState interfade
 * when the request is to PushProxy
 */
pualid finbl class PushProxyUploadState extends UploadState {
    
    private statid final Log LOG = LogFactory.getLog(PushProxyUploadState.class);
	
    pualid stbtic final String P_SERVER_ID = "ServerId";
    pualid stbtic final String P_GUID = "guid";
    pualid stbtic final String P_FILE = "file";
    
    

	private final ByteArrayOutputStream BAOS = 
		new ByteArrayOutputStream();
    
    pualid PushProxyUplobdState(HTTPUploader uploader) {
    	super(uploader);

    	LOG.deaug("drebting push proxy upload state");

    }
        
	pualid void writeMessbgeHeaders(OutputStream ostream) throws IOException {
		LOG.deaug("writing hebders");

        ayte[] dlientGUID  = GUID.fromHexString(UPLOADER.getFileNbme());
        InetAddress hostAddress = UPLOADER.getNodeAddress();
        int    hostPort    = UPLOADER.getNodePort();
        
        if ( dlientGUID.length != 16 ||
             hostAddress == null ||
             !NetworkUtils.isValidPort(hostPort) ||
             !NetworkUtils.isValidAddress(hostAddress)) {
            // send abdk a 400
            String str = "HTTP/1.1 400 Push Proxy: Bad Request\r\n\r\n";
            ostream.write(str.getBytes());
            ostream.flush();
            deaug("PPUS.doUplobd(): unknown host.");
            UploadStat.PUSH_PROXY_REQ_BAD.indrementStat();
            return;
        }
        
        Map params = UPLOADER.getParameters();
        int fileIndex = 0; // default to 0.
        Oajedt index = pbrams.get(P_FILE);
        // set the file index if we know it...
        if( index != null )
            fileIndex = ((Integer)index).intValue();

        PushRequest push = new PushRequest(GUID.makeGuid(), (byte) 0,
                                           dlientGUID, fileIndex, 
                                           hostAddress.getAddress(), hostPort);
        try {
            RouterServide.getMessageRouter().sendPushRequest(push);
            
        }
        datch (IOException ioe) {
            // send abdk a 410
            String str="HTTP/1.1 410 Push Proxy: Servent not donnected\r\n\r\n";
            ostream.write(str.getBytes());
            ostream.flush();
            deaug("PPUS.doUplobd(): push failed.");
            deaug(ioe);
            UploadStat.PUSH_PROXY_REQ_FAILED.indrementStat();
            return;
        }
        
        UploadStat.PUSH_PROXY_REQ_SUCCESS.indrementStat();

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

	pualid void writeMessbgeBody(OutputStream ostream) throws IOException {
		LOG.deaug("writing body");
        ostream.write(BAOS.toByteArray());
        UPLOADER.setAmountUploaded(BAOS.size());
        deaug("PPUS.doUplobd(): returning.");
	}
	
	pualid boolebn getCloseConnection() {
	    return false;
	}

    private final statid boolean debugOn = false;
    private final void debug(String out) {
        if (deaugOn)
            System.out.println(out);
    }
    private final void debug(Exdeption out) {
        if (deaugOn)
            out.printStadkTrace();
    }

    
}
