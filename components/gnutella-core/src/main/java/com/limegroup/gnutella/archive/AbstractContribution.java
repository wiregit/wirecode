package com.limegroup.gnutella.archive;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.limegroup.gnutella.FileDesc;


abstract class AbstractContribution implements Contribution {

	private String _title;
	private String _description;
	private int _media;
	private int _collection;
	private int _type;

	private String _username, _password;
	
	private final LinkedHashMap<FileDesc, ArchiveFile> _files = new LinkedHashMap<FileDesc, ArchiveFile>();
    
	
	private HashMap<String, String> _fields = new HashMap<String, String>();

	private volatile boolean _cancelled;
    
    
    /** LOCKING: this */
    private String _curFileName;
    private int _filesSent = 0;
    private long _totalBytesSent;
    
    protected long _totalUploadSize;
    protected final Map<String, UploadFileProgress> _fileNames2Progress = new HashMap<String, UploadFileProgress>();

    
    private ContributionState _id = ContributionState.NOT_CONNECTED;
    
    private final List<UploadListener> _uploadListeners = new CopyOnWriteArrayList<UploadListener>();
	
    /* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getVerificationUrl()
	 */
	abstract public String getVerificationUrl();
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#requestIdentifier(java.lang.String)
	 */
	abstract public String requestIdentifier( String identifier ) throws 
        IdentifierUnavailableException, IOException;
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getIdentifier()
	 */
	abstract public String getIdentifier();

	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#upload()
	 */
	abstract public void upload() throws IOException;
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#addFileDesc(com.limegroup.gnutella.FileDesc)
	 */
	public void addFileDesc( FileDesc fd ) { 
		_files.put( fd, new ArchiveFile(fd));
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#removeFileDesc(com.limegroup.gnutella.FileDesc)
	 */
	public void removeFileDesc( FileDesc fd ) { 
		_files.remove( fd );
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#containsFileDesc(com.limegroup.gnutella.FileDesc)
	 */
	public boolean containsFileDesc( FileDesc fd ) { 
		return _files.containsKey( fd ); 
	}	
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#cancel()
	 */
	public void cancel() {
		_cancelled = true;
	}
	
	boolean isCancelled() {
		return _cancelled;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getFileDescs()
	 */
	public Set<FileDesc> getFileDescs() { 
		return Collections.unmodifiableSet( _files.keySet() ); 
	}
	
	protected Collection<ArchiveFile> getFiles() {
		return Collections.unmodifiableCollection(_files.values());
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#setTitle(java.lang.String)
	 */
	public void setTitle( String title ) {
		_title = title;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getTitle()
	 */
	public String getTitle() {
		return _title;
	}
	
	public void setDescription( String description ) 
	throws DescriptionTooShortException {
		_description = description;
	}
	
	public String getDescription() {
		return _description;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#setMedia(int)
	 */
	public void setMedia( int media ) {
		if ( Archives.getMediaString( media ) == null ) {
			throw new IllegalArgumentException( "Invalid media type: " 
					+ media );
		}
		_media = media;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getMedia()
	 */
	public int getMedia() {
		return _media;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#setCollection(int)
	 */
	public void setCollection( int collection ) {
		if ( Archives.getCollectionString( collection ) == null ) {
			throw new IllegalArgumentException( "Invalid collection type: " 
					+ collection );
		}
		_collection = collection;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getCollection()
	 */
	public int getCollection() {
		return _collection;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#setType(int)
	 */
	public void setType( int type ) {
		if (Archives.getTypeString( type ) == null ) {
			throw new IllegalArgumentException( "Invalid dublin-core type: "
					+ type );
		}
		_type = type;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getType()
	 */
	public int getType() {
		return _type;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getPassword()
	 */
	public String getPassword() {
		return _password;
	}

	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#setPassword(java.lang.String)
	 */
	public void setPassword(String password) {
		_password = password;
	}

	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getUsername()
	 */
	public String getUsername() {
		return _username;
	}

	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#setUsername(java.lang.String)
	 */
	public void setUsername(String username) {
		_username = username;
	}
		
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#setField(java.lang.String, java.lang.String)
	 */
	

	public void setField( String field, String value ) {
		_fields.put( field, value );
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getField(java.lang.String)
	 */
	public String getField( String field ) {
		return _fields.get( field );
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#removeField(java.lang.String)
	 */
	public void removeField( String field ) {
		_fields.remove( field );
	}
	
	protected Map<String, String> getFields() {
		return Collections.unmodifiableMap( _fields );
	}
    
    protected class UploadFileProgress {
        
        private final long _fileSize;
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

    /* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#addListener(com.limegroup.gnutella.archive.UploadListener)
	 */
    public void addListener( UploadListener l ) {
        _uploadListeners.add( l );
    }
    
    /* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#removeListener(com.limegroup.gnutella.archive.UploadListener)
	 */
    public void removeListener( UploadListener l ) {
        _uploadListeners.remove( l );
    }
    
    private void notifyStateChange() {
        for(UploadListener l : _uploadListeners) {
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
    
    /* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getFilesSent()
	 */
    public synchronized int getFilesSent() {
        return _filesSent;
    }

    /* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getTotalFiles()
	 */
    public synchronized int getTotalFiles() {
        return _fileNames2Progress.size();
    }

    
    /* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getFileBytesSent()
	 */
    public synchronized long getFileBytesSent() {
        return  _fileNames2Progress.get( _curFileName ).getBytesSent();       

    }
    
    /* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getFileSize()
	 */
    public synchronized long getFileSize() {
        return _fileNames2Progress.get( _curFileName ).getFileSize();
    }
    
    
    /* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getTotalBytesSent()
	 */
    public synchronized long getTotalBytesSent() {
        return _totalBytesSent;
    }
    
    /* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getTotalSize()
	 */
    public synchronized long getTotalSize() {
        return _totalUploadSize;
    }

    
    /* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getFileName()
	 */
    public synchronized String getFileName() {
        return _curFileName;
    }

    
    /* (non-Javadoc)
	 * @see com.limegroup.gnutella.archive.Contribution#getID()
	 */
    public ContributionState getID() {
        return _id;
    }
    
    
    void connected() {
        _id = ContributionState.CONNECTED;
    }
    
    
    void fileStarted( String fileName, long bytesSent ) {
        _id = ContributionState.FILE_STARTED;
        synchronized(this) {
            _curFileName = fileName;
            _fileNames2Progress.get( fileName ).setBytesSent( bytesSent );
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
        _id = ContributionState.FILE_PROGRESSED;
        
        synchronized(this) {
            UploadFileProgress progress = _fileNames2Progress.get( _curFileName );
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
        _id = ContributionState.FILE_PROGRESSED;
        synchronized(this) {
            _totalBytesSent += bytesSentDelta;
             _fileNames2Progress.get( _curFileName ).incrBytesSent( bytesSentDelta );
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
        _id = ContributionState.FILE_COMPLETED;
        
        synchronized(this) {
            UploadFileProgress progress =  _fileNames2Progress.get( _curFileName );
            progress.setBytesSent( progress.getFileSize() );
            _filesSent++;
        }
        notifyStateChange();
    }
    
    void checkinStarted() {
        _id = ContributionState.CHECKIN_STARTED;
        notifyStateChange();
    }
    
    void checkinCompleted() {
        _id = ContributionState.CHECKIN_COMPLETED;
        notifyStateChange();
    }
	
}
