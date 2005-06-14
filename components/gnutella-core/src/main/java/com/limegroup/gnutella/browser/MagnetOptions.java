package com.limegroup.gnutella.browser;

import java.io.IOException;
import java.io.Serializable;
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

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.URLDecoder;

/**
 * Contains information fields extracted from a magnet link.
 */
public class MagnetOptions implements Serializable {
	
	public static final String MAGNET    = "magnet:?";
	private static final String HTTP     = "http://";
	
	private final Map optionsMap;
	
	private static final String XS = "XS";
	private static final String XT = "XT";
	private static final String AS = "AS";
	private static final String DN = "DN";
	private static final String KT = "KT";
	
	private transient String [] defaultURLs;
	private transient String localizedErrorMessage;
	private transient URN urn;
	
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
		
		MagnetOptions [] ret = new MagnetOptions[options.size()];
		int i = 0;
		for (Iterator iter = options.values().iterator(); iter.hasNext(); i++) {
			Map current = (Map) iter.next();
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
		optionsMap  = Collections.unmodifiableMap(options);
    }
    
	public String toString() {
		return toMagnet();
	}
	
	public String toMagnet() {
		StringBuffer ret = new StringBuffer(MAGNET);
		
		for (Iterator iter = getExactTopics().iterator(); iter.hasNext();) {
			String xt = (String) iter.next();
			ret.append("&xt=").append(xt);
		}
		if (getDisplayName() != null) 
			ret.append("&dn=").append(URLEncoder.encode(getDisplayName()));
		if (getKeywordTopic() != null) 
			ret.append("&kt=").append(URLEncoder.encode(getKeywordTopic()));
		for (Iterator iter= getXS().iterator(); iter.hasNext();) {
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
			
		} else if (urn == URN.INVALID)
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
	 * Returns true if there are enough pieces of information to start e.g a
	 * download from it.
	 * <p>At any rate there has to be at least one default url and a sha1 or
	 * a non empty keyword topic.
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
	
    public String getDisplayName() {
        return (String)optionsMap.get(DN);
    }
    
    public String getKeywordTopic() {
        return (String)optionsMap.get(KT);
    }
    
    public List getExactTopics() {
		return getList(XT); 
    }
    
    public List getXS() {
        return getList(XS);
    }
	
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
}

