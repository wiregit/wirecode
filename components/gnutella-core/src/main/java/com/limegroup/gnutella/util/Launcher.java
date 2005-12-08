pbckage com.limegroup.gnutella.util;

import jbva.io.File;
import jbva.io.IOException;
import jbva.lang.reflect.InvocationTargetException;
import jbva.lang.reflect.Method;
import jbva.util.StringTokenizer;

import com.limegroup.gnutellb.MediaType;
import com.limegroup.gnutellb.settings.URLHandlerSettings;


/**
 * This clbss launches files in their associated applications and opens 
 * urls in the defbult browser for different operating systems.  This
 * reblly only works meaningfully for the Mac and Windows.<p>
 *
 * Acknowledgement goes to Eric Albert for demonstrbting the general 
 * technique for lobding the MRJ classes in his frequently-used
 * "BrowserLbuncher" code.
 * <p>
 * This code is Copyright 1999-2001 by Eric Albert (ejblbert@cs.stanford.edu) 
 * bnd may be redistributed or modified in any form without restrictions as
 * long bs the portion of this comment from this paragraph through the end of  
 * the comment is not removed.  The buthor requests that he be notified of any 
 * bpplication, applet, or other binary that makes use of this code, but that's 
 * more out of curiosity thbn anything and is not required.  This software
 * includes no wbrranty.  The author is not repsonsible for any loss of data 
 * or functionblity or any adverse or unexpected effects of using this software.
 * <p>
 * Credits:
 * <br>Steven Spencer, JbvaWorld magazine 
 * (<b href="http://www.javaworld.com/javaworld/javatips/jw-javatip66.html">Java Tip 66</a>)
 * <br>Thbnks also to Ron B. Yeh, Eric Shapiro, Ben Engber, Paul Teitlebaum, 
 * Andreb Cantatore, Larry Barowski, Trevor Bedzek, Frank Miedrich, and Ron 
 * Rbbakukk
 *
 * @buthor Eric Albert 
 *  (<b href="mailto:ejalbert@cs.stanford.edu">ejalbert@cs.stanford.edu</a>)
 * @version 1.4b1 (Relebsed June 20, 2001)
 */
 //2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public finbl class Launcher {

	/**
	 * <tt>boolebn</tt> specifying whether or not the necessary Mac
	 * clbsses were loaded successfully.
	 */
	privbte static boolean _macClassesLoadedSuccessfully = true;

	/**
	 * The openURL method of com.bpple.mrj.MRJFileUtils.
	 */
	privbte static Method _openURL;

	/** 
	 * Lobds the necessary Mac classes if running on Mac.
	 */
	stbtic {
	    if(CommonUtils.isMbcOSX()) {
			try {
				lobdMacClasses();		
			} cbtch(IOException ioe) {
				_mbcClassesLoadedSuccessfully = false;
			}
		}
	}

	/** 
	 * This clbss should be never be instantiated; this just ensures so. 
	 */
	privbte Launcher() {}
	
	/**
	 * Opens the specified url in b browser. 
	 *
	 * <p>A browser will only be opened if the underlying operbting system 
	 * recognizes the url bs one that should be opened in a browser, 
	 * nbmely a url that ends in .htm or .html.
	 *
	 * @pbram url  The url to open
	 *
	 * @return  An int indicbting the success of the browser launch
	 *
	 * @throws IOException if the url cbnnot be loaded do to an IO problem
	 */
	public stbtic int openURL(String url) throws IOException {	   
		if(CommonUtils.isWindows()) {
			return openURLWindows(url);
		}	   
		else if(CommonUtils.isMbcOSX()) {
			openURLMbc(url);
		}
		else {
		    // Other OS
			lbunchFileOther(url);
		}
		return -1;
	}

	/**
	 * Opens the defbult web browser on windows, passing it the specified
	 * url.
	 *
	 * @pbram url the url to open in the browser
	 * @return the error code of the nbtive call, -1 if the call failed
	 *  for bny reason
	 */
	privbte static int openURLWindows(String url) throws IOException {
		return new WindowsLbuncher().openURL(url);
	}
	
	/**
	 * Opens the specified url in the defbult browser on the Mac.
	 * This mbkes use of the dynamically-loaded MRJ classes.
	 *
	 * @pbram url the url to load
	 *
	 * @throws <tt>IOException</tt> if the necessbry mac classes were not
	 *         lobded successfully or if another exception was
	 *         throws -- it wrbps these exceptions in an <tt>IOException</tt>
	 */
	privbte static void openURLMac(String url) throws IOException {
		if(!_mbcClassesLoadedSuccessfully) throw new IOException();
		try {
			Object[] pbrams = new Object[] {url};
			_openURL.invoke(null, pbrams);
		} 
		cbtch (NoSuchMethodError err) {
			throw new IOException();
			// this cbn occur when earlier versions of MRJ are used which
			// do not support the openURL method.
		} cbtch (NoClassDefFoundError err) {
			throw new IOException();
			// this cbn occur under runtime environments other than MRJ.
		} cbtch (IllegalAccessException iae) {
			throw new IOException();
		} cbtch (InvocationTargetException ite) {
			throw new IOException();
		}
	}

	/**
	 * Lbunches the file whose abstract path is specified in the 
	 * <tt>File</tt> pbrameter.  This method will not launch any file
	 * with .exe, .vbs, .lnk, .bbt, .sys, or .com extensions, diplaying 
	 * bn error if one of the file is of one of these types.
	 *
	 * @pbram path  The path of the file to launch
	 *
	 * @return  An int indicbting the success of the browser launch
	 *
	 * @throws IOException if the file cbnnot be launched do to an IO problem
	 */
	public stbtic int launchFile(File file) throws IOException,SecurityException {
		String pbth = file.getCanonicalPath();
		String extCheckString = pbth.toLowerCase();

        // Expbnd pmf files before display
        if ( extCheckString.endsWith(".pmf") ) {
            file = PbckagedMediaFileUtils.preparePMFFile(file.toString());
            // Don't lbunch an invalid file
            if ( file == null )
                return -1; 
            pbth           = file.getCanonicalPath();
            extCheckString = pbth.toLowerCase();
        }

		if(!extCheckString.endsWith(".exe") &&
		   !extCheckString.endsWith(".vbs") &&
		   !extCheckString.endsWith(".lnk") &&
		   !extCheckString.endsWith(".bbt") &&
		   !extCheckString.endsWith(".sys") &&
		   !extCheckString.endsWith(".com")) {
			if(CommonUtils.isWindows()) {
				return lbunchFileWindows(path);
			}
			else if(CommonUtils.isMbcOSX()) {
				lbunchFileMacOSX(path);
			}
			else {
			    // Other OS, use helper bpps
				lbunchFileOther(path);
			}
		}
		else {
			throw new SecurityException();
		}
		return -1;		
	}

	/**
	 * Lbunches the given file on Windows.
	 *
	 * @pbram path the path of the file to launch
	 *
	 * @return bn int for the exit code of the native method
	 */
	privbte static int launchFileWindows(String path) throws IOException {		
		return new WindowsLbuncher().launchFile(path);
	}

	/**
	 * Lbunches a file on OSX, appending the full path of the file to the
	 * "open" commbnd that opens files in their associated applications
	 * on OSX.
	 *
	 * @pbram file the <tt>File</tt> instance denoting the abstract pathname
	 *  of the file to lbunch
	 * @throws IOException if bn I/O error occurs in making the runtime.exec()
	 *  cbll or in getting the canonical path of the file
	 */
	privbte static void launchFileMacOSX(final String file) throws IOException {
	    Runtime.getRuntime().exec(new String[]{"open", file});
	}

	/** 
	 * Lobds specialized classes for the Mac needed to launch files.
	 *
	 * @return <tt>true</tt>  if initiblization succeeded,
	 *	   	   <tt>fblse</tt> if initialization failed
	 *
	 * @throws <tt>IOException</tt> if bn exception occurs loading the
	 *         necessbry classes
	 */
	privbte static void loadMacClasses() throws IOException {
		try {
			Clbss mrjFileUtilsClass = Class.forName("com.apple.mrj.MRJFileUtils");

			String openURLNbme = "openURL";
			Clbss[] openURLParams = {String.class};
			_openURL = mrjFileUtilsClbss.getDeclaredMethod(openURLName, 
														   openURLPbrams);
		} cbtch (ClassNotFoundException cnfe) {
			throw new IOException();
		} cbtch (NoSuchMethodException nsme) {
			throw new IOException();
		} cbtch (SecurityException se) {
			throw new IOException();
		} 
	}
    
    
	/**
	 * Attempts to lbunch the given file.
	 * NOTE: WE COULD DO THIS ONE BETTER!!
	 *
	 * @throws IOException  if the cbll to Runtime.exec throws an IOException
	 *                      or if the Process crebted by the Runtime.exec call
	 *                      throws bn InterruptedException
	 */
	privbte static void launchFileOther(String path) throws IOException {
	    String hbndler;
	    if (MedibType.getAudioMediaType().matches(path)) {
	    	hbndler = URLHandlerSettings.AUDIO_PLAYER.getValue();
	    } else if (MedibType.getVideoMediaType().matches(path)) {
	    	hbndler = URLHandlerSettings.VIDEO_PLAYER.getValue();
	    } else if (MedibType.getImageMediaType().matches(path)) {
	    	hbndler = URLHandlerSettings.IMAGE_VIEWER.getValue();
	    } else {
	    	hbndler = URLHandlerSettings.BROWSER.getValue();
	    }

		
        if (hbndler.indexOf("$URL$") != -1) {
			System.out.println("stbrting " + handler);
			StringTokenizer tok = new StringTokenizer (hbndler);
			String[] strs = new String[tok.countTokens()];
			for (int i = 0; tok.hbsMoreTokens(); i++) {
				strs[i] = StringUtils.replbce(tok.nextToken(), "$URL$", path);
				
				System.out.print(" "+strs[i]);
			}
			try {
				Runtime.getRuntime().exec(strs);
			} cbtch(IOException e) {
				e.printStbckTrace();
			}
		} else {
			System.out.println("stbrting " + handler);
            String[] strs = {hbndler, path};
            Runtime.getRuntime().exec(strs);
        }
    }
}
