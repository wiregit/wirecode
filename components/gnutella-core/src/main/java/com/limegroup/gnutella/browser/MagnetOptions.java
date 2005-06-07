package com.limegroup.gnutella.browser;

import java.io.IOException;
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
	
    public MagnetOptions() {
        _dn = null;
        _kt = null;
        _xs = new ArrayList();
        _xt = new ArrayList();
        _as = new ArrayList();
    }
    
	public String toString() {
		String ret = MAGNET;
		
		int size = _xt.size();
        for (int i = 0; i < size; i++) 
            ret += "&xt="+_xt.get(i)+"";
		if ( _dn != null ) 
			ret += "&dn="+_dn+"";
		if ( _kt != null ) 
			ret += "&kt="+_kt+"";
		size = _xs.size();
        for (int i = 0; i < size; i++) 
            ret += "&xs="+_xs.get(i)+"";
		size = _as.size();
        for (int i = 0; i < size; i++) 
            ret += "&as="+_as.get(i)+"";
		return ret;
	}
	
	public URN getSHA1Urn() {
		URN urn = extractSHA1URNFromList(getXT());
		
		if (urn == null) {
			urn = extractSHA1URNFromList(getXS());
		}
		if (urn == null) {
			urn = extractSHA1URNFromList(getAS());
		}
		return urn;
	}
	
	public boolean isValid() {
		 return getDefaultURLs().length > 0  || getSHA1Urn() != null 
			|| (getKT() != null && getKT().length() > 0);
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
	
	public List getPotentialURLs() {
		List urls = new ArrayList();
		urls.addAll(getPotentialURLs(getXT()));
		urls.addAll(getPotentialURLs(getXS()));
		urls.addAll(getPotentialURLs(getAS()));
		return urls;
	}
	
	public List getPotentialURLs(List strings) {
		List ret = new ArrayList();
		for (Iterator iter = strings.iterator(); iter.hasNext(); ) {
			String str = (String)iter.next();
			if (str.startsWith(HTTP))
				ret.add(str);
		}
		return ret;
	}
	 
	 public String[] getDefaultURLs() {
		 List urls = getPotentialURLs();
		 for(Iterator it = urls.iterator(); it.hasNext(); ) {
			 try {
				 String nextURL = (String)it.next();
				 new URI(nextURL.toCharArray());  // is it a valid URI?
			 }
			 catch(URIException e) {
				 it.remove(); // if not, remove it from the list.
             }
		 }
		 return (String[])urls.toArray(new String[urls.size()]);
	 }
    
    public String getDN() {
        return _dn;
    }
    
    public void setDN(String str) {
        _dn = str;
    }

    public String getKT() {
        return _kt;
    }
    
    public void setKT(String str) {
        _kt = str;
    }

    public List getXT() {
        return _xt;
    }
    
    public void addXT(String str) {
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
}

