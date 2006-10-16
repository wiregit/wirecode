package com.limegroup.gnutella.archive;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Archives {
    
	/** Internet Archive Media Types */
	
	/** Contributions of media type movies will default to 
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
	
	/** Contributions of media type audio will default to
	 * the open-source audio collection
	 */
	public static final int MEDIA_AUDIO = 3;
	private static final Integer _mediaAudio =
		new Integer( MEDIA_AUDIO );
	
	/* no contribution of software yet from general community
	 * (Oct 2005)
	 */
//	public static final int MEDIA_SOFTWARE = 4;

	private static final Map<Integer, String> _mediaStrings;
	
	static {
		Map<Integer, String> m = new HashMap<Integer, String>();
		
		m.put( _mediaMovies, "movies" );
		m.put( _mediaAudio, "audio" );
		
		_mediaStrings = Collections.unmodifiableMap(m);
	}
	

	/* Dublin-core types.  See http://dublincore.org/documents/dcmi-type-vocabulary/ */
	
	public static final int TYPE_MOVING_IMAGE = 1;
	private static final Integer _typeMovingImage = new Integer( TYPE_MOVING_IMAGE );
	
	public static final int TYPE_SOUND = 2;
	private static final Integer _typeSound = new Integer( TYPE_SOUND );
	
	private static final Map<Integer, String> _typeStrings;
	
	static {
		Map<Integer, String> m = new HashMap<Integer, String>();
		
		m.put( _typeMovingImage, "MovingImage" );
		m.put( _typeSound, "Sound" );
		
		_typeStrings = Collections.unmodifiableMap( m );
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
	
	private static final Map<Integer, String> _collectionStrings;
	private static final Map<Integer, Integer> _defaultCollectionsForMedia;
	private static final Map<Integer, Integer> _defaultTypesForMedia;
	
	static {
		Map<Integer, String> mCS = new HashMap<Integer, String>();
		Map<Integer, Integer> mDCFM = new HashMap<Integer, Integer>();
		Map<Integer, Integer> mDTFM = new HashMap<Integer, Integer>();
		
		mCS.put( _collectionOpensourceMovies, "opensource_movies" );		
		mCS.put( _collectionOpensourceAudio, "opensource_audio" );
		_collectionStrings = Collections.unmodifiableMap( mCS );
		
		mDCFM.put( _mediaMovies, _collectionOpensourceMovies );
		mDTFM.put( _mediaMovies, _typeMovingImage );
				
		mDCFM.put( _mediaAudio, _collectionOpensourceMovies );
		mDTFM.put( _mediaAudio, _typeSound );
		
		_defaultCollectionsForMedia = Collections.unmodifiableMap( mDCFM );
		_defaultTypesForMedia = Collections.unmodifiableMap( mDTFM );
	}

	static String getMediaString( int media ) {
		return _mediaStrings.get( new Integer( media ) );
	}
	
	static String getCollectionString( int collection ) {
		return  _collectionStrings.get( new Integer( collection ) );
	}
	
	static String getTypeString( int type ) {
		return _typeStrings.get( new Integer( type ));
	}
	
	/**
	 * 
	 * @param media
	 * @return
	 * @throws IllegalArgumentException
	 *         If media is not valid
	 */
	static int defaultCollectionForMedia( int media ) {
		Integer c =  _defaultCollectionsForMedia.get( new Integer( media ) );
		
		if ( c == null ) {
			throw new IllegalArgumentException( "Invalid media type: " + media );
		}
		return c.intValue();
	}
	
	/**
	 * 
	 * @param media
	 * @return
	 * @ throws IllegalArgumentException
	 *          If media is not valid
	 */
	static int defaultTypesForMedia( int media ) {
		Integer c = _defaultTypesForMedia.get( new Integer( media ));
		
		if (c == null ) {
			throw new IllegalArgumentException( "Invalid media type: " + media );
		}
		return c.intValue();
	}

	
	// first character can only be alphanumberic
	private static final Pattern BAD_BEGINNING_CHARS =
		Pattern.compile( "^[^\\p{Alnum}]+" );
	
	// only allow alphanumerics and . - _
	private static final Pattern BAD_CHARS = 
		Pattern.compile( "[^\\p{Alnum}\\.\\-_]" );
	private static final String REPLACE_STR = "_";
	
	public static String normalizeName( String name ) {
		final int MIN_LENGTH = 5;
		final int MAX_LENGTH = 100;

		
		if ( name == null )
			return null;
		
		// chop off all bad beginning characters
		name = BAD_BEGINNING_CHARS.matcher( name ).replaceFirst("");
		
		name = BAD_CHARS.matcher( name ).replaceAll(REPLACE_STR);
		
		final StringBuilder nameBuf = new StringBuilder( name );
		
		while ( nameBuf.length() < MIN_LENGTH ) {
			nameBuf.append( REPLACE_STR );
		}
		 
		
		if ( nameBuf.length() > MAX_LENGTH ) {
			nameBuf.setLength( MAX_LENGTH );
		}

		return nameBuf.toString(); 
	}

	/* factory methods */
	
	public static Contribution createContribution( String username, String password, 
			String title, String description, int media)
	throws DescriptionTooShortException {
		return new AdvancedContribution( username, password, title, description, media );
	}

	public static Contribution createContribution( String username, String password, 
			String title, String description, int media, int collection, int type )
	throws DescriptionTooShortException {
		return new AdvancedContribution( username, password, title, description, media, collection, type );
	}
	
	/**
	 * checks if the given description would be valid 
	 * @param description
	 * @return
	 */
	public static void checkDescription( String description )
		throws DescriptionTooShortException
	{
		AdvancedContribution.checkDescription( description );
	}
	
}
