package com.limegroup.gnutella;

import com.limegroup.gnutella.*; 
import com.limegroup.gnutella.http.*; 
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;

/**
 * This class provides convenient data and utility functions to
 * huge test classes.
 */
public final class HugeTestUtils {

	public static final URL[] BAD_PORT_URLS = new URL[2];

	/**
	 * Strings representing invalid URNs.
	 */
	public static final String[] INVALID_URN_STRINGS = {
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFBC",
		"urn:sh1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"ur:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"rn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urnsha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		" urn:sHa1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn::sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn: sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1 :PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1 :PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1: PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWU GYQYPFB",
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWU GYQYPFB ",
		null,
		"",
		"test"
	};

	/**
	 * String representing valid URNs.
	 */
	public static final String[] VALID_URN_STRINGS = {
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:ZLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
		"Urn:sha1:ALSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
		"uRn:sHa1:QLRTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:WLPTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:Sha1:ELSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"UrN:sha1:RLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sHa1:ILSTIIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:FLSTXIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPZB",
		"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPZC",
		"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPZD",
		"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPZE",
		"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPRC",
		"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPSD",
		"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPTE",
	};

	/**
	 * Array of query strings.
	 */
	public static final String[] QUERY_STRINGS = {
		"urn:Sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"file",
		"different file",
		"file.mp3",
		"urn:",
		"urn:sha1",
		"good file",
		"urn:sha1:PLSTXIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"this one too",
		"urn:sha1:PLSTXIPQGSSZTS5FJUPAKUZWUGYQYPFBCCC",
		"urn:sha1:PLSTXIPQGSSZTS5FJUPAKUZWUGYQYR",
		"file.file",
		"the best file ever",
		"big file",
		"little file",
		"random file",
		"first file",
		"old file",
		"new file"
	};

	/**
	 * URLs for general use.
	 */
	public static final String [] HOST_STRINGS = {
		"www.limewire.com",
		"www.cnn.com",
		"www.test.org",
		"www.limewire.org",
		"www.eff.org",
		"www.guerrillanews.com",
		"www.natprior.org",
		"www.census.gov",
		"www.cmpbs.org",
		"www.lc.gov",
		"www.news.com",
		"www.oreilly.com",
		"www.apple.com",
		"www.gnutella.com",
		"www.gnutellanews.com",
		"java.sun.com",
		"www.jakarta.org",
	};

	/**
	 * Alternate locations with timestamps.
	 */
	public static final String[] VALID_TIMESTAMPED_LOCS = {
		"http://201.34.78.2:6352/get/2/"+
		                     "lime%20capital%20management%2001.mpg "+
		                     "2002-04-09T20:32:33Z",
		"http://201.34.78.4:6352/get/2/"+
		               "lime%20capital%20management%2002.mpg "+
		               "2002-04-09T20:32:34Z",
		"http://201.28.12.36:6352/get/2/"+
		               "lime%20capital%20management%2001.mpg "+
		               "2002-04-09T20:32:33Z",
		"http://201.98.12.36:6342/get/2/"+
		               "lime%20capital%20management%2001.mpg "+
		               "2002-04-09T20:32:33Z",
		"http://201.36.12.36:6351/get/2/"+
		               "lime%20capital%20management%2001.mpg "+
		               "2002-04-09T20:32:33Z",
		"http://201.90.12.36:6362/get/2/"+
		               "lime%20capital%20management%2001.mpg "+
		               "2002-04-09T20:32:33Z",
		"http://201.90.12.36:6352/get/2/"+
		               "lime%20capital%20management%2001.mpg "+
		               "\r\n2002-04-09T20:32:33Z",
		"http://201.90.12.36:6382/get/2/"+
		               "lime%20capital%20management%2001.mpg "+
		               "  \n\r\n2002-04-09T20:32:33Z",
		"http://201.90.12.36:6352/get/2/"+
            "lime%20capital%20management%2001.mpg "+
            "  \n\r\n2002-04-09T20:32:33Z",
		"http://201.90.12.36:6352/get/2/"+
            "lime%20capital%20management%2001.mpg "+
            "  \n\r\n2002-04-09T20:32:33Z"
	};

	/**
	 * Alternate locations without timestamps.
	 */
	public static final String[] VALID_NONTIMESTAMPED_LOCS = {
		"http://23.40.39.40:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://221.20.12.36:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://21.47.12.36:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://201.40.201.35:6322/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://40.17.12.36:6332/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://12.24.40.67:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://40.28.40.24:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg"
	};

	/**
	 * Alternate locations without timestamps that are not firewalled.
	 */
	public static final String[] NON_FIREWALLED_LOCS = {
		"http://50.40.39.40:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://51.20.12.36:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://52.47.12.36:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://53.40.201.35:6322/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://201.24.40.67:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://201.28.40.24:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg"
	};
	
    public static final String[] FIREWALLED_LOCS = {
		"http://192.168.39.40:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://127.20.12.36:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://10.47.12.36:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://172.16.201.35:6322/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://172.17.12.36:6332/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://172.18.40.67:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://172.31.40.24:6352/get/2/"+
	    "lime%20capital%20management%2001.mpg"
    };

