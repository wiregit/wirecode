package com.limegroup.gnutella.browser;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.MessageService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.Sockets;
import com.limegroup.gnutella.util.URLDecoder;

public class ExternalControl {
    
    private static final Log LOG = LogFactory.getLog(ExternalControl.class);


	private static final String LOCALHOST       = "127.0.0.1"; 
	private static final String HTTP            = "http://";
	private static boolean      initialized     = false;
	private static String       enqueuedRequest = null;

	public static String preprocessArgs(String args[]) {
	    LOG.trace("enter proprocessArgs");

		String arg = new String();
		for(int i = 0; i < args.length; i++) {
			arg += args[i];
		}
		return arg;
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
	    if(  CommonUtils.isWindows() && testForLimeWire(arg) ) {
		    System.exit(0);	
		}
	}


	public static boolean  isInitialized() {
		return initialized;
	}
	public static void enqueueMagnetRequest(String arg) {
	    LOG.trace("enter enqueueMagnetRequest");
		enqueuedRequest = arg;
	}

	public static void runQueuedMagnetRequest() {
		initialized = true;
	    if ( enqueuedRequest != null ) {
			String request   = enqueuedRequest;
			enqueuedRequest = null;
            handleMagnetRequest(request);
		}
	}
	
	
	//refactored the download logic into a separate method
	public static void handleMagnetRequest(String arg) {
	    LOG.trace("enter handleMagnetRequest");

		ActivityCallback callback = RouterService.getCallback();

		// Make sure that connections are active
		if ( RouterService.getNumInitializedConnections() <= 0 ) 
		    RouterService.connect();

		callback.restoreApplication();
		callback.showDownloads();

	    MagnetOptions options[] = parseMagnet(arg);

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
	public static void fireMagnet(Socket socket) {
	    LOG.trace("enter fireMagnet");
	    
        Thread.currentThread().setName("IncomingMagnetThread");
		try {
			// Only allow control from localhost
			if (!NetworkUtils.isLocalHost(socket)) {
                if(LOG.isWarnEnabled())
				    LOG.warn("Invalid magnet request from: " + socket.getInetAddress().getHostAddress());
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
            handleMagnetRequest(line);
		} catch (IOException e) {
		    LOG.warn("Exception while responding to magnet request", e);
		} finally {
		    try { socket.close(); } catch (IOException e) { }
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
		if( !NetworkUtils.isValidPort(port) ) {
		    ConnectionSettings.PORT.revertToDefault();
		    port = ConnectionSettings.PORT.getValue();
        }   
		try {
			socket = Sockets.connect(LOCALHOST, port, 500);
			InputStream istream = socket.getInputStream(); 
			socket.setSoTimeout(500); 
		    ByteReader byteReader = new ByteReader(istream);
		    OutputStream os = socket.getOutputStream();
		    OutputStreamWriter osw = new OutputStreamWriter(os);
		    BufferedWriter out = new BufferedWriter(osw);
		    out.write("MAGNET "+arg+" ");
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
		ArrayList list = new ArrayList();
		StringTokenizer tokens = new StringTokenizer
			(magnets, System.getProperty("line.separator"));
		while (tokens.hasMoreTokens()) {
			String next = tokens.nextToken();
			MagnetOptions[] options = parseMagnet(next);
			if (options.length > 0) {
				list.addAll(Arrays.asList(options));
			}
		}
		return (MagnetOptions[])list.toArray(new MagnetOptions[0]);
	}
	
	/**
	 * Returns an empty array if the string could not be parsed.
	 * @param arg
	 * @return array may be empty, but is never <code>null</code>
	 */
	public static MagnetOptions[] parseMagnet(String arg) {
	    LOG.trace("enter parseMagnet");
		HashMap options = new HashMap();

		// Strip out any single quotes added to escape the string
		if ( arg.startsWith("'") )
			arg = arg.substring(1);
		if ( arg.endsWith("'") )
			arg = arg.substring(0,arg.length()-1);
		
		// Parse query  -  TODO: case sensitive?
		if ( !arg.startsWith(MagnetOptions.MAGNET) )
			return new MagnetOptions[0];

		// Parse and assemble magnet options together.
		//
		arg = arg.substring(8);
		StringTokenizer st = new StringTokenizer(arg, "&");
		String          keystr;
		String          cmdstr;
		int             start;
		int             index;
		Integer         iIndex;
		int             periodLoc;
		MagnetOptions   curOptions;
		
		// Process each key=value pair
     	while (st.hasMoreTokens()) {
		    keystr = st.nextToken();
			keystr = keystr.trim();
			start  = keystr.indexOf("=")+1;
			if(start == 0) continue; // no '=', ignore.
		    cmdstr = keystr.substring(start);
			keystr = keystr.substring(0,start-1);
            try {
                cmdstr = URLDecoder.decode(cmdstr);
            } catch (IOException e1) {
                continue;
            }
			// Process any numerical list of cmds
			if ( (periodLoc = keystr.indexOf(".")) > 0 ) {
				try {
			        index = Integer.parseInt(keystr.substring(periodLoc+1));
				} catch (NumberFormatException e) {
					continue;
				}
			} else {
				index = 0;
			}
			// Add to any existing options
			iIndex = new Integer(index);
			curOptions = (MagnetOptions) options.get(iIndex);			
			if (curOptions == null) 
				curOptions = new MagnetOptions();

			if ( keystr.startsWith("xt") ) {
				curOptions.addExactTopic(cmdstr);
			} else if ( keystr.startsWith("dn") ) {
				curOptions.setDisplayName(cmdstr);
			} else if ( keystr.startsWith("kt") ) {
				curOptions.setKeywordTopic(cmdstr);
			} else if ( keystr.startsWith("xs") ) {
				curOptions.addXS(cmdstr);
			} else if ( keystr.startsWith("as") ) {
				curOptions.addAS(cmdstr);
			}
			
			options.put(iIndex,curOptions);
		}
		
		return (MagnetOptions[])options.values().toArray(new MagnetOptions[0]);
	}

}
