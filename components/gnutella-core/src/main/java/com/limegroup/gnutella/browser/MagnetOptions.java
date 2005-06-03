package com.limegroup.gnutella.browser;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains information fields extracted from a magnet link.
 */
public class MagnetOptions {
	public static final String MAGNET    = "magnet:?";
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