	/**
	 * Invalid alternate locations.
	 */
	public static final String[] INVALID_LOCS = {
		"",
		null,
		"http",
		"http://",
		"www",
		"test",
		"http://www",
		"http://www ",
		"http: www",
		"www.test.com",
		"http://www.test.com",
		"http: //40.201.12.36:6332/get/2/"+
		    "lime%20capital%20management%2001.mpg",
	};
	

	/**
	 * Array of GUIDs for use by tests.
	 */
	public static final GUID[] GUIDS = {
		new GUID(GUID.makeGuid()),
		new GUID(GUID.makeGuid()),
		new GUID(GUID.makeGuid()),
		new GUID(GUID.makeGuid()),
		new GUID(GUID.makeGuid()),
		new GUID(GUID.makeGuid()),
		new GUID(GUID.makeGuid()),
		new GUID(GUID.makeGuid()),
		new GUID(GUID.makeGuid())
	};

	/**
	 * Array of URNs for use by tests.
	 */
	public static final URN[] URNS = new URN[VALID_URN_STRINGS.length];

	/**
	 * Array of URNType instances for use by tests.
	 */
	public static final UrnType[] URN_TYPES = new UrnType[VALID_URN_STRINGS.length];

	public static final URL[] UNEQUAL_URLS = new URL[HOST_STRINGS.length];
	public static final URL[] EQUAL_URLS = new URL[HOST_STRINGS.length];

	/**
	 * Array of unequal alternate locations for testing convenience.
	 */
	public static final AlternateLocation[] UNEQUAL_SHA1_LOCATIONS = 
		new AlternateLocation[UNEQUAL_URLS.length];

	/**
	 * Array of alternate locations with equal hashes but unequal host names 
	 * for testing convenience.
	 */
	public static final AlternateLocation[] EQUAL_SHA1_LOCATIONS = 
		new AlternateLocation[EQUAL_URLS.length];

	public static final Set[] URN_SETS = new Set[VALID_URN_STRINGS.length];

	/**
	 * A "unique" SHA1 for convenience.
	 */
	public static URN UNIQUE_SHA1;

	private static final HugeTestUtils INSTANCE = new HugeTestUtils();

	static {
		
		try {
			UNIQUE_SHA1 = 
				URN.createSHA1Urn("urn:sha1:PLSTHIFQGSJZT45FJUPAKUZWUGYQYPFB");
		} catch(IOException e) {
			e.printStackTrace();
		}
		try {
			BAD_PORT_URLS[0] = new URL("http", "www.limewire.org", -1, "test");
			BAD_PORT_URLS[1] = new URL("http", "www.limewire.org", 66000, "test");				
		} catch(MalformedURLException e) {
			e.printStackTrace();
		}

		for(int i=0; i<VALID_URN_STRINGS.length; i++) {
			try {
				URN urn = URN.createSHA1Urn(VALID_URN_STRINGS[i]);
				URNS[i] = urn;
				URN_TYPES[i] = urn.getUrnType();
				Set urnSet = new HashSet();
				urnSet.add(urn);
				URN_SETS[i] = urnSet;
			} catch(IOException e) {
				e.printStackTrace();
			}
		}

		for(int i=0; i<HOST_STRINGS.length; i++) {
			try {
				UNEQUAL_URLS[i] = 
					new URL("http", HOST_STRINGS[i], 6346, 
							"/uri-res/N2R?"+URNS[i].httpStringValue());
			} catch(MalformedURLException e) {
				// this should not happen
				e.printStackTrace();
			}
		}

		for(int i=0; i<HOST_STRINGS.length; i++) {
			try {
				EQUAL_URLS[i] = 
					new URL("http", HOST_STRINGS[i], 6346, 
							"/uri-res/N2R?"+URNS[0].httpStringValue());
			} catch(MalformedURLException e) {
				// this should not happen
				e.printStackTrace();
			}
		}

		for(int i=0; i<UNEQUAL_SHA1_LOCATIONS.length; i++) {
			try {
				UNEQUAL_SHA1_LOCATIONS[i] = 
					AlternateLocation.createAlternateLocation(UNEQUAL_URLS[i]);
			} catch(MalformedURLException e) {
				// this should not happen
				e.printStackTrace();
			}
		}

		for(int i=0; i<EQUAL_SHA1_LOCATIONS.length; i++) {
			try {
				EQUAL_SHA1_LOCATIONS[i] = 
					AlternateLocation.createAlternateLocation(EQUAL_URLS[0]);
			} catch(MalformedURLException e) {
				// this should not happen
				e.printStackTrace();
			}
		}

	}

	/**
	 * Array of URNType instances for use by tests.
	 */
	//static final UrnType[] URN_TYPES = new UrnType[VALID_URN_STRINGS.length];
	//private static final ArrayList URN_SETS = new ArrayList();
	//private static final HugeTestUtils INSTANCE = new HugeTestUtils();

	static HugeTestUtils instance() {
		return INSTANCE;
	}

	private HugeTestUtils() {
		/*
		for(int i=0; i<VALID_URN_STRINGS.length; i++) {
			try {
				URN urn = URN.createSHA1Urn(VALID_URN_STRINGS[i]);
				URNS[i] = urn;
				URN_TYPES[i] = urn.getUrnType();
				Set urnSet = new HashSet();
				urnSet.add(urn);
				URN_SETS.add(urnSet);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		*/
	}
	
}




