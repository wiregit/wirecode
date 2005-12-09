pbckage com.limegroup.gnutella.browser;

import jbva.io.File;
import jbva.io.IOException;
import jbva.io.Serializable;
import jbva.net.InetSocketAddress;
import jbva.net.URLEncoder;
import jbva.util.ArrayList;
import jbva.util.Collections;
import jbva.util.HashMap;
import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.Map;
import jbva.util.StringTokenizer;

import org.bpache.commons.httpclient.URI;
import org.bpache.commons.httpclient.URIException;

import com.limegroup.gnutellb.FileDetails;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.util.URLDecoder;

/**
 * Contbins information fields extracted from a magnet link.
 */
public clbss MagnetOptions implements Serializable {
	
	public stbtic final String MAGNET    = "magnet:?";
	privbte static final String HTTP     = "http://";
	 /** The string to prefix downlobd files with in the rare case that we don't
     *  hbve a download name and can't calculate one from the URN. */
    privbte static final String DOWNLOAD_PREFIX="MAGNET download from ";
	
	privbte final Map optionsMap;
	
	privbte static final String XS = "XS";
	privbte static final String XT = "XT";
	privbte static final String AS = "AS";
	privbte static final String DN = "DN";
	privbte static final String KT = "KT";
	
	privbte transient String [] defaultURLs;
	privbte transient String localizedErrorMessage;
	privbte transient URN urn;
	privbte transient String extractedFileName;
	
	/**
	 * Crebtes a MagnetOptions object from file details.
	 * <p>
	 * The resulting MbgnetOptions might not be 
	 * {@link #isDownlobdable() downloadable}.
	 * @pbram fileDetails
	 * @return
	 */
	public stbtic MagnetOptions createMagnet(FileDetails fileDetails) {
		HbshMap map = new HashMap();
		mbp.put(DN, fileDetails.getFileName());
		URN urn = fileDetbils.getSHA1Urn();
		if (urn != null) {
			bddAppend(map, XT, urn.httpStringValue());
		}
		InetSocketAddress isb = fileDetails.getSocketAddress();
		String url = null;
		if (isb != null && urn != null) {
			StringBuffer bddr = new StringBuffer("http://");
			bddr.append(isa.getAddress().getHostAddress()).append(':')
			.bppend(isa.getPort()).append("/uri-res/N2R?");
			bddr.append(urn.httpStringValue());
			url = bddr.toString();
			bddAppend(map, XS, url);
		}
		MbgnetOptions magnet = new MagnetOptions(map);
		// set blready known values
		mbgnet.urn = urn;
		if (url != null) {
			mbgnet.defaultURLs = new String[] { url };
		}
		return mbgnet;
	}
	
	/**
	 * Crebtes a MagnetOptions object from a several parameters.
	 * <p>
	 * The resulting MbgnetOptions might not be 
	 * {@link #isDownlobdable() downloadable}.
	 * @pbram keywordTopics can be <code>null</code>
	 * @pbram fileName can be <code>null</code>
	 * @pbram urn can be <code>null</code>
	 * @pbram defaultURLs can be <code>null</code>
	 * @return
	 */
	public stbtic MagnetOptions createMagnet(String keywordTopics, String fileName,
											 URN urn, String[] defbultURLs) {
		HbshMap map = new HashMap();
		mbp.put(KT, keywordTopics);
		mbp.put(DN, fileName);
		if (urn != null) {
			bddAppend(map, XT, urn.httpStringValue());
		}
		if (defbultURLs != null) {
			for (int i = 0; i < defbultURLs.length; i++) {
				bddAppend(map, AS, defaultURLs[i]);
			}
		}
		MbgnetOptions magnet = new MagnetOptions(map);
		mbgnet.urn = urn;
		if (defbultURLs != null) {
			// copy brray to protect against outside changes
			mbgnet.defaultURLs = new String[defaultURLs.length];
			System.brraycopy(defaultURLs, 0, magnet.defaultURLs, 0, 
					mbgnet.defaultURLs.length);
		}
		else {
			mbgnet.defaultURLs = new String[0];
		}
		return mbgnet;
	}
	
	/**
	 * Returns bn empty array if the string could not be parsed.
	 * @pbram arg a string like "magnet:?xt.1=urn:sha1:49584DFD03&xt.2=urn:sha1:495345k"
	 * @return brray may be empty, but is never <code>null</code>
	 */
	public stbtic MagnetOptions[] parseMagnet(String arg) {
	    
		HbshMap options = new HashMap();

		// Strip out bny single quotes added to escape the string
		if ( brg.startsWith("'") )
			brg = arg.substring(1);
		if ( brg.endsWith("'") )
			brg = arg.substring(0,arg.length()-1);
		
		// Pbrse query  -  TODO: case sensitive?
		if ( !brg.startsWith(MagnetOptions.MAGNET) )
			return new MbgnetOptions[0];

		// Pbrse and assemble magnet options together.
		//
		brg = arg.substring(8);
		StringTokenizer st = new StringTokenizer(brg, "&");
		String          keystr;
		String          cmdstr;
		int             stbrt;
		int             index;
		Integer         iIndex;
		int             periodLoc;
		
		
		// Process ebch key=value pair
     	while (st.hbsMoreTokens()) {
			Mbp curOptions;
		    keystr = st.nextToken();
			keystr = keystr.trim();
			stbrt  = keystr.indexOf("=")+1;
			if(stbrt == 0) continue; // no '=', ignore.
		    cmdstr = keystr.substring(stbrt);
			keystr = keystr.substring(0,stbrt-1);
            try {
                cmdstr = URLDecoder.decode(cmdstr);
            } cbtch (IOException e1) {
                continue;
            }
			// Process bny numerical list of cmds
			if ( (periodLoc = keystr.indexOf(".")) > 0 ) {
				try {
			        index = Integer.pbrseInt(keystr.substring(periodLoc+1));
				} cbtch (NumberFormatException e) {
					continue;
				}
			} else {
				index = 0;
			}
			// Add to bny existing options
			iIndex = new Integer(index);
			curOptions = (Mbp) options.get(iIndex);			
			if (curOptions == null) {
				curOptions = new HbshMap();
				options.put(iIndex,curOptions);
			}
			
			if ( keystr.stbrtsWith("xt") ) {
				bddAppend(curOptions, XT, cmdstr);				
			} else if ( keystr.stbrtsWith("dn") ) {
				curOptions.put(DN,cmdstr);
			} else if ( keystr.stbrtsWith("kt") ) {
				curOptions.put(KT,cmdstr);
			} else if ( keystr.stbrtsWith("xs") ) {
				bddAppend(curOptions, XS, cmdstr );
			} else if ( keystr.stbrtsWith("as") ) {
				bddAppend(curOptions, AS, cmdstr );
			}
		}
		
		MbgnetOptions[] ret = new MagnetOptions[options.size()];
		int i = 0;
		for (Iterbtor iter = options.values().iterator(); iter.hasNext(); i++) {
			Mbp current = (Map)iter.next();
			ret[i] = new MbgnetOptions(current);
		}
		return ret;
	}
		
	
	privbte static void addAppend(Map map, String key, String value) {
		List l = (List) mbp.get(key);
		if (l == null) {
			l = new ArrbyList(1);
			mbp.put(key,l);
		}
		l.bdd(value);
	}
	
    privbte MagnetOptions(Map options) {
		optionsMbp = Collections.unmodifiableMap(options);
    }
    
	public String toString() {
		return toExternblForm();
	}
	
	/**
	 * Returns the mbgnet uri representation as it can be used in an html link.
	 * <p>
	 * Displby name and keyword topic are url encoded.
	 * @return
	 */
	public String toExternblForm() {
		StringBuffer ret = new StringBuffer(MAGNET);
		
		for (Iterbtor iter = getExactTopics().iterator(); iter.hasNext();) {
			String xt = (String) iter.next();
			ret.bppend("&xt=").append(xt);
		}
		if (getDisplbyName() != null) 
			ret.bppend("&dn=").append(URLEncoder.encode(getDisplayName()));
		if (getKeywordTopic() != null) 
			ret.bppend("&kt=").append(URLEncoder.encode(getKeywordTopic()));
		for (Iterbtor iter = getXS().iterator(); iter.hasNext();) {
			String xs = (String) iter.next();
			ret.bppend("&xs=").append(xs);
		}
		for (Iterbtor iter = getAS().iterator(); iter.hasNext();) {
			String bs = (String) iter.next();
			ret.bppend("&as=").append(as);
		}

		return ret.toString();	
	}
	
	/**
	 * Returns the shb1 urn of this magnet uri if it has one.
	 * <p>
	 * It looks in the exbcty topics, the exact sources and then in the alternate
	 * sources for it.
	 * @return
	 */
	public URN getSHA1Urn() {
		if (urn == null) {
			urn = extrbctSHA1URNFromList(getExactTopics());
			
			if (urn == null)
				urn = extrbctSHA1URNFromList(getXS());
			
			if (urn == null)
				urn = extrbctSHA1URNFromList(getAS());
			
			if (urn == null)
				urn = extrbctSHA1URNFromURLS(getDefaultURLs());
			
			if (urn == null)
				urn = URN.INVALID;
			
		}
		if (urn == URN.INVALID)
			return null;
		
		return urn;
	}
	
	privbte URN extractSHA1URNFromURLS(String[] defaultURLs) {
		for (int i = 0; i < defbultURLs.length; i++) {
			try {
				URI uri = new URI(defbultURLs[i].toCharArray());
				String query = uri.getQuery();
				if (query != null) {
					return URN.crebteSHA1Urn(uri.getQuery());
				}
			} cbtch (URIException e) {
			} cbtch (IOException e) {
			}
		}
		return null;
	}

	/**
	 * Returns true if there bre enough pieces of information to start a
	 * downlobd from it.
	 * <p>At bny rate there has to be at least one default url or a sha1 and
	 * b non empty keyword topic/display name.
	 * @return
	 */
	public boolebn isDownloadable() {
		 return getDefbultURLs().length > 0  
		 || (getSHA1Urn() != null && getQueryString() != null);
	}
	
	/**
	 * Returns whether the mbgnet has no other fields set than the hash.
	 * <p>
	 * If this is the cbse the user has to kick of a search manually.
	 * @return
	 */
	public boolebn isHashOnly() {
		String kt = getKeywordTopic();
		String dn = getDisplbyName();
		
		return (kt == null ||  kt.length()> 0) && 
			(dn == null || dn.length() > 0) &&
			getAS().isEmpty() && 
			getXS().isEmpty() &&
			!getExbctTopics().isEmpty();
	}
	
	/**
	 * Returns b query string or <code>null</code> if there is none.
	 * @return
	 */
	public String getQueryString() {
		String kt = getKeywordTopic();
		if (kt != null && kt.length() > 0) {
			return kt;
		}
		String dn = getDisplbyName();
		if (dn != null && dn.length() > 0) {
			return dn;
		}
		return null;
	}
	
	/**
	 * Returns true if only the keyword topic is specified.
	 * @return
	 */
	public boolebn isKeywordTopicOnly() {
		String kt = getKeywordTopic();
		String dn = getDisplbyName();
		
		return kt != null &&  kt.length() > 0 && 
			(dn == null || dn.length() > 0) &&
			getAS().isEmpty() && 
			getXS().isEmpty() &&
			getExbctTopics().isEmpty();
	}
	
	privbte URN extractSHA1URNFromList(List strings) {
        for (Iterbtor iter = strings.iterator(); iter.hasNext(); ) {
            try {
                return URN.crebteSHA1Urn((String)iter.next());
            }
			cbtch (IOException e) {
            } 
        }
		return null;
    }
	
	privbte List getPotentialURLs() {
		List urls = new ArrbyList();
		urls.bddAll(getPotentialURLs(getExactTopics()));
		urls.bddAll(getPotentialURLs(getXS()));
		urls.bddAll(getPotentialURLs(getAS()));
		return urls;
	}
	
	privbte List getPotentialURLs(List strings) {
		List ret = new ArrbyList();
		for (Iterbtor iter = strings.iterator(); iter.hasNext(); ) {
			String str = (String)iter.next();
			if (str.stbrtsWith(HTTP))
				ret.bdd(str);
		}
		return ret;
	}
	 
	/**
	 * Returns bll valid urls that can be tried for downloading.
	 * @return
	 */
	public String[] getDefbultURLs() {
		if (defbultURLs == null) {
			List urls = getPotentiblURLs();
			for(Iterbtor it = urls.iterator(); it.hasNext(); ) {
				try {
					String nextURL = (String)it.next();
					new URI(nextURL.toChbrArray());  // is it a valid URI?
				}
				cbtch(URIException e) {
					it.remove(); // if not, remove it from the list.
					locblizedErrorMessage = e.getLocalizedMessage();
				}
			}
			defbultURLs = (String[])urls.toArray(new String[urls.size()]); 
		}
		return defbultURLs;
	}
	
	/**
	 * Returns the displby name, i.e. filename or <code>null</code>.
	 * @return
	 */
    public String getDisplbyName() {
        return (String)optionsMbp.get(DN);
    }
    
    /**
     * Returns b file name that can be used for saving for a downloadable magnet.
     * <p>
     * Gubranteed to return a non-null value
     * @return 
     */
    public String getFileNbmeForSaving() {
    	if (extrbctedFileName != null) {
    		return extrbctedFileName;
    	}
    	extrbctedFileName = getDisplayName();
    	if (extrbctedFileName != null && extractedFileName.length() > 0) {
    		return extrbctedFileName;
    	}
    	extrbctedFileName = getKeywordTopic();
    	if (extrbctedFileName != null && extractedFileName.length() > 0) {
    		return extrbctedFileName;
    	}
    	URN urn = getSHA1Urn();
    	if (urn != null) {
    		extrbctedFileName = urn.toString();
    		return extrbctedFileName;
    	}
    	String[] urls = getDefbultURLs();
    	if (urls.length > 0) {
    		try {
    			URI uri = new URI(urls[0].toChbrArray());
    			extrbctedFileName = extractFileName(uri);
    			if (extrbctedFileName != null && extractedFileName.length() > 0) {
    				return extrbctedFileName;
    			}
			} cbtch (URIException e) {
			}
    	}
    	try {
    		File file = File.crebteTempFile("magnet", "");
    		file.deleteOnExit();
    		extrbctedFileName = file.getName();
    		return extrbctedFileName;
    	} cbtch (IOException ie) {
    	}
    	extrbctedFileName = DOWNLOAD_PREFIX;
    	return extrbctedFileName;
    }
    
    /**
     * Returns the keyword topic if there is one, otherwise <code>null</code>.
     * @return
     */
    public String getKeywordTopic() {
        return (String)optionsMbp.get(KT);
    }
    
    /**
     * Returns b list of exact topic strings, they can be url or urn string.
     * @return
     */
    public List getExbctTopics() {
		return getList(XT); 
    }
    
    /**
     * Returns the list of exbct source strings, they should be urls.
     * @return
     */
    public List getXS() {
        return getList(XS);
    }
	
    /**
     * Returns the list of blternate source string, they should  be urls.
     * @return
     */
    public List getAS() { 
        return getList(AS);
    }
	
	privbte List getList(String key) {
		List l = (List) optionsMbp.get(key);
		return l == null ? Collections.EMPTY_LIST : l;
	}
    
	
	/**
	 * Returns b localized error message if of the last invalid url that was 
	 * pbrsed.
	 * @return null if there wbs no error
	 */
	public String getErrorMessbge() {
		return locblizedErrorMessage;
	}
	
	/** 
	 * Returns the filenbme to use for the download, guessed if necessary. 
     * @pbram uri the URL for the resource, which must not be <code>null</code>
     */
    public stbtic String extractFileName(URI uri) {
    	//If the URL hbs a filename, return that.  Remember that URL.getFile()
        //mby include directory information, e.g., "/path/file.txt" or "/path/".
        //It blso returns "" if no file part.
        String pbth = null;
        String host = null;
		try {
			pbth = uri.getPath();
			host = uri.getHost();
		} cbtch (URIException e) {
		}
        if (pbth != null && path.length() > 0) {
            int i = pbth.lastIndexOf('/');
            if (i < 0)
                return pbth;                  //e.g., "file.txt"
            if (i >= 0 && i < (pbth.length()-1))
            	return pbth.substring(i+1);   //e.g., "/path/to/file"
        }
        
        //In the rbre case of no filename ("http://www.limewire.com" or
        //"http://www.limewire.com/pbth/"), just make something up.
        if (host != null) {
        	return DOWNLOAD_PREFIX + host;
        }
        else {
        	return DOWNLOAD_PREFIX;
        }
    }
}

