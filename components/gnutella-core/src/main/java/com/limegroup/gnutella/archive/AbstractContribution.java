package com.limegroup.gnutella.archive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import com.limegroup.gnutella.FileDesc;

/**
 * Follow these steps to do upload a contribution to the 
 * Internet Archive:
 * 
 * 	1.	create a Contribution object using your preferred concrete subclass
 * 		of AbstractContribution
 * 	2.	call getVerificationUrl() with your requested identifier
 * 	3.	call addFile() for each file you want to add to the contribution
 * 	4.	call addListener() with your UploadListener
 * 	5.  call upload() to upload the contribution
 * 
 * @author tim
 *
 */
public abstract class AbstractContribution {

	public static final String repositoryVersion = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/AbstractContribution.java,v 1.1.2.1 2005-10-26 20:02:48 tolsen Exp $";
	
	private String _title;
	private int _media;
	private int _collection;

	private String _username;
	
	private final LinkedHashSet _fds = new LinkedHashSet();
	private final ArrayList _uploadListeners = new ArrayList();
	
	// if by chance this class becomes serializable, and
	// you wish to write the password to disk, then feel free
	// to take out the transient keyword
	private transient String _password;
	
	/** String -> String */
	private HashMap _fields = new HashMap();

	
	abstract public String getVerificationUrl( String identifier )
	throws IdentifierUnavailableException, IOException;
	
	abstract public void upload();
	
	// a collection consists of one or more files
	
	public void addFile( FileDesc fd ) { _fds.add( fd ); }	
	public void removeFile( FileDesc fd ) { _fds.remove( fd ); }
	public boolean containsFile( FileDesc fd ) { return _fds.contains( fd ); }
	
	/**
	 * @return a set of the files in the collection (implemented as a LinkedHashSet to
	 * keep the elements in the order they were added)
	 *         
	 */
	public Set getFiles() { return Collections.unmodifiableSet( _fds ); }
	
	public void addListener( FTPUploadListener l ) {
		_uploadListeners.add( l );
	}
	
	public void removeListener( FTPUploadListener l ) {
		_uploadListeners.remove( l );
	}
	
	public void setTitle( String title ) {
		_title = title;
	}
	
	public String getTitle() {
		return _title;
	}
	
	/**
	 * 
	 * @param media
	 * 
	 * @throws IllegalArgumentException
	 *         If media is not valid
	 * 
	 */
	public void setMedia( int media ) {
		if ( ArchiveConstants.getMediaString( media ) == null ) {
			throw new IllegalArgumentException( "Invalid media type: " 
					+ media );
		}
		_media = media;
	}
	
	public int getMedia() {
		return _media;
	}
	
	/**
	 * 
	 * @param collection
	 * @throws IllegalArgumentException
	 *         If collection is not valid
	 *         
	 */
	public void setCollection( int collection ) {
		if ( ArchiveConstants.getCollectionString( collection ) == null ) {
			throw new IllegalArgumentException( "Invalid collection type: " 
					+ collection );
		}
		_collection = collection;
	}
	
	public int getCollection() {
		return _collection;
	}
	
	public String getPassword() {
		return _password;
	}

	public void setPassword(String password) {
		_password = password;
	}

	public String getUsername() {
		return _username;
	}

	public void setUsername(String username) {
		_username = username;
	}
		
	
	/**
	 * Fields You can include whatever fields you like, but the following are
	 * known (possibly semantically)  by the Internet Archive
	 * 
	 * Movies and Audio: date, description, runtime
	 * 
	 * Audio: creator, notes, source, taper 	 
	 *  
	 * Movies: color, contact, country, credits, director, producer,
	 *		production_company, segments, segments, sound, sponsor, shotlist 
	 */
	

	public void setField( String field, String value ) {
		_fields.put( field, value );
	}
	
	public String getField( String field ) {
		return (String) _fields.get( field );
	}
	
	public void removeField( String field ) {
		_fields.remove( field );
	}

	
}
