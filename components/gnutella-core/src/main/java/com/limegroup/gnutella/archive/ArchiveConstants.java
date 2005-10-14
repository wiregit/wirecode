package com.limegroup.gnutella.archive;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ArchiveConstants {
	private String __id__ = "$Id: ArchiveConstants.java,v 1.1.2.1 2005-10-14 23:27:03 tolsen Exp $";
	
	/** Internet Archive Media Types */
	
	/** Contributions of type movies will default to 
	 *  the open-source movies collection
	 */
	public static final int MEDIA_MOVIES = 1;
	private static final Integer _mediaMovies = 
		new Integer( MEDIA_MOVIES );

	
	/* doesn't look like Internet Archive is accepting even
	 * open-source/CC books from the general community
	 * (Oct 2005)
	 */
	//public static final int MEDIA_TEXTS = 2;
	
	/** Contributions of type audio will default to
	 * the open-source audio collection
	 */
	public static final int MEDIA_AUDIO = 3;
	private static final Integer _mediaAudio =
		new Integer( MEDIA_AUDIO );
	
	/* no contribution of software yet from general community
	 * (Oct 2005)
	 */
//	public static final int MEDIA_SOFTWARE = 4;

	private static final Map _mediaStrings;
	
	static {
		Map m = new HashMap();
		
		m.put( _mediaMovies, "movies" );
		m.put( _mediaAudio, "audio" );
		
		_mediaStrings = Collections.unmodifiableMap(m);
	}
	

	/* Just use open-source collections for now */
	
	/**** Movie Collections ****/
	
	/** Open-Source Movies - CC-licensed movies */

	
	public static final int COLLECTION_OPENSOURCE_MOVIES = 1;
	private static final Integer _collectionOpensourceMovies =
		new Integer( COLLECTION_OPENSOURCE_MOVIES );

	
	/**** Audio Collections ****/
	
	/** Open-Source Audio - CC-licensed audio */

	public static final int COLLECTION_OPENSOURCE_AUDIO = 2;
	private static final Integer _collectionOpensourceAudio =
		new Integer( COLLECTION_OPENSOURCE_AUDIO );
	
	private static final Map _collectionStrings;
	private static final Map _defaultCollectionsForType;
	
	static {
		Map mCS = new HashMap();
		Map mDCFT = new HashMap();
		
		mCS.put( _collectionOpensourceMovies, "opensource_movies" );		
		mCS.put( _collectionOpensourceAudio, "opensource_audio" );
		_collectionStrings = Collections.unmodifiableMap( mCS );
		
		mDCFT.put( _mediaMovies, _collectionOpensourceMovies );
				
		mDCFT.put( _mediaAudio, _collectionOpensourceMovies );
		_defaultCollectionsForType = Collections.unmodifiableMap( mDCFT );
	}

	static String getMediaString( int media ) {
		return (String) _mediaStrings.get( new Integer( media ) );
	}
	
	static String getCollectionString( int collection ) {
		return (String) _collectionStrings
			.get( new Integer( collection ) );
	}
	
	static int defaultCollectionForType( int type ) 
	throws IllegalArgumentException {
		Integer c = (Integer) _defaultCollectionsForType.get( new Integer( type ) );
		
		if ( c == null ) {
			throw new IllegalArgumentException( "Invalid media type: " + type );
		}
		return c.intValue();
	}
	
}
