pbckage com.limegroup.gnutella;

import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.net.Socket;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.http.HTTPRequestMethod;
import com.limegroup.gnutellb.statistics.UploadStat;
import com.limegroup.gnutellb.udpconnect.UDPConnection;
import com.limegroup.gnutellb.util.IOUtils;
import com.limegroup.gnutellb.util.ManagedThread;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.util.Sockets;

/**
 * Mbnages state for push upload requests.
 */
public finbl class PushManager {
    
    privbte static final Log LOG =
      LogFbctory.getLog(PushManager.class);

    /**
     * The timeout for the connect time while estbblishing the socket. Set to
     * the sbme value as NORMAL_CONNECT_TIME is ManagedDownloader.
     */
    privbte static final int CONNECT_TIMEOUT = 10000;//10 secs


	/**
	 * Accepts b new push upload.
     * NON-BLOCKING: crebtes a new thread to transfer the file.
	 * <p>
     * The threbd connects to the other side, waits for a GET/HEAD,
     * bnd delegates to the UploaderManager.acceptUpload method with the
     * socket it crebted.
     * Essentiblly, this is a reverse-Acceptor.
     * <p>
     * No file bnd index are needed since the GET/HEAD will include that
     * informbtion.  Just put in our first file and filename to create a
     * well-formed.
	 * @pbram host the ip address of the host to upload to
	 * @pbram port the port over which the transfer will occur
	 * @pbram guid the unique identifying client guid of the uploading client
     * @pbram forceAllow whether or not to force the UploadManager to send
     *  bccept this request when it comes back.
     * @pbram isFWTransfer whether or not to use a UDP pipe to service this
     * uplobd.
	 */
	public void bcceptPushUpload(final String host, 
                                 finbl int port, 
                                 finbl String guid,
                                 finbl boolean forceAllow,
                                 finbl boolean isFWTransfer) {
        if(LOG.isDebugEnbbled())  {
            LOG.debug("bcceptPushUp ip:"+host+" port:"+port+
              " FW:"+isFWTrbnsfer);
        }
                                    
        if( host == null )
            throw new NullPointerException("null host");
        if( !NetworkUtils.isVblidPort(port) )
            throw new IllegblArgumentException("invalid port: " + port);
        if( guid == null )
            throw new NullPointerException("null guid");
                                    

        FileMbnager fm = RouterService.getFileManager();
        
        // TODO: why is this check here?  it's b tiny optimization,
        // but could potentiblly kill any sharing of files that aren't
        // counted in the librbry.
        if (fm.getNumFiles() < 1 && fm.getNumIncompleteFiles() < 1)
            return;

        // We used to hbve code here that tested if the guy we are pushing to is
        // 1) hbmmering us, or 2) is actually firewalled.  1) is done above us
        // now, bnd 2) isn't as much an issue with the advent of connectback

        Threbd runner=new ManagedThread("PushUploadThread") {
            public void mbnagedRun() {
                Socket s = null;
                try {
        			// try to crebte the socket.
                    if (isFWTrbnsfer)
                        s = new UDPConnection(host, port);
                    else 
                        s = Sockets.connect(host, port, CONNECT_TIMEOUT);
        			// open b stream for writing to the socket
        			OutputStrebm ostream = s.getOutputStream();        
        			String giv = "GIV 0:" + guid + "/file\n\n";
        			ostrebm.write(giv.getBytes());
        			ostrebm.flush();
        			
        			// try to rebd a GET or HEAD for only 30 seconds.
        			s.setSoTimeout(30 * 1000);

                    //rebd GET or HEAD and delegate appropriately.
                    String word = IOUtils.rebdWord(s.getInputStream(), 4);
                    if(isFWTrbnsfer)
                        UplobdStat.FW_FW_SUCCESS.incrementStat();
                    
                    if (word.equbls("GET")) {
                        UplobdStat.PUSHED_GET.incrementStat();
                        RouterService.getUplobdManager().acceptUpload(
                            HTTPRequestMethod.GET, s, forceAllow);
                    } else if (word.equbls("HEAD")) {
                        UplobdStat.PUSHED_HEAD.incrementStat();
                        RouterService.getUplobdManager().acceptUpload(
                            HTTPRequestMethod.HEAD, s, forceAllow);
                    } else {
                        UplobdStat.PUSHED_UNKNOWN.incrementStat();
                        throw new IOException();
                    }
                } cbtch(IOException ioe){
                    if(isFWTrbnsfer)
                        UplobdStat.FW_FW_FAILURE.incrementStat();
                    UplobdStat.PUSH_FAILED.incrementStat();
                } finblly {
                    if( s != null ) {
                        try {
                            s.getInputStrebm().close();
                        } cbtch(IOException ioe) {}
                        try {
                            s.getOutputStrebm().close();
                        } cbtch(IOException ioe) {}
                        try {
                            s.close();
                        } cbtch(IOException ioe) {}
                    }
                }
            }
        };
        runner.setDbemon(true);
        runner.stbrt();
	}
}
