package com.limegroup.gnutella.browser;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import com.limegroup.gnutella.URN;

/**
 * Contains information fields extracted from a magnet link.
 */
public class MagnetOptions {
	
	public static final String MAGNET    = "magnet:?";
	private static final String HTTP     = "http://";
	
	private String _dn; 
	private String _kt;
    private List _xs;
	private List _xt;
    private List _as;
	private String errorMessage;
	
    public MagnetOptions() {
        _dn = null;
        _kt = null;
        _xs = new ArrayList();
        _xt = new ArrayList();
        _as = new ArrayList();
    }
    
	public String toString() {
		StringBuffer ret = new StringBuffer(MAGNET);
		
		int size = _xt.size();
        for (int i = 0; i < size; i++) 
            ret.append("&xt=").append(_xt.get(i));
		if (_dn != null) 
			ret.append("&dn=").append(URLEncoder.encode(_dn));
		if (_kt != null) 
			ret.append("&kt=").append(URLEncoder.encode(_kt));
		size = _xs.size();
        for (int i = 0; i < size; i++) 
            ret.append("&xs=").append(_xs.get(i));
		size = _as.size();
        for (int i = 0; i < size; i++) 
            ret.append("&as=").append(_as.get(i));
		
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
		URN urn = extractSHA1URNFromList(getExactTopics());
		
		if (urn == null) {
			urn = extractSHA1URNFromList(getXS());
		}
		if (urn == null) {
			urn = extractSHA1URNFromList(getAS());
		}
		return urn;
	}
	
	/**
	 * Returns true if there are enough pieces of information to start e.g a
	 * download from it.
	 * @return
	 */
	public boolean isValid() {
		 return getDefaultURLs().length > 0  && (getSHA1Urn() != null 
			|| (getKeywordTopic() != null && getKeywordTopic().length() > 0));
	}
	
	/**
	 * Returns whether the magnet has no other fields set than the hash.
	 * <p>
	 * If this is the case the user has to kick of a search manually.
	 * @return
	 */
	public boolean isHashOnly() {
		return _kt == null && _dn == null && _as.isEmpty() && _xs.isEmpty();
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
		List urls = getPotentialURLs();
		for(Iterator it = urls.iterator(); it.hasNext(); ) {
			try {
				String nextURL = (String)it.next();
				new URI(nextURL.toCharArray());  // is it a valid URI?
			}
			catch(URIException e) {
				it.remove(); // if not, remove it from the list.
				errorMessage = e.getLocalizedMessage();
             }
		}
		return (String[])urls.toArray(new String[urls.size()]);
	}
	
    public String getDisplayName() {
        return _dn;
    }
    
	/**
	 * Sets the display name.
	 * @param str
	 */
    public void setDisplayName(String str) {
        _dn = str;
    }

    public String getKeywordTopic() {
        return _kt;
    }
    
    public void setKeywordTopic(String str) {
        _kt = str;
    }

    public List getExactTopics() {
        return _xt;
    }
    
    public void addExactTopic(String str) {
        _xt.add(str);
    }

    public List getXS() {
        return _xs;
    }
    
    public void addXS(String str) {
        _xs.add(str);
    }

    public List getAS() {
        return _as;
    }
    
    public void addAS(String str) {
        _as.add(str);
    }
	
	/**
	 * Returns a localized error message if of the last invalid url that was 
	 * parsed.
	 * @return null if there was no error
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
}

