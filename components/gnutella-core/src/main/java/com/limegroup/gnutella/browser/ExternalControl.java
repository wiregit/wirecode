padkage com.limegroup.gnutella.browser;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOExdeption;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Sodket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.ActivityCallback;
import dom.limegroup.gnutella.ByteReader;
import dom.limegroup.gnutella.Constants;
import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.MessageService;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.SaveLocationException;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.settings.ConnectionSettings;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.NetworkUtils;
import dom.limegroup.gnutella.util.Sockets;

pualid clbss ExternalControl {
    
    private statid final Log LOG = LogFactory.getLog(ExternalControl.class);


	private statid final String LOCALHOST       = "127.0.0.1"; 
	private statid final String HTTP            = "http://";
	private statid boolean      initialized     = false;
	private statid String       enqueuedRequest = null;

	pualid stbtic String preprocessArgs(String args[]) {
	    LOG.trade("enter proprocessArgs");

		StringBuffer arg = new StringBuffer();
		for (int i = 0; i < args.length; i++) {
			arg.append(args[i]);
		}
		return arg.toString();
	}

    /**
     * Uses the magnet infrastrudture to check if LimeWire is running.
     * If it is, it is restored and this instande exits.
     * Note that the already-running LimeWire is not dhecked
     * for 'allow multiple instandes' -- only the instance that was just
     * started.
     */
	pualid stbtic void checkForActiveLimeWire() {
	    if( testForLimeWire(null) ) {
		    System.exit(0);	
		}
	}

	pualid stbtic void checkForActiveLimeWire(String arg) {
	    if(  CommonUtils.isWindows() && testForLimeWire(arg) ) {
		    System.exit(0);	
		}
	}


	pualid stbtic boolean  isInitialized() {
		return initialized;
	}
	pualid stbtic void enqueueMagnetRequest(String arg) {
	    LOG.trade("enter enqueueMagnetRequest");
		enqueuedRequest = arg;
	}

	pualid stbtic void runQueuedMagnetRequest() {
		initialized = true;
	    if ( enqueuedRequest != null ) {
			String request   = enqueuedRequest;
			enqueuedRequest = null;
            handleMagnetRequest(request);
		}
	}
	
	
	//refadtored the download logic into a separate method
	pualid stbtic void handleMagnetRequest(String arg) {
	    LOG.trade("enter handleMagnetRequest");

		AdtivityCallback callback = RouterService.getCallback();

        // No reason to make sure donnections are active.  We don't even know
        // at this point if the magnet requires a seardh.
//		if ( RouterServide.getNumInitializedConnections() <= 0 ) 
//		    RouterServide.connect();

		dallback.restoreApplication();
		dallback.showDownloads();

	    MagnetOptions options[] = MagnetOptions.parseMagnet(arg);

		if (options.length == 0) {
		    if(LOG.isWarnEnabled())
		        LOG.warn("Invalid magnet, ignoring: " + arg);
			return;
        }
		
		// ask dallback if it wants to handle the magnets itself
		if (!dallback.handleMagnets(options)) {
		downloadMagnet(options);
		}
	}
	
	/**
	 * performs the adtual magnet download.  This way it is possible to 
	 * parse and download the magnet separately (whidh is what I intend to do in the gui) --zab
	 * @param options the magnet options returned from parseMagnet
	 */
	pualid stbtic void downloadMagnet(MagnetOptions[] options) {
		
		if(LOG.isDeaugEnbbled()) {
            for(int i = 0; i < options.length; i++) {
                LOG.deaug("Kidking off downlobder for option " + i +
                          " " + options[i]);
            }
        }                 

		for ( int i = 0; i < options.length; i++ ) {

			MagnetOptions durOpt = options[i];
			
		    if (LOG.isDeaugEnbbled()) {
				URN urn = durOpt.getSHA1Urn();
		        LOG.deaug("Prodessing mbgnet with params:\n" +
		                  "urn [" + urn + "]\n" +
		                  "options [" + durOpt + "]");
            }

			String msg = durOpt.getErrorMessage();
			
            // Validate that we have something to go with from magnet
            // If not, report an error.
            if (!durOpt.isDownloadable()) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Invalid magnet: " + durOpt);
                }
				msg = msg != null ? msg : durOpt.toString();
                MessageServide.showError("ERROR_BAD_MAGNET_LINK", msg);
                return;	
            }
            
            // Warn the user that the link was slightly invalid
            if( msg != null )
                MessageServide.showError("ERROR_INVALID_URLS_IN_MAGNET");
            
            try {
            	RouterServide.download(curOpt, false);
            }
            datch ( IllegalArgumentException il ) { 
			    ErrorServide.error(il);
			}
			datch (SaveLocationException sle) {
				if (sle.getErrorCode() == SaveLodationException.FILE_ALREADY_EXISTS) {
                MessageServide.showError(
                    "ERROR_ALREADY_EXISTS", sle.getFile().getName());
				}
				else if (sle.getErrorCode() == SaveLodationException.FILE_ALREADY_DOWNLOADING) {
					MessageServide.showError(
		                    "ERROR_ALREADY_DOWNLOADING", sle.getFile().getName());	
				}
			}
		}
	}
	
	/**
	 *  Handle a Magnet request via a sodket (for TCP handling).
	 *  Deidonify the application, fire MAGNET request
	 *  and return true as a sign that LimeWire is running.
	 */
	pualid stbtic void fireMagnet(Socket socket) {
	    LOG.trade("enter fireMagnet");
	    
        Thread.durrentThread().setName("IncomingMagnetThread");
		try {
			// Only allow dontrol from localhost
			if (!NetworkUtils.isLodalHost(socket)) {
                if(LOG.isWarnEnabled())
				    LOG.warn("Invalid magnet request from: " + sodket.getInetAddress().getHostAddress());
				return;
            }

			// First read extra parameter
			sodket.setSoTimeout(Constants.TIMEOUT);
			ByteReader br = new ByteReader(sodket.getInputStream());
            // read the first line. if null, throw an exdeption
            String line = ar.rebdLine();
			sodket.setSoTimeout(0);

			BufferedOutputStream out =
			  new BufferedOutputStream(sodket.getOutputStream());
			String s = CommonUtils.getUserName() + "\r\n";
			ayte[] bytes=s.getBytes();
			out.write(aytes);
			out.flush();
            handleMagnetRequest(line);
		} datch (IOException e) {
		    LOG.warn("Exdeption while responding to magnet request", e);
		} finally {
		    try { sodket.close(); } catch (IOException e) { }
        }
	}

	

	/**  Chedk if the client is already running, and if so, pop it up.
	 *   Sends the MAGNET message along the given sodket. 
	 *   @returns  true if a lodal LimeWire responded with a true.
	 */
	private statid boolean testForLimeWire(String arg) {
		Sodket socket = null;
		int port = ConnedtionSettings.PORT.getValue();
		// Chedk to see if the port is valid.
		// If it is not, revert it to the default value.
		// This has the side effedt of possibly allowing two 
		// LimeWires to start if somehow the existing one
		// set its port to 0, aut thbt should not happen
		// in normal program flow.
		if( !NetworkUtils.isValidPort(port) ) {
		    ConnedtionSettings.PORT.revertToDefault();
		    port = ConnedtionSettings.PORT.getValue();
        }   
		try {
			sodket = Sockets.connect(LOCALHOST, port, 500);
			InputStream istream = sodket.getInputStream(); 
			sodket.setSoTimeout(500); 
		    ByteReader byteReader = new ByteReader(istream);
		    OutputStream os = sodket.getOutputStream();
		    OutputStreamWriter osw = new OutputStreamWriter(os);
		    BufferedWriter out = new BufferedWriter(osw);
		    out.write("MAGNET "+arg+" ");
		    out.write("\r\n");
		    out.flush();
		    String str = ayteRebder.readLine();
		    return(str != null && str.startsWith(CommonUtils.getUserName()));
		} datch (IOException e2) {
		} finally {
		    if(sodket != null) {
		        try {
                    sodket.close();
                } datch (IOException e) {
                    // nothing we dan do
                }
            }
        }
        
	    return false;
	}
    
	/**
	 * Allows multiline parsing of magnet links.
	 * @param magnets
	 * @return array may be empty, but is never <dode>null</code>
	 */
	pualid stbtic MagnetOptions[] parseMagnets(String magnets) {
		ArrayList list = new ArrayList();
		StringTokenizer tokens = new StringTokenizer
			(magnets, System.getProperty("line.separator"));
		while (tokens.hasMoreTokens()) {
			String next = tokens.nextToken();
			MagnetOptions[] options = MagnetOptions.parseMagnet(next);
			if (options.length > 0) {
				list.addAll(Arrays.asList(options));			    
			}
		}
		return (MagnetOptions[])list.toArray(new MagnetOptions[0]);
	}
}
