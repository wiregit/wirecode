package com.limegroup.gnutella.browser;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.URLDecoder;

/**
 * Contains information fields extracted from a magnet link.
 */
public class MagnetOptions implements Serializable {
	
	public static final String MAGNET    = "magnet:?";
	private static final String HTTP     = "http://";
	 /** The string to prefix download files with in the rare case that we don't
     *  have a download name and can't calculate one from the URN. */
    private static final String DOWNLOAD_PREFIX="MAGNET download from ";
	
	private final Map optionsMap;
	
	private static final String XS = "XS";
	private static final String XT = "XT";
	private static final String AS = "AS";
	private static final String DN = "DN";
	private static final String KT = "KT";
	
	private transient String [] defaultURLs;
	private transient String localizedErrorMessage;
	private transient URN urn;
	private transient String extractedFileName;
	
	/**
	 * Creates a MagnetOptions object from file details.
	 * <p>
	 * The resulting MagnetOptions might not be 
	 * {@link #isDownloadable() downloadable}.
	 * @param fileDetails
	 * @return
	 */
	public static MagnetOptions createMagnet(FileDetails fileDetails) {
		HashMap map = new HashMap();
		map.put(DN, fileDetails.getFileName());
		URN urn = fileDetails.getSHA1Urn();
		if (urn != null) {
			addAppend(map, XT, urn.httpStringValue());
		}
		InetSocketAddress isa = fileDetails.getSocketAddress();
		String url = null;
		if (isa != null && urn != null) {
			StringBuffer addr = new StringBuffer("http://");
			addr.append(isa.getAddress().getHostAddress()).append(':')
			.append(isa.getPort()).append("/uri-res/N2R?");
			addr.append(urn.httpStringValue());
			url = addr.toString();
			addAppend(map, XS, url);
		}
		MagnetOptions magnet = new MagnetOptions(map);
		// set already known values
		magnet.urn = urn;
		if (url != null) {
			magnet.defaultURLs = new String[] { url };
		}
		return magnet;
	}
	
	/**
	 * Creates a MagnetOptions object from a several parameters.
	 * <p>
	 * The resulting MagnetOptions might not be 
	 * {@link #isDownloadable() downloadable}.
	 * @param keywordTopics can be <code>null</code>
	 * @param fileName can be <code>null</code>
	 * @param urn can be <code>null</code>
	 * @param defaultURLs can be <code>null</code>
	 * @return
	 */
	public static MagnetOptions createMagnet(String keywordTopics, String fileName,
											 URN urn, String[] defaultURLs) {
		HashMap map = new HashMap();
		map.put(KT, keywordTopics);
		map.put(DN, fileName);
		if (urn != null) {
			addAppend(map, XT, urn.httpStringValue());
		}
		if (defaultURLs != null) {
			for (int i = 0; i < defaultURLs.length; i++) {
				addAppend(map, AS, defaultURLs[i]);
			}
		}
		MagnetOptions magnet = new MagnetOptions(map);
		magnet.urn = urn;
		if (defaultURLs != null) {
			// copy array to protect against outside changes
			magnet.defaultURLs = new String[defaultURLs.length];
			System.arraycopy(defaultURLs, 0, magnet.defaultURLs, 0, 
					magnet.defaultURLs.length);
		}
		else {
			magnet.defaultURLs = new String[0];
		}
		return magnet;
	}
	
	/**
	 * Returns an empty array if the string could not be parsed.
	 * @param arg a string like "magnet:?xt.1=urn:sha1:49584DFD03&xt.2=urn:sha1:495345k"
	 * @return array may be empty, but is never <code>null</code>
	 */
	public static MagnetOptions[] parseMagnet(String arg) {
	    
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
		
		
		// Process each key=value pair
     	while (st.hasMoreTokens()) {
			Map curOptions;
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
			curOptions = (Map) options.get(iIndex);			
			if (curOptions == null) {
				curOptions = new HashMap();
				options.put(iIndex,curOptions);
			}
			
			if ( keystr.startsWith("xt") ) {
				addAppend(curOptions, XT, cmdstr);				
			} else if ( keystr.startsWith("dn") ) {
				curOptions.put(DN,cmdstr);
			} else if ( keystr.startsWith("kt") ) {
				curOptions.put(KT,cmdstr);
			} else if ( keystr.startsWith("xs") ) {
				addAppend(curOptions, XS, cmdstr );
			} else if ( keystr.startsWith("as") ) {
				addAppend(curOptions, AS, cmdstr );
			}
		}
		
		MagnetOptions[] ret = new MagnetOptions[options.size()];
		int i = 0;
		for (Iterator iter = options.values().iterator(); iter.hasNext(); i++) {
			Map current = (Map)iter.next();
			ret[i] = new MagnetOptions(current);
		}
		return ret;
	}
		
	
	private static void addAppend(Map map, String key, String value) {
		List l = (List) map.get(key);
		if (l == null) {
			l = new ArrayList(1);
			map.put(key,l);
		}
		l.add(value);
	}
	
