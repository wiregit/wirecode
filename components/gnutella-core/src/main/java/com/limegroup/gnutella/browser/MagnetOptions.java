package com.limegroup.gnutella.browser;

/**
 * Contains information fields extracted from a magnet link.
 */
public class MagnetOptions {
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


