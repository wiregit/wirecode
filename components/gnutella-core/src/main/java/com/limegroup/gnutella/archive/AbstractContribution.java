package com.limegroup.gnutella.archive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.util.CoWList;

/**
 * 
 * A contribution consists of one or more files that we upload to a location
 * such as the Internet Archive.
 * 
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
 */
public abstract class AbstractContribution {

	public static final String REPOSITORY_VERSION = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/AbstractContribution.java,v 1.1.2.14 2005-11-07 17:43:26 zlatinb Exp $";
	
	private String _title;
	private int _media;
	private int _collection;

	private String _username, _password;
	
	private final LinkedHashMap _files = new LinkedHashMap();
    
	
	/** String -> String */
	private HashMap _fields = new HashMap();

	private volatile boolean _cancelled;
    
    
    public static final int NOT_CONNECTED = 0;
    public static final int CONNECTED = 1;  
    public static final int FILE_STARTED = 2;
    public static final int FILE_PROGRESSED = 3;
    public static final int FILE_COMPLETED = 4;
    public static final int CHECKIN_STARTED = 5;
    public static final int CHECKIN_COMPLETED = 6;


    /** LOCKING: this */
    private String _curFileName;
    private int _filesSent = 0;
    private long _totalBytesSent;
    
    protected long _totalUploadSize;
    protected final Map _fileNames2Progress = new HashMap();

    
    private int _id = NOT_CONNECTED;
    
    private final List _uploadListeners = new CoWList(CoWList.ARRAY_LIST);
	
    /**
     * @return the verification URL that should be used for the contribution
     */
	abstract public String getVerificationUrl();
	
	/**
     * @return normalized identifier
	 */
	abstract public String reserveIdentifier( String identifier ) throws 
        IdentifierUnavailableException, IOException;

	abstract public void upload() throws IOException;
	
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
    
    protected class UploadFileProgress {
        
        private long _fileSize;
        private long _bytesSent = 0;
        
        public UploadFileProgress( long fileSize ) {
            _fileSize = fileSize;
        }
        
        public long getFileSize() {
            return _fileSize;
        }
        
        public long getBytesSent() {
            return _bytesSent;
        }
        
        public void setBytesSent( long bytesSent ) {
            _bytesSent = bytesSent;
        }
        public void incrBytesSent( long bytesSentDelta ) {
            _bytesSent += bytesSentDelta;
        }
    }

    public void addListener( UploadListener l ) {
        _uploadListeners.add( l );
    }
    
    public void removeListener( UploadListener l ) {
        _uploadListeners.remove( l );
    }
    
    private void notifyStateChange() {
        for (Iterator i = _uploadListeners.iterator(); i.hasNext();) {
            UploadListener l = (UploadListener) i.next();
            
            switch ( _id ) {
            case FILE_STARTED:
                l.fileStarted();
                break;
            case FILE_PROGRESSED:
                l.fileProgressed();
                break;
            case FILE_COMPLETED:
                l.fileCompleted();
                break;
            case CONNECTED:
                l.connected();
                break;
            case CHECKIN_STARTED:
                l.checkinStarted();
                break;
            case CHECKIN_COMPLETED:
                l.checkinCompleted();
                break;
            default:    
                break;
            }
            
        }
    }
    
    public synchronized int getFilesSent() {
        return _filesSent;
    }

    public synchronized int getTotalFiles() {
        return getFileDescs().size();
    }

    
    public synchronized long getFileBytesSent() {
        return ((UploadFileProgress) _fileNames2Progress.get( _curFileName )).getBytesSent();       

    }
    
    public synchronized long getFileSize() {
        return ((UploadFileProgress) _fileNames2Progress.get( _curFileName )).getFileSize();
    }
    
    
    public synchronized long getTotalBytesSent() {
        return _totalBytesSent;
    }
    
    public synchronized long getTotalSize() {
        return _totalUploadSize;
    }

    
    public synchronized String getFileName() {
        return _curFileName;
    }

    
    public int getID() {
        return _id;
    }
    
    
    void connected() {
        _id = CONNECTED;
    }
    
    
    void fileStarted( String fileName, long bytesSent ) {
        _id = FILE_STARTED;
        synchronized(this) {
            _curFileName = fileName;
            ((UploadFileProgress) _fileNames2Progress.get( fileName )).setBytesSent( bytesSent );
        }
        notifyStateChange();
    }
    
    void fileStarted( String fileName ) {
        fileStarted( fileName, 0 );
    }
    
    /**
     * 
     * @param fileName
     * @param bytesSent
     * 
     * @throws IllegalStateException
     *         If fileName does not match the current fileName
     */
    void fileProgressed( long bytesSent ) {
        _id = FILE_PROGRESSED;
        
        synchronized(this) {
            UploadFileProgress progress = (UploadFileProgress) _fileNames2Progress.get( _curFileName );
            // find delta       
            long delta = bytesSent - progress.getBytesSent();
            _totalBytesSent += delta;
            progress.setBytesSent( bytesSent );
        }
        notifyStateChange();
    }
    
    /**
     * 
     * @param fileName
     * @param bytesSentDelta
     * 
     * @throws IllegalStateException
     *         If fileName does not match the current fileName
     */
    void fileProgressedDelta( long bytesSentDelta ) {
        _id = FILE_PROGRESSED;
        synchronized(this) {
            _totalBytesSent += bytesSentDelta;
            ((UploadFileProgress) _fileNames2Progress.get( _curFileName )).incrBytesSent( bytesSentDelta );
        }
        notifyStateChange();
    }
    
    /**
     * 
     * @param fileName
     * 
     * @throws IllegalStateException
     *         If fileName does not match the current fileName
     */
    void fileCompleted() {
        _id = FILE_COMPLETED;
        
        synchronized(this) {
            UploadFileProgress progress = (UploadFileProgress) _fileNames2Progress.get( _curFileName );
            progress.setBytesSent( progress.getFileSize() );
            _filesSent++;
        }
        notifyStateChange();
    }
    
    void checkinStarted() {
        _id = CHECKIN_STARTED;
        notifyStateChange();
    }
    
    void checkinCompleted() {
        _id = CHECKIN_COMPLETED;
        notifyStateChange();
    }
	
}
