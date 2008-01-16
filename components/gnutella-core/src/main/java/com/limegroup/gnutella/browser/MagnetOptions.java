package com.limegroup.gnutella.browser;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.EncodingUtils;
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
	
	private final Map<Option, List<String>> optionsMap;
    
    private enum Option {
        XS, XT, AS, DN, KT;
        
        public static Option valueFor(String str) {
            for(Option option : values()) {
                if(str.toUpperCase(Locale.US).startsWith(option.toString()))
                    return option;
            }
            return null;
        }
        
        
    }
	
	private transient String [] defaultURLs;
	private transient String localizedErrorMessage;
	private transient URN urn;
	private transient String extractedFileName;
	private transient List<URN> guidUrns;
	
	/**
	 * Creates a MagnetOptions object from file details.
	 * <p>
	 * The resulting MagnetOptions might not be 
	 * {@link #isDownloadable() downloadable}.
	 * @param fileDetails
	 * @return
	 */
	public static MagnetOptions createMagnet(FileDetails fileDetails) {
		Map<Option, List<String>> map = new HashMap<Option, List<String>>();
        List<String> name = new ArrayList<String>(1);
        name.add(fileDetails.getFileName());
		map.put(Option.DN, name);
		URN urn = fileDetails.getSHA1Urn();
		if (urn != null) {
			addAppend(map, Option.XT, urn.httpStringValue());
		}
		InetSocketAddress isa = fileDetails.getInetSocketAddress();
		String url = null;
		if (isa != null && urn != null) {
            StringBuilder addr = new StringBuilder("http://");
			addr.append(isa.getAddress().getHostAddress()).append(':')
			.append(isa.getPort()).append("/uri-res/N2R?");
			addr.append(urn.httpStringValue());
			url = addr.toString();
			addAppend(map, Option.XS, url);
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
		Map<Option, List<String>> map = new HashMap<Option, List<String>>();
        List<String> kt = new ArrayList<String>(1);
        kt.add(keywordTopics);
		map.put(Option.KT, kt);
        List<String> dn = new ArrayList<String>(1);
        dn.add(fileName);
		map.put(Option.DN, dn);
		if (urn != null) {
			addAppend(map, Option.XT, urn.httpStringValue());
		}
		if (defaultURLs != null) {
			for (int i = 0; i < defaultURLs.length; i++) {
				addAppend(map, Option.AS, defaultURLs[i]);
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

	/**
	 * Returns an empty array if the string could not be parsed.
	 * @param arg a string like "magnet:?xt.1=urn:sha1:49584DFD03&xt.2=urn:sha1:495345k"
	 * @return array may be empty, but is never <code>null</code>
	 */
	public static MagnetOptions[] parseMagnet(String arg) {
	    
		Map<Integer, Map<Option, List<String>>> options = new HashMap<Integer, Map<Option, List<String>>>();

		// Strip out any single quotes added to escape the string
		if ( arg.startsWith("'") )
			arg = arg.substring(1);
		if ( arg.endsWith("'") )
			arg = arg.substring(0,arg.length()-1);
		
		// Parse query
		if (!arg.toLowerCase(Locale.US).startsWith(MagnetOptions.MAGNET))
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
			Map<Option, List<String>> curOptions;
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
			curOptions = options.get(iIndex);			
			if (curOptions == null) {
				curOptions = new HashMap<Option, List<String>>();
				options.put(iIndex,curOptions);
			}
			
            Option option = Option.valueFor(keystr);
            if(option != null)
                addAppend(curOptions, option, cmdstr);
		}
		
		MagnetOptions[] ret = new MagnetOptions[options.size()];
		int i = 0;
        for(Map<Option, List<String>> current : options.values()) 
            ret[i++] = new MagnetOptions(current);
		return ret;
	}
		
	
	private static void addAppend(Map<Option, List<String>> map, Option key, String value) {
		List<String> l = map.get(key);
		if (l == null) {
			l = new ArrayList<String>(1);
			map.put(key,l);
		}
		l.add(value);
	}
	
    private MagnetOptions(Map<Option, List<String>> options) {
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
        StringBuilder ret = new StringBuilder(MAGNET);
		
        for(String xt : getExactTopics())
			ret.append("&xt=").append(xt);

		if (getDisplayName() != null) 
			ret.append("&dn=").append(EncodingUtils.encode(getDisplayName()));
        
		if (getKeywordTopic() != null) 
			ret.append("&kt=").append(EncodingUtils.encode(getKeywordTopic()));
        
        for(String xs : getXS())
			ret.append("&xs=").append(xs);
        
        for(String as : getAS())
			ret.append("&as=").append(as);

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
		 if (getDefaultURLs().length > 0) {
		     return true;
		 }
		 if (getSHA1Urn() != null) {
		     if (getQueryString() != null) {
		         return true;
		     }
		     if (!getGUIDUrns().isEmpty()) {
		         return true;
		     }
		 }
		 return false;
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
	
	private URN extractSHA1URNFromList(List<String> strings) {
        for(String str : strings) {
            try {
                return URN.createSHA1Urn(str);
            } catch (IOException ignored) {} 
        }
		return null;
    }
	
	private List<String> getPotentialURLs() {
		List<String> urls = new ArrayList<String>();
		urls.addAll(getPotentialURLs(getExactTopics()));
		urls.addAll(getPotentialURLs(getXS()));
		urls.addAll(getPotentialURLs(getAS()));
		return urls;
	}
	
	private List<String> getPotentialURNs() {
        List<String> urls = new ArrayList<String>();
        urls.addAll(getPotentialURNs(getExactTopics()));
        urls.addAll(getPotentialURNs(getXS()));
        urls.addAll(getPotentialURNs(getAS()));
        return urls;
    }
	
	private List<String> getPotentialURNs(List<String> strings) {
        List<String> ret = new ArrayList<String>();
        for(String str: strings) {
            if(str.toLowerCase(Locale.US).startsWith(URN.Type.URN_NAMESPACE_ID))
                ret.add(str);
        }
        return ret;
    }
	
	private List<String> getPotentialURLs(List<String> strings) {
		List<String> ret = new ArrayList<String>();
        for(String str: strings) {
            if(str.toLowerCase(Locale.US).startsWith(HTTP))
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
			List<String> urls = getPotentialURLs();
			for(Iterator<String> it = urls.iterator(); it.hasNext(); ) {
				try {
					String nextURL = it.next();
					new URI(nextURL.toCharArray());  // is it a valid URI?
				} catch(URIException e) {
					it.remove(); // if not, remove it from the list.
					localizedErrorMessage = e.getLocalizedMessage();
				}
			}
			defaultURLs = urls.toArray(new String[urls.size()]); 
		}
		return defaultURLs;
	}
	
	/**
	 * Returns immutable list of all valid GUID urns than can be tried for downloading.
	 * 
	 * GUID urns denote possibly firewalled hosts in the network that can be looked up
	 * as possible download sources for this magnet link.
	 */
    public List<URN> getGUIDUrns() {
        if (guidUrns != null) {
            return guidUrns;
        }
        List<URN> urns = null;
        List<String> potentialUrns = getPotentialURNs();
        for (String candidate : potentialUrns) {
            try {
                URN urn = URN.createGUIDUrn(candidate);
                if (urn.isGUID()) {
                    if (urns == null) {
                        urns = new ArrayList<URN>(2);
                    }
                    urns.add(urn);
                }
            } catch (IOException ie) {
                // ignore, just not a valid guid urn 
            }
        }
        if (urns == null) {
            urns = Collections.emptyList();
        } else {
            urns = Collections.unmodifiableList(urns);
        }
        // only set after list is full to avoid race condition where some other thread sees the half full list
        guidUrns = urns;
        return urns;
    }
	
	/**
	 * Returns the display name, i.e. filename or <code>null</code>.
	 * @return
	 */
    public String getDisplayName() {
        List<String> list = optionsMap.get(Option.DN);
        if(list == null || list.isEmpty())
            return null;
        else
            return list.get(0);
    }
    
    /**
     * Returns a file name that can be used for saving for a downloadable magnet.
     * <p>
     * Guaranteed to return a non-null value
     * @return 
     */
    public String getFileNameForSaving() {
        if (extractedFileName != null) 
            return extractedFileName;
        
    	String name = getRawNameForSaving();
        
        // remove any leading slashes or dots
        while(name.startsWith(".") || name.startsWith("\\") || name.startsWith("/"))
            name = name.substring(1);
            
        extractedFileName = name;
        return extractedFileName;
    }
    
    private String getRawNameForSaving() {
        
        String tempFileName = getDisplayName();
        if (tempFileName != null && tempFileName.length() > 0) {
            return tempFileName;
        }
        tempFileName = getKeywordTopic();
        if (tempFileName != null && tempFileName.length() > 0) {
            return tempFileName;
        }
        URN urn = getSHA1Urn();
        if (urn != null) {
            tempFileName = urn.toString();
            return tempFileName;
        }
        String[] urls = getDefaultURLs();
        if (urls.length > 0) {
            try {
                URI uri = new URI(urls[0].toCharArray());
                tempFileName = extractFileName(uri);
                if (tempFileName != null && tempFileName.length() > 0) {
                    return tempFileName;
                }
            } catch (URIException e) {
            }
        }
        try {
            File file = FileUtils.createTempFile("magnet", "");
            file.deleteOnExit();
            tempFileName = file.getName();
            return tempFileName;
        } catch (IOException ie) {
        }
        tempFileName = DOWNLOAD_PREFIX;
        return tempFileName;
    }
    
    /**
     * Returns the keyword topic if there is one, otherwise <code>null</code>.
     * @return
     */
    public String getKeywordTopic() {
        List<String> list = optionsMap.get(Option.KT);
        if(list == null || list.isEmpty())
            return null;
        else
            return list.get(0);
    }
    
    /**
     * Returns a list of exact topic strings, they can be url or urn string.
     * @return
     */
    public List<String> getExactTopics() {
		return getList(Option.XT); 
    }
    
    /**
     * Returns the list of exact source strings, they should be urls.
     * @return
     */
    public List<String> getXS() {
        return getList(Option.XS);
    }
	
    /**
     * Returns the list of alternate source string, they should  be urls.
     * @return
     */
    public List<String> getAS() { 
        return getList(Option.AS);
    }
	
	private List<String> getList(Option key) {
		List<String> l = optionsMap.get(key);
        if(l == null)
            return Collections.emptyList();
        else
            return l;
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

