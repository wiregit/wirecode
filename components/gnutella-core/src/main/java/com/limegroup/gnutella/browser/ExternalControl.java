package com.limegroup.gnutella.browser;

import com.limegroup.gnutella.*; 
import com.limegroup.gnutella.util.URLDecoder;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.settings.*;
import com.sun.java.util.collections.*;
import java.util.StringTokenizer;
import java.net.*;
import java.io.*;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

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

		if ( options == null ) {
		    if(LOG.isWarnEnabled())
		        LOG.warn("Invalid magnet, ignoring: " + arg);
			return;
        }
		
		downloadMagnet(options);
        
        
	}
	
	/**
	 * performs the actual magnet download.  This way it is possible to 
	 * parse and download the magnet separately (which is what I intend to do in the gui) --zab
	 * @param options the magnet options returned from parseMagnet
	 */
	public static void downloadMagnet(MagnetOptions []options) {
		if(LOG.isDebugEnabled()) {
            for(int i = 0; i < options.length; i++) {
                LOG.debug("Kicking off downloader for option " + i +
                          " " + options[i]);
            }
        }                 

		// Kick off appropriate downloaders
        // 
        MagnetOptions curOpt;
        boolean xtSHA1 = false;
        boolean xsSHA1 = false;
        boolean asSHA1 = false;
		for ( int i = 0; i < options.length; i++ ) {
            curOpt = options[i];

            // Find SHA1 URN
			URN urn = null;
			if ( curOpt.xt != null && curOpt.xt.length() > 0 ) {
				try {
			        urn = URN.createSHA1Urn(curOpt.xt);
                    xtSHA1 = true;
				} catch (IOException e) { 
                    /* xt Not a SHA1 - try xs */ 
                    if ( curOpt.xs != null && curOpt.xs.length() > 0 ) {
                        try {
                            urn = URN.createSHA1Urn(curOpt.xs);
                            xsSHA1 = true;
                        } catch (IOException e2) { 
                            /* xs Not a SHA1 */ 
                            if ( curOpt.as != null && 
                                 curOpt.as.length() > 0 ) {
                                try {
                                    urn = URN.createSHA1Urn(curOpt.as);
                                    asSHA1 = true;
                                } catch (IOException e3) { /* as Not a SHA1 */ }
                            }
                        }
                    }
                }
			}

			// Collect up http locations
			String defaultURLs[] = null;
			ArrayList urls = new ArrayList();
            String errorMsg = null;
            if (!xtSHA1 && curOpt.xt != null && 
                curOpt.xt.startsWith(HTTP)) 
                urls.add(curOpt.xt);
			if (!xsSHA1 && curOpt.xs != null && 
                curOpt.xs.startsWith(HTTP)) 
				urls.add(curOpt.xs);
			if (!asSHA1 && curOpt.as != null && 
                curOpt.as.startsWith(HTTP)) 
				urls.add(curOpt.as);
			if (urls.size() > 0) {
                // Verify each URL before adding it to the defaultURLs.
                for(Iterator it = urls.iterator(); it.hasNext(); ) {
                    try {
                        String nextURL = (String)it.next();
                        new URI(nextURL.toCharArray());  // is it a valid URI?
                    } catch(URIException e) {
                        LOG.warn("Invalid URI in magnet", e);
                        errorMsg = e.getMessage();
                        it.remove(); // if not, remove it from the list.
                    }
                }
		        defaultURLs = (String[])urls.toArray(new String[urls.size()]);
			}
			
		    if(LOG.isDebugEnabled()) {
		        LOG.debug("Processing magnet with params:\n" +
		                  "urn [" + urn + "]\n" +
		                  "options [" + curOpt + "]");
            }

            // Validate that we have something to go with from magnet
            // If not, report an error.
            if ( !( urls.size() > 0  || 
                    urn != null || 
                    (curOpt.kt != null && !"".equals(curOpt.kt)) ) ) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Invalid magnet. urls.size == " + urls.size() +
                             "curOpt.kt == " + curOpt.kt);
                }
                if ( errorMsg != null )
                    errorMsg = curOpt.toString() + " (" + errorMsg + ")";
                else
                    errorMsg = curOpt.toString();
                MessageService.showError("ERROR_BAD_MAGNET_LINK", errorMsg);
                return;
            }
            
            // Warn the user that the link was slightly invalid
            if( errorMsg != null )
                MessageService.showError("ERROR_INVALID_URLS_IN_MAGNET");
            
            try {
            	RouterService.download
                   	(urn,curOpt.kt,curOpt.dn,defaultURLs,false);//!overwrite
            } catch ( AlreadyDownloadingException a ) {  
                MessageService.showError(
                    "ERROR_ALREADY_DOWNLOADING", a.getFilename());
			} catch ( IllegalArgumentException il ) { 
			    ErrorService.error(il);
			} catch (FileExistsException fex) {
                MessageService.showError(
                    "ERROR_ALREADY_EXISTS", fex.getFileName());
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
			if ( !LOCALHOST.equals(
				  socket.getInetAddress().getHostAddress()) ) {
                if(LOG.isWarnEnabled()) {
				    LOG.warn("Invalid magnet request from: " + 
				              socket.getInetAddress().getHostAddress());
                }
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


	
	public static MagnetOptions[] parseMagnet(String arg) {
	    LOG.trace("enter parseMagnet");
		MagnetOptions[] ret = null;
		HashMap         options = new HashMap();

		// Strip out any single quotes added to escape the string
		if ( arg.startsWith("'") )
			arg = arg.substring(1);
		if ( arg.endsWith("'") )
			arg = arg.substring(0,arg.length()-1);
		
		// Parse query  -  TODO: case sensitive?
		if ( !arg.startsWith(MagnetOptions.MAGNET) )
			return ret;

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

			if ( keystr.startsWith("xt") &&
                 cmdstr.indexOf("urn:sha1:")!=-1 ) {
				curOptions.xt = cmdstr;
			} else if ( keystr.startsWith("dn") ) {
				curOptions.dn = cmdstr;
			} else if ( keystr.startsWith("kt") ) {
				curOptions.kt = cmdstr;
			} else if ( keystr.startsWith("xs") ) {
				curOptions.xs = cmdstr;
			} else if ( keystr.startsWith("as") ) {
				curOptions.as = cmdstr;
			}
			options.put(iIndex, curOptions);
		}
		
		ret = new MagnetOptions[options.size()];
		ret = (MagnetOptions[]) options.values().toArray(ret);

		return ret;
	}

	

}