    private MagnetOptions(Map options) {
		optionsMap = Collections.unmodifiableMap(options);
    }
    
	public String toString() {
		return toExternalForm();
	}
	
	/**
	 * Returns the magnet uri representation as it can be used in an html link.
	 * <p>
	 * Display name and keyword topic are url encoded.
	 * @return
	 */
	public String toExternalForm() {
		StringBuffer ret = new StringBuffer(MAGNET);
		
		for (Iterator iter = getExactTopics().iterator(); iter.hasNext();) {
			String xt = (String) iter.next();
			ret.append("&xt=").append(xt);
		}
		if (getDisplayName() != null) 
			ret.append("&dn=").append(URLEncoder.encode(getDisplayName()));
		if (getKeywordTopic() != null) 
			ret.append("&kt=").append(URLEncoder.encode(getKeywordTopic()));
		for (Iterator iter = getXS().iterator(); iter.hasNext();) {
			String xs = (String) iter.next();
			ret.append("&xs=").append(xs);
		}
		for (Iterator iter = getAS().iterator(); iter.hasNext();) {
			String as = (String) iter.next();
			ret.append("&as=").append(as);
		}

		return ret.toString();	
	}
	
	/**
	 * Returns the sha1 urn of this magnet uri if it has one.
	 * <p>
	 * It looks in the exacty topics, the exact sources and then in the alternate
	 * sources for it.
	 * @return
	 */
	public URN getSHA1Urn() {
		if (urn == null) {
			urn = extractSHA1URNFromList(getExactTopics());
			
			if (urn == null)
				urn = extractSHA1URNFromList(getXS());
			
			if (urn == null)
				urn = extractSHA1URNFromList(getAS());
			
			if (urn == null)
				urn = extractSHA1URNFromURLS(getDefaultURLs());
			
			if (urn == null)
				urn = URN.INVALID;
			
		}
		if (urn == URN.INVALID)
			return null;
		
		return urn;
	}
	
	private URN extractSHA1URNFromURLS(String[] defaultURLs) {
		for (int i = 0; i < defaultURLs.length; i++) {
			try {
				URI uri = new URI(defaultURLs[i].toCharArray());
				String query = uri.getQuery();
				if (query != null) {
					return URN.createSHA1Urn(uri.getQuery());
				}
			} catch (URIException e) {
			} catch (IOException e) {
			}
		}
		return null;
	}

	/**
	 * Returns true if there are enough pieces of information to start a
	 * download from it.
	 * <p>At any rate there has to be at least one default url or a sha1 and
	 * a non empty keyword topic/display name.
	 * @return
	 */
	public boolean isDownloadable() {
		 return getDefaultURLs().length > 0  
		 || (getSHA1Urn() != null && getQueryString() != null);
	}
	
	/**
	 * Returns whether the magnet has no other fields set than the hash.
	 * <p>
	 * If this is the case the user has to kick of a search manually.
	 * @return
	 */
	public boolean isHashOnly() {
		String kt = getKeywordTopic();
		String dn = getDisplayName();
		
		return (kt == null ||  kt.length()> 0) && 
			(dn == null || dn.length() > 0) &&
			getAS().isEmpty() && 
			getXS().isEmpty() &&
			!getExactTopics().isEmpty();
	}
	
	/**
	 * Returns a query string or <code>null</code> if there is none.
	 * @return
	 */
	public String getQueryString() {
		String kt = getKeywordTopic();
		if (kt != null && kt.length() > 0) {
			return kt;
		}
		String dn = getDisplayName();
		if (dn != null && dn.length() > 0) {
			return dn;
		}
		return null;
	}
	
	/**
	 * Returns true if only the keyword topic is specified.
	 * @return
	 */
	public boolean isKeywordTopicOnly() {
		String kt = getKeywordTopic();
		String dn = getDisplayName();
		
		return kt != null &&  kt.length() > 0 && 
			(dn == null || dn.length() > 0) &&
			getAS().isEmpty() && 
			getXS().isEmpty() &&
			getExactTopics().isEmpty();
	}
	
	private URN extractSHA1URNFromList(List strings) {
        for (Iterator iter = strings.iterator(); iter.hasNext(); ) {
            try {
                return URN.createSHA1Urn((String)iter.next());
            }
			catch (IOException e) {
            } 
        }
		return null;
    }
	
	private List getPotentialURLs() {
		List urls = new ArrayList();
		urls.addAll(getPotentialURLs(getExactTopics()));
		urls.addAll(getPotentialURLs(getXS()));
		urls.addAll(getPotentialURLs(getAS()));
		return urls;
	}
	
