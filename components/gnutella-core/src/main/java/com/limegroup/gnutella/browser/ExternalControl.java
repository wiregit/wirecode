package com.limegroup.gnutella.browser;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;
import org.limewire.service.MessageService;
import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.Sockets;

public class ExternalControl {
    
    private static final Log LOG = LogFactory.getLog(ExternalControl.class);


	private static final String LOCALHOST       = "127.0.0.1"; 
	private static boolean      initialized     = false;
	private static volatile String       enqueuedRequest = null;

	public static String preprocessArgs(String args[]) {
	    LOG.trace("enter proprocessArgs");

        StringBuilder arg = new StringBuilder();
		for (int i = 0; i < args.length; i++) {
			arg.append(args[i]);
		}
		return arg.toString();
	}
	
    /**
     * Uses the magnet infrastructure to check if LimeWire is running.
     * If it is, it is restored and this instance exits.
     * Note that the already-running LimeWire is not checked
     * for 'allow multiple instances' -- only the instance that was just
     * started.
     */
	public static void checkForActiveLimeWire() {
	    if( testForLimeWire(null) ) {
		    System.exit(0);	
		}
	}

	public static void checkForActiveLimeWire(String arg) {
	    if ((OSUtils.isWindows() || OSUtils.isLinux()) && testForLimeWire(arg)) {
		    System.exit(0);	
		}
	}


	public static boolean  isInitialized() {
		return initialized;
	}
	public static void enqueueControlRequest(String arg) {
	    LOG.trace("enter enqueueControlRequest");
		enqueuedRequest = arg;
	}

	public static void runQueuedControlRequest() {
		initialized = true;
	    if ( enqueuedRequest != null ) {
			String request   = enqueuedRequest;
			enqueuedRequest = null;
			if (isTorrentRequest(request))
				handleTorrentRequest(request);
			else
				handleMagnetRequest(request);
		}
	}
	
	/**
	 * @return true if this is a torrent request.  
	 */
	private static boolean isTorrentRequest(String arg) {
		if (arg == null) 
			return false;
		arg = arg.trim().toLowerCase();
		// magnets pointing to .torrent files are just magnets for now
		return arg.endsWith(".torrent") && !arg.startsWith("magnet:");
	}
	
	//refactored the download logic into a separate method
	public static void handleMagnetRequest(String arg) {
	    LOG.trace("enter handleMagnetRequest");

	    ActivityCallback callback = restoreApplication();
        // No reason to make sure connections are active.  We don't even know
        // at this point if the magnet requires a search.
//		if ( RouterService.getNumInitializedConnections() <= 0 ) 
//		    RouterService.connect();



	    MagnetOptions options[] = MagnetOptions.parseMagnet(arg);

		if (options.length == 0) {
		    if(LOG.isWarnEnabled())
		        LOG.warn("Invalid magnet, ignoring: " + arg);
			return;
        }
		
		// ask callback if it wants to handle the magnets itself
		if (!callback.handleMagnets(options)) {
		    downloadMagnet(options);
		}
	}
	
	private static ActivityCallback restoreApplication() {
		ActivityCallback callback = RouterService.getCallback();
		callback.restoreApplication();
		callback.showDownloads();
		return callback;
	}
	
	private static void handleTorrentRequest(String arg) {
		LOG.trace("enter handleTorrentRequest");
		ActivityCallback callback = restoreApplication();
		File torrentFile = new File(arg.trim());
		callback.handleTorrent(torrentFile);
	}
	
	/**
	 * performs the actual magnet download.  This way it is possible to 
	 * parse and download the magnet separately (which is what I intend to do in the gui) --zab
	 * @param options the magnet options returned from parseMagnet
	 */
	public static void downloadMagnet(MagnetOptions[] options) {
		
		if(LOG.isDebugEnabled()) {
            for(int i = 0; i < options.length; i++) {
                LOG.debug("Kicking off downloader for option " + i +
                          " " + options[i]);
            }
        }                 

		for ( int i = 0; i < options.length; i++ ) {

			MagnetOptions curOpt = options[i];
			
		    if (LOG.isDebugEnabled()) {
				URN urn = curOpt.getSHA1Urn();
		        LOG.debug("Processing magnet with params:\n" +
		                  "urn [" + urn + "]\n" +
		                  "options [" + curOpt + "]");
            }

			String msg = curOpt.getErrorMessage();
			
            // Validate that we have something to go with from magnet
            // If not, report an error.
            if (!curOpt.isDownloadable()) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Invalid magnet: " + curOpt);
                }
				msg = msg != null ? msg : curOpt.toString();
                MessageService.showError("ERROR_BAD_MAGNET_LINK", msg);
                return;	
            }
            
