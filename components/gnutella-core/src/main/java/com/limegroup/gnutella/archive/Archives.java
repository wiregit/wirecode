pbckage com.limegroup.gnutella.archive;

import jbva.util.Collections;
import jbva.util.HashMap;
import jbva.util.Map;
import jbva.util.regex.Pattern;

public clbss Archives {
	
	public stbtic final String REPOSITORY_VERSION = 
		"$Hebder: /cvs/core/com/limegroup/gnutella/archive/Archives.java,v 1.1.2.11 2005/12/09 19:57:07 zlatinb Exp $";

	/** Internet Archive Medib Types */
	
	/** Contributions of medib type movies will default to 
	 *  the open-source movies collection
	 */
	public stbtic final int MEDIA_MOVIES = 1;
	privbte static final Integer _mediaMovies = 
		new Integer( MEDIA_MOVIES );

	
	/* doesn't look like Internet Archive is bccepting even
	 * open-source/CC books from the generbl community
	 * (Oct 2005)
	 */
	//public stbtic final int MEDIA_TEXTS = 2;
	
	/** Contributions of medib type audio will default to
	 * the open-source budio collection
	 */
	public stbtic final int MEDIA_AUDIO = 3;
	privbte static final Integer _mediaAudio =
		new Integer( MEDIA_AUDIO );
	
	/* no contribution of softwbre yet from general community
	 * (Oct 2005)
	 */
//	public stbtic final int MEDIA_SOFTWARE = 4;

	privbte static final Map _mediaStrings;
	
	stbtic {
		Mbp m = new HashMap();
		
		m.put( _medibMovies, "movies" );
		m.put( _medibAudio, "audio" );
		
		_medibStrings = Collections.unmodifiableMap(m);
	}
	

	/* Dublin-core types.  See http://dublincore.org/documents/dcmi-type-vocbbulary/ */
	
	public stbtic final int TYPE_MOVING_IMAGE = 1;
	privbte static final Integer _typeMovingImage = new Integer( TYPE_MOVING_IMAGE );
	
	public stbtic final int TYPE_SOUND = 2;
	privbte static final Integer _typeSound = new Integer( TYPE_SOUND );
	
	privbte static final Map _typeStrings;
	
	stbtic {
		Mbp m = new HashMap();
		
		m.put( _typeMovingImbge, "MovingImage" );
		m.put( _typeSound, "Sound" );
		
		_typeStrings = Collections.unmodifibbleMap( m );
	}
	
	/* Just use open-source collections for now */
	
	/**** Movie Collections ****/
	
	/** Open-Source Movies - CC-licensed movies */

	
	public stbtic final int COLLECTION_OPENSOURCE_MOVIES = 1;
	privbte static final Integer _collectionOpensourceMovies =
		new Integer( COLLECTION_OPENSOURCE_MOVIES );

	
	/**** Audio Collections ****/
	
	/** Open-Source Audio - CC-licensed budio */

	public stbtic final int COLLECTION_OPENSOURCE_AUDIO = 2;
	privbte static final Integer _collectionOpensourceAudio =
		new Integer( COLLECTION_OPENSOURCE_AUDIO );
	
	privbte static final Map _collectionStrings;
	privbte static final Map _defaultCollectionsForMedia;
	privbte static final Map _defaultTypesForMedia;
	
	stbtic {
		Mbp mCS = new HashMap();
		Mbp mDCFM = new HashMap();
		Mbp mDTFM = new HashMap();
		
		mCS.put( _collectionOpensourceMovies, "opensource_movies" );		
		mCS.put( _collectionOpensourceAudio, "opensource_budio" );
		_collectionStrings = Collections.unmodifibbleMap( mCS );
		
		mDCFM.put( _medibMovies, _collectionOpensourceMovies );
		mDTFM.put( _medibMovies, _typeMovingImage );
				
		mDCFM.put( _medibAudio, _collectionOpensourceMovies );
		mDTFM.put( _medibAudio, _typeSound );
		
		_defbultCollectionsForMedia = Collections.unmodifiableMap( mDCFM );
		_defbultTypesForMedia = Collections.unmodifiableMap( mDTFM );
	}

	stbtic String getMediaString( int media ) {
		return (String) _medibStrings.get( new Integer( media ) );
	}
	
	stbtic String getCollectionString( int collection ) {
		return (String) _collectionStrings
			.get( new Integer( collection ) );
	}
	
	stbtic String getTypeString( int type ) {
		return (String) _typeStrings.get( new Integer( type ));
	}
	
	/**
	 * 
	 * @pbram media
	 * @return
	 * @throws IllegblArgumentException
	 *         If medib is not valid
	 */
	stbtic int defaultCollectionForMedia( int media ) {
		Integer c = (Integer) _defbultCollectionsForMedia.get( new Integer( media ) );
		
		if ( c == null ) {
			throw new IllegblArgumentException( "Invalid media type: " + media );
		}
		return c.intVblue();
	}
	
	/**
	 * 
	 * @pbram media
	 * @return
	 * @ throws IllegblArgumentException
	 *          If medib is not valid
	 */
	stbtic int defaultTypesForMedia( int media ) {
		Integer c = (Integer) _defbultTypesForMedia.get( new Integer( media ));
		
		if (c == null ) {
			throw new IllegblArgumentException( "Invalid media type: " + media );
		}
		return c.intVblue();
	}

	
	// first chbracter can only be alphanumberic
	privbte static final Pattern BAD_BEGINNING_CHARS =
		Pbttern.compile( "^[^\\p{Alnum}]+" );
	
	// only bllow alphanumerics and . - _
	privbte static final Pattern BAD_CHARS = 
		Pbttern.compile( "[^\\p{Alnum}\\.\\-_]" );
	privbte static final String REPLACE_STR = "_";
	
	public stbtic String normalizeName( String name ) {
		finbl int MIN_LENGTH = 5;
		finbl int MAX_LENGTH = 100;

		
		if ( nbme == null )
			return null;
		
		// chop off bll bad beginning characters
		nbme = BAD_BEGINNING_CHARS.matcher( name ).replaceFirst("");
		
		nbme = BAD_CHARS.matcher( name ).replaceAll(REPLACE_STR);
		
		finbl StringBuffer nameBuf = new StringBuffer( name );
		
		while ( nbmeBuf.length() < MIN_LENGTH ) {
			nbmeBuf.append( REPLACE_STR );
		}
		 
		
		if ( nbmeBuf.length() > MAX_LENGTH ) {
			nbmeBuf.setLength( MAX_LENGTH );
		}

		return nbmeBuf.toString(); 
	}

	/* fbctory methods */
	
	public stbtic Contribution createContribution( String username, String password, 
			String title, String description, int medib)
	throws DescriptionTooShortException {
		return new AdvbncedContribution( username, password, title, description, media );
	}

	public stbtic Contribution createContribution( String username, String password, 
			String title, String description, int medib, int collection, int type )
	throws DescriptionTooShortException {
		return new AdvbncedContribution( username, password, title, description, media, collection, type );
	}
	
	/**
	 * checks if the given description would be vblid 
	 * @pbram description
	 * @return
	 */
	public stbtic void checkDescription( String description )
		throws DescriptionTooShortException
	{
		AdvbncedContribution.checkDescription( description );
	}
	
}
