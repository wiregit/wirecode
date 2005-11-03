package com.limegroup.gnutella.archive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.limegroup.gnutella.FileDesc;

/**
 * Follow these steps to do upload a contribution to the 
 * Internet Archive:
 * 
 * 	1.	create a Contribution object using your preferred concrete subclass
 * 		of AbstractContribution
 * 	2.	call requestIdentifier() with your requested identifier
 * 	3.	if step 2 successful, call getVerificationUrl() to get the verification URL
 * 	4.	call addFile() for each file you want to add to the contribution
 * 	5.	call addListener() with your UploadListener
 * 	6.  call upload() to upload the contribution
 * 
 * @author tim
 *
 */
public abstract class AbstractContribution {

	public static final String REPOSITORY_VERSION = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/AbstractContribution.java,v 1.1.2.12 2005-11-03 22:35:28 tolsen Exp $";
	
	private String _title;
	private int _media;
	private int _collection;

	private String _username;
	
	private final LinkedHashMap _files = new LinkedHashMap();
	private final ArrayList _uploadListeners = new ArrayList();
	
	// if by chance this class becomes serializable, and
	// you wish to write the password to disk, then feel free
	// to take out the transient keyword
	private transient String _password;
	
	/** String -> String */
	private HashMap _fields = new HashMap();

	private volatile boolean _cancelled = false;
	
	abstract public String getVerificationUrl();
	
	
	// returns normalized (no funny characters) identifier
	abstract public String reserveIdentifier( String identifier )
	throws IdentifierUnavailableException, IOException;
	
	abstract public void upload() throws IOException;
	
	// a contribution consists of one or more files
	// note that we currently set the licenseurl for the
	// contribution to th
	
	public void addFileDesc( FileDesc fd ) { 
		_files.put( fd, new File(fd));
	}
	
	public void removeFileDesc( FileDesc fd ) { 
		_files.remove( fd );
	}
	
	public boolean containsFileDesc( FileDesc fd ) { 
		return _files.containsKey( fd ); 
	}	
	
	public void cancel() {
		_cancelled = true;
	}
	
	boolean isCancelled() {
		return _cancelled;
	}
	
	/**
	 * @return a set of the files in the collection
	 * 
	 * I'm guessing that LinkedHashMap returns a LinkedHashSet for keySet() 
	 * so the order should be in the order they were added
	 *         
	 */
	public Set getFileDescs() { 
		return Collections.unmodifiableSet( _files.keySet() ); 
	}
	
	protected Collection getFiles() {
		return Collections.unmodifiableCollection(_files.values());
	}
	
	public void addListener( UploadListener l ) {
		_uploadListeners.add( l );
	}
	
	public void removeListener( UploadListener l ) {
		_uploadListeners.remove( l );
	}
	
	void processUploadEvent( UploadEvent e ) {
		for (Iterator i = _uploadListeners.iterator(); i.hasNext();) {
			UploadListener l = (UploadListener) i.next();
			
			switch ( e.getID() ) {
			case UploadEvent.FILE_STARTED:
				l.fileStarted( e );
				break;
			case UploadEvent.FILE_PROGRESSED:
				l.fileProgressed( e );
				break;
			case UploadEvent.FILE_COMPLETED:
				l.fileCompleted( e );
				break;
			case UploadEvent.CONNECTED:
				l.connected( e );
				break;
			case UploadEvent.CHECKIN_STARTED:
				l.checkinStarted( e );
				break;
			case UploadEvent.CHECKIN_COMPLETED:
				l.checkinCompleted( e );
				break;
			default:	
				break;
			}
			
		}
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
		if ( Archives.getMediaString( media ) == null ) {
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
		if ( Archives.getCollectionString( collection ) == null ) {
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
	 *
	 * Also see the Dublin Core: http://dublincore.org/documents/dces/
	 * 
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
	
	protected Map getFields() {
		return Collections.unmodifiableMap( _fields );
	}

	
}