            // Warn the user that the link was slightly invalid
            if( msg != null )
                MessageService.showError("ERROR_INVALID_URLS_IN_MAGNET");
            
            try {
            	RouterService.download(curOpt, false);
            }
            catch ( IllegalArgumentException il ) { 
			    ErrorService.error(il);
			}
			catch (SaveLocationException sle) {
				if (sle.getErrorCode() == SaveLocationException.FILE_ALREADY_EXISTS) {
                MessageService.showError(
                    "ERROR_ALREADY_EXISTS", sle.getFile().getName());
				}
				else if (sle.getErrorCode() == SaveLocationException.FILE_ALREADY_DOWNLOADING) {
					MessageService.showError(
		                    "ERROR_ALREADY_DOWNLOADING", sle.getFile().getName());	
				}
			}
		}
	}
	
	/**
	 *  Handle a Magnet request via a socket (for TCP handling).
	 *  Deiconify the application, fire MAGNET request
	 *  and return true as a sign that LimeWire is running.
	 */
	public static void fireControlThread(Socket socket, boolean magnet) {
	    LOG.trace("enter fireControl");
	    
        Thread.currentThread().setName("IncomingControlThread");
		try {
			// Only allow control from localhost
			if (!NetworkUtils.isLocalHost(socket)) {
                if(LOG.isWarnEnabled())
				    LOG.warn("Invalid control request from: " + socket.getInetAddress().getHostAddress());
				return;
            }

			// First read extra parameter
			socket.setSoTimeout(Constants.TIMEOUT);
			ByteReader br = new ByteReader(socket.getInputStream());
            // read the first line. if null, throw an exception
            String line = br.readLine();
			socket.setSoTimeout(0);

			BufferedOutputStream out =
			  new BufferedOutputStream(socket.getOutputStream());
			String s = CommonUtils.getUserName() + "\r\n";
			byte[] bytes=s.getBytes();
			out.write(bytes);
			out.flush();
			if (magnet)
				handleMagnetRequest(line);
			else
				handleTorrentRequest(line);
		} catch (IOException e) {
		    LOG.warn("Exception while responding to control request", e);
		} finally {
		    IOUtils.close(socket);
        }
	}

	

	/**  Check if the client is already running, and if so, pop it up.
	 *   Sends the MAGNET message along the given socket. 
	 *   @returns  true if a local LimeWire responded with a true.
	 */
	private static boolean testForLimeWire(String arg) {
		Socket socket = null;
		int port = ConnectionSettings.PORT.getValue();
		// Check to see if the port is valid.
		// If it is not, revert it to the default value.
		// This has the side effect of possibly allowing two 
		// LimeWires to start if somehow the existing one
		// set its port to 0, but that should not happen
		// in normal program flow.
		String type = isTorrentRequest(arg) ? "TORRENT" : "MAGNET";
		if( !NetworkUtils.isValidPort(port) ) {
		    ConnectionSettings.PORT.revertToDefault();
		    port = ConnectionSettings.PORT.getValue();
        }   
		try {
			socket = Sockets.connect(new InetSocketAddress(LOCALHOST, port), 500);
			InputStream istream = socket.getInputStream(); 
			socket.setSoTimeout(500); 
		    ByteReader byteReader = new ByteReader(istream);
		    OutputStream os = socket.getOutputStream();
		    OutputStreamWriter osw = new OutputStreamWriter(os);
		    BufferedWriter out = new BufferedWriter(osw);
		    out.write(type+" "+arg+" ");
		    out.write("\r\n");
		    out.flush();
		    String str = byteReader.readLine();
		    return(str != null && str.startsWith(CommonUtils.getUserName()));
		} catch (IOException e2) {
		} finally {
		    if(socket != null) {
		        try {
                    socket.close();
                } catch (IOException e) {
                    // nothing we can do
                }
            }
        }
        
	    return false;
	}
    
	/**
	 * Allows multiline parsing of magnet links.
	 * @param magnets
	 * @return array may be empty, but is never <code>null</code>
	 */
	public static MagnetOptions[] parseMagnets(String magnets) {
		List<MagnetOptions> list = new ArrayList<MagnetOptions>();
		StringTokenizer tokens = new StringTokenizer
			(magnets, System.getProperty("line.separator"));
		while (tokens.hasMoreTokens()) {
			String next = tokens.nextToken();
			MagnetOptions[] options = MagnetOptions.parseMagnet(next);
			if (options.length > 0) {
				list.addAll(Arrays.asList(options));			    
			}
		}
		return list.toArray(new MagnetOptions[0]);
	}
}
