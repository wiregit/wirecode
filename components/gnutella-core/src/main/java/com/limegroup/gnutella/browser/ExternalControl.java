package com.limegroup.gnutella.browser;

import com.limegroup.gnutella.*; 
import com.limegroup.gnutella.util.URLDecoder;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.downloader.*;
import com.sun.java.util.collections.*;
import java.util.StringTokenizer;
import java.net.*;
import java.io.*;


public class ExternalControl {


	private static final String LOCALHOST = "127.0.0.1"; 
	private static final String HTTP      = "http://";

	public static String preprocessArgs(String args[]) {

		String arg = new String();
		for(int i = 0; i < args.length; i++) {
			arg += args[i];
		}
		return arg;
	}

	public static void checkForActiveLimeWire(String arg) {
	    if(  CommonUtils.isWindows() && testForLimeWire(arg) ) {
		    System.exit(0);	
		}
	}
	
	public static void handleMagnetRequest(String arg) {

        RouterService    router   = RouterService.instance();
		ActivityCallback callback = router.getActivityCallback();
        SettingsManager  settings = SettingsManager.instance();

		// Make sure that connections are active
		if ( router.getNumInitializedConnections() <= 0 ) 
		    router.connect();

		callback.restoreApplication();
		callback.showDownloads();

	    MagnetOptions options[] = parseMagnet(arg);

		if ( options == null )
			return;

		// Kick off appropriate downloaders
		for ( int i = 0; i < options.length; i++ ) {
			URN urn = null;
			if ( options[i].xt != null && options[i].xt.length() > 0 ) {
				try {
			        urn = URN.createSHA1Urn(options[i].xt);
				} catch (IOException e) { /* Not a SHA1 */ }
			}

			// Collect up exact locations and additional locations
			String defaultURLs[] = null;
			ArrayList urls = new ArrayList();
			if (options[i].xs != null && options[i].xs.startsWith(HTTP)) 
				urls.add(options[i].xs);
			if (options[i].as != null && options[i].as.startsWith(HTTP)) 
				urls.add(options[i].as);
			if (urls.size() > 0) {
				defaultURLs = new String[urls.size()];
		        defaultURLs = (String[]) urls.toArray(defaultURLs);
			}

			try {
				//System.out.println("download parms:");
				//System.out.println("urn:"+urn);
				//System.out.println("kt:"+options[i].kt);
				//System.out.println("dn:"+options[i].dn);
				//System.out.println("xs:"+options[i].xs);
				//System.out.println("as:"+options[i].as);

				// TODO:  Need to juggle xt, xs and as if SHA1s and URLs are
				// being used loosely.
                router.download(urn,options[i].kt,options[i].dn,defaultURLs);
			} catch ( AlreadyDownloadingException a ) {  
			    // Silently fail
			} catch ( IllegalArgumentException il ) { 
			    // Silently fail
			}
		}
	}

	/**
	 *  Handle a Magnet request via a socket (for TCP handling).
	 *  Deiconify the application, fire MAGNET request
	 *  and return true as a sign that LimeWire is running.
	 */
	public static void fireMagnet(Socket socket) {
		try {
			// Only allow control from localhost
			if ( !LOCALHOST.equals(
				  socket.getInetAddress().getHostAddress()) )
				return;

			InputStream in = socket.getInputStream();

			// First read extra parameter
			socket.setSoTimeout(SettingsManager.instance().getTimeout());
			String word=IOUtils.readWord(in,500);
			socket.setSoTimeout(0);

			BufferedOutputStream out =
			  new BufferedOutputStream(socket.getOutputStream());
			String s = "true\n\r";
			byte[] bytes=s.getBytes();
			out.write(bytes);
			out.flush();
		    handleMagnetRequest(word);
		} catch (IOException e) {
		}
			
		try { socket.close(); } catch (IOException e) { }
	}

	

	/**  Check if the client is already running, and if so, pop it up.
	 *   Sends the MAGNET message along the given socket. 
	 *   @returns  true if a local LimeWire responded with a true.
	 */
	private static boolean testForLimeWire(String arg) {
		Socket socket = null;
		try {
			socket = Sockets.connect(LOCALHOST, 
		      SettingsManager.instance().getPort(), 500, true);
		} catch (IOException e) {
		    return false;
		}
		//The try-catch below is a work-around for JDK bug 4091706.
		InputStream istream=null;
		try {
			istream=socket.getInputStream(); 
		} catch (Exception e) {
			return false;
		}
		try {
			socket.setSoTimeout(500); 
		    ByteReader byteReader = new ByteReader(istream);
		    OutputStream os = socket.getOutputStream();
		    OutputStreamWriter osw = new OutputStreamWriter(os);
		    BufferedWriter out=new BufferedWriter(osw);
		    out.write("MAGNET "+arg+" ");
		    out.write("\n\r");
		    out.flush();
		    String str = byteReader.readLine();
		    return(str != null && str.startsWith("true"));
		} catch (IOException e2) {
		}
	    return false;
	}


	private static MagnetOptions[] parseMagnet(String arg) {
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
		    cmdstr = keystr.substring(start);
			keystr = keystr.substring(0,start-1);
            cmdstr=URLDecoder.decode(cmdstr);
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

	/*
    public static void main(String args[]) {
		String arg = preprocessArgs(args);
	    connectSearchDownload(arg);
    }	
	*/

}

class MagnetOptions {
	public static final String MAGNET    = "magnet:?";
	public String xt;
	public String dn; 
	public String kt; 
	public String xs;
	public String as;  // This is technically suppose to handle multiple

	public String toString() {
		String ret = MAGNET;
		
		if ( xt != null ) 
			ret += "&xt="+xt+"";
		if ( dn != null ) 
			ret += "&dn="+dn+"";
		if ( kt != null ) 
			ret += "&kt="+kt+"";
		if ( xs != null ) 
			ret += "&xs="+xs+"";
		if ( as != null ) 
			ret += "&as="+as+"";
		return ret;
	}
}
