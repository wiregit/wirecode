package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*; 
import com.sun.java.util.collections.*;
import java.io.*;

/**
 * This class provides convenient data and utility functions to
 * huge test classes.
 */
final class HugeTestUtils {

	/**
	 * Strings representing invalid URNs.
	 */
	static final String[] INVALID_URN_STRINGS = {
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
	static final String[] VALID_URN_STRINGS = {
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:ZLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
		"Urn:sha1:ALSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
		"uRn:sHa1:QLRTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:WLPTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:Sha1:ELSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"UrN:sha1:RLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sHa1:ILSTIIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:FLSTXIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPFB"
	};

	/**
	 * Array of query strings.
	 */
	static final String[] QUERY_STRINGS = {
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
	static final String [] URL_STRINGS = {
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
		"java.sun.com"
	};

	/**
	 * Alternate locations with timestamps.
	 */
	static final String[] VALID_TIMESTAMPED_LOCS = {
		"http://Y.Y.Y.Y:6352/get/2/"+
		                     "lime%20capital%20management%2001.mpg "+
		                     "2002-04-09T20:32:33Z",
		"http://Y.Y.Y.Y:6352/get/2/"+
		               "lime%20capital%20management%2002.mpg "+
		               "2002-04-09T20:32:33Z",
		"http://Y.Z.Y.Y:6352/get/2/"+
		               "lime%20capital%20management%2001.mpg "+
		               "2002-04-09T20:32:33Z",
		"http://Y.W.Y.Y:6352/get/2/"+
		               "lime%20capital%20management%2001.mpg "+
		               "2002-04-09T20:32:33Z",
		"http://Y.T.Y.Y:6352/get/2/"+
		               "lime%20capital%20management%2001.mpg "+
		               "2002-04-09T20:32:33Z",
		"http: //Y.R.Y.Y:6352/get/2/"+
		               "lime%20capital%20management%2001.mpg "+
		               "2002-04-09T20:32:33Z"
	};

	/**
	 * Alternate locations without timestamps.
	 */
	static final String[] VALID_NONTIMESTAMPED_LOCS = {
		HTTPHeaderName.ALT_LOCATION.httpStringValue()+
		    ": http://Y.Y.Y.Y:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		HTTPHeaderName.ALT_LOCATION.httpStringValue()+
		    ": http://Y.X.Y.Y:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		HTTPHeaderName.ALT_LOCATION.httpStringValue()+
		    ": http://Y.R.Y.Y:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://Y.Y.Y.Y:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http: //Y.Y.Y.Y:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://Y.S.Y.Y:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://Y.Z.Y.Y:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg"
	};

	/**
	 * Invalid alternate locations.
	 */
	static final String[] INVALID_LOCS = {
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
		"http://www.test.com"
	};
	

	/**
	 * Array of GUIDs for use by tests.
	 */
	static final GUID[] GUIDS = {
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
	static final URN[] URNS = new URN[VALID_URN_STRINGS.length];

	/**
	 * Array of URNType instances for use by tests.
	 */
	static final UrnType[] URN_TYPES = new UrnType[VALID_URN_STRINGS.length];
	private static final ArrayList URN_SETS = new ArrayList();
	private static final HugeTestUtils INSTANCE = new HugeTestUtils();

	static HugeTestUtils instance() {
		return INSTANCE;
	}

	private HugeTestUtils() {
		for(int i=0; i<VALID_URN_STRINGS.length; i++) {
			try {
				URN urn = URNFactory.createUrn(VALID_URN_STRINGS[i]);
				URNS[i] = urn;
				URN_TYPES[i] = urn.getUrnType();
				Set urnSet = new HashSet();
				urnSet.add(urn);
				URN_SETS.add(urnSet);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}




