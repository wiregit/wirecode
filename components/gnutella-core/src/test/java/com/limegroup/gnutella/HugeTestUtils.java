package com.limegroup.gnutella;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.limewire.service.ErrorService;

import com.limegroup.gnutella.altlocs.AlternateLocation;

/**
 * This class provides convenient data and utility functions to
 * huge test classes.
 */
@SuppressWarnings("unchecked")
public final class HugeTestUtils {

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
		"try this",
		"file",
		"different file",
		"file.mp3",
		"urn",
		"urnsha1",
		"good file",
		"maybe this",
		"this one too",
		"how about this one",
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
	public static final URN.Type[] URN_TYPES = new URN.Type[VALID_URN_STRINGS.length];
    
    public static final String[] SOME_IPS = new String[] {
        "1.2.3.4",
        "1.2.3.5",
        "1.2.3.6",
        "1.2.3.7",
        "1.2.3.8",
        "1.2.3.9",
        "1.2.3.10",
        "1.2.3.11",
        "1.2.3.12",
        "1.2.3.13",
        "1.2.3.14",
        "1.2.3.15",
    };
    
    public static final String[] FIREWALLED_IPS = new String[] {
        "192.168.39.40:6352",
        "127.20.12.36:6352",
        "10.47.12.36:6352",
        "172.16.201.35:6322",
        "172.17.12.36:6332",
        "172.18.40.67:6352",
        "172.31.40.24:6352",
    };

	/**
	 * Array of unequal alternate locations for testing convenience.
	 */
	public static final AlternateLocation[] UNEQUAL_SHA1_LOCATIONS = 
		new AlternateLocation[SOME_IPS.length];

	/**
	 * Array of alternate locations with equal hashes but unequal host names 
	 * for testing convenience.
	 */
	public static final AlternateLocation[] EQUAL_SHA1_LOCATIONS = 
		new AlternateLocation[SOME_IPS.length];

	public static final Set[] URN_SETS = new Set[VALID_URN_STRINGS.length];

	/**
	 * A "unique" SHA1 for convenience.
	 */
	public static URN UNIQUE_SHA1;

	public static URN SHA1;

	static {
		
		try {
			UNIQUE_SHA1 = 
				URN.createSHA1Urn("urn:sha1:PLSTHIFQGSJZT45FJUPAKUZWUGYQYPFB");
		} catch(IOException e) {
			ErrorService.error(e);
		}

		try {
			SHA1 = URN.createSHA1Urn(VALID_URN_STRINGS[3]);
		} catch(IOException e) {
			ErrorService.error(e);
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
				ErrorService.error(e);
			}
		}

		for(int i=0; i<UNEQUAL_SHA1_LOCATIONS.length; i++) {
			try {
				UNEQUAL_SHA1_LOCATIONS[i] = AlternateLocation.create(SOME_IPS[i], URNS[i]);
			} catch(IOException e) {
				// this should not happen
				ErrorService.error(e);
			}
		}

		for(int i=0; i<EQUAL_SHA1_LOCATIONS.length; i++) {
			try {
				EQUAL_SHA1_LOCATIONS[i] = AlternateLocation.create(SOME_IPS[i], URNS[0]);
			} catch(IOException e) {
				// this should not happen
				ErrorService.error(e);
			}
		}
	}
}