	private List getPotentialURLs(List strings) {
		List ret = new ArrayList();
		for (Iterator iter = strings.iterator(); iter.hasNext(); ) {
			String str = (String)iter.next();
			if (str.startsWith(HTTP))
				ret.add(str);
		}
		return ret;
	}
	 
	/**
	 * Returns all valid urls that can be tried for downloading.
	 * @return
	 */
	public String[] getDefaultURLs() {
		if (defaultURLs == null) {
			List urls = getPotentialURLs();
			for(Iterator it = urls.iterator(); it.hasNext(); ) {
				try {
					String nextURL = (String)it.next();
					new URI(nextURL.toCharArray());  // is it a valid URI?
				}
				catch(URIException e) {
					it.remove(); // if not, remove it from the list.
					localizedErrorMessage = e.getLocalizedMessage();
				}
			}
			defaultURLs = (String[])urls.toArray(new String[urls.size()]); 
		}
		return defaultURLs;
	}
	
	/**
	 * Returns the display name, i.e. filename or <code>null</code>.
	 * @return
	 */
    public String getDisplayName() {
        return (String)optionsMap.get(DN);
    }
    
    /**
     * Returns a file name that can be used for saving for a downloadable magnet.
     * <p>
     * Guaranteed to return a non-null value
     * @return 
     */
    public String getFileNameForSaving() {
    	if (extractedFileName != null) {
    		return extractedFileName;
    	}
    	extractedFileName = getDisplayName();
    	if (extractedFileName != null && extractedFileName.length() > 0) {
    		return extractedFileName;
    	}
    	extractedFileName = getKeywordTopic();
    	if (extractedFileName != null && extractedFileName.length() > 0) {
    		return extractedFileName;
    	}
    	URN urn = getSHA1Urn();
    	if (urn != null) {
    		extractedFileName = urn.toString();
    		return extractedFileName;
    	}
    	String[] urls = getDefaultURLs();
    	if (urls.length > 0) {
    		try {
    			URI uri = new URI(urls[0].toCharArray());
    			extractedFileName = extractFileName(uri);
    			if (extractedFileName != null && extractedFileName.length() > 0) {
    				return extractedFileName;
    			}
			} catch (URIException e) {
			}
    	}
    	try {
    		File file = File.createTempFile("magnet", "");
    		file.deleteOnExit();
    		extractedFileName = file.getName();
    		return extractedFileName;
    	} catch (IOException ie) {
    	}
    	extractedFileName = DOWNLOAD_PREFIX;
    	return extractedFileName;
    }
    
    /**
     * Returns the keyword topic if there is one, otherwise <code>null</code>.
     * @return
     */
    public String getKeywordTopic() {
        return (String)optionsMap.get(KT);
    }
    
    /**
     * Returns a list of exact topic strings, they can be url or urn string.
     * @return
     */
    public List getExactTopics() {
		return getList(XT); 
    }
    
    /**
     * Returns the list of exact source strings, they should be urls.
     * @return
     */
    public List getXS() {
        return getList(XS);
    }
	
    /**
     * Returns the list of alternate source string, they should  be urls.
     * @return
     */
    public List getAS() { 
        return getList(AS);
    }
	
	private List getList(String key) {
		List l = (List) optionsMap.get(key);
		return l == null ? Collections.EMPTY_LIST : l;
	}
    
	
	/**
	 * Returns a localized error message if of the last invalid url that was 
	 * parsed.
	 * @return null if there was no error
	 */
	public String getErrorMessage() {
		return localizedErrorMessage;
	}
	
	/** 
	 * Returns the filename to use for the download, guessed if necessary. 
     * @param uri the URL for the resource, which must not be <code>null</code>
     */
    public static String extractFileName(URI uri) {
    	//If the URL has a filename, return that.  Remember that URL.getFile()
        //may include directory information, e.g., "/path/file.txt" or "/path/".
        //It also returns "" if no file part.
        String path = null;
        String host = null;
		try {
			path = uri.getPath();
			host = uri.getHost();
		} catch (URIException e) {
		}
        if (path != null && path.length() > 0) {
            int i = path.lastIndexOf('/');
            if (i < 0)
                return path;                  //e.g., "file.txt"
            if (i >= 0 && i < (path.length()-1))
            	return path.substring(i+1);   //e.g., "/path/to/file"
        }
        
        //In the rare case of no filename ("http://www.limewire.com" or
        //"http://www.limewire.com/path/"), just make something up.
        if (host != null) {
        	return DOWNLOAD_PREFIX + host;
        }
        else {
        	return DOWNLOAD_PREFIX;
        }
    }
}

