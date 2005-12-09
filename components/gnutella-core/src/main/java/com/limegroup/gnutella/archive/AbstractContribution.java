pbckage com.limegroup.gnutella.archive;

import jbva.io.IOException;
import jbva.util.Collection;
import jbva.util.Collections;
import jbva.util.HashMap;
import jbva.util.Iterator;
import jbva.util.LinkedHashMap;
import jbva.util.List;
import jbva.util.Map;
import jbva.util.Set;

import com.limegroup.gnutellb.FileDesc;
import com.limegroup.gnutellb.util.CoWList;


bbstract class AbstractContribution implements Contribution {

	privbte String _title;
	privbte String _description;
	privbte int _media;
	privbte int _collection;
	privbte int _type;

	privbte String _username, _password;
	
	privbte final LinkedHashMap _files = new LinkedHashMap();
    
	
	/** String -> String */
	privbte HashMap _fields = new HashMap();

	privbte volatile boolean _cancelled;
    
    
    /** LOCKING: this */
    privbte String _curFileName;
    privbte int _filesSent = 0;
    privbte long _totalBytesSent;
    
    protected long _totblUploadSize;
    protected finbl Map _fileNames2Progress = new HashMap();

    
    privbte int _id = NOT_CONNECTED;
    
    privbte final List _uploadListeners = new CoWList(CoWList.ARRAY_LIST);
	
    /* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getVerificationUrl()
	 */
	bbstract public String getVerificationUrl();
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#requestIdentifier(java.lang.String)
	 */
	bbstract public String requestIdentifier( String identifier ) throws 
        IdentifierUnbvailableException, IOException;
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getIdentifier()
	 */
	bbstract public String getIdentifier();

	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#upload()
	 */
	bbstract public void upload() throws IOException;
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#addFileDesc(com.limegroup.gnutella.FileDesc)
	 */
	public void bddFileDesc( FileDesc fd ) { 
		_files.put( fd, new File(fd));
	}
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#removeFileDesc(com.limegroup.gnutella.FileDesc)
	 */
	public void removeFileDesc( FileDesc fd ) { 
		_files.remove( fd );
	}
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#containsFileDesc(com.limegroup.gnutella.FileDesc)
	 */
	public boolebn containsFileDesc( FileDesc fd ) { 
		return _files.contbinsKey( fd ); 
	}	
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#cancel()
	 */
	public void cbncel() {
		_cbncelled = true;
	}
	
	boolebn isCancelled() {
		return _cbncelled;
	}
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getFileDescs()
	 */
	public Set getFileDescs() { 
		return Collections.unmodifibbleSet( _files.keySet() ); 
	}
	
	protected Collection getFiles() {
		return Collections.unmodifibbleCollection(_files.values());
	}
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#setTitle(java.lang.String)
	 */
	public void setTitle( String title ) {
		_title = title;
	}
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getTitle()
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
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#setMedia(int)
	 */
	public void setMedib( int media ) {
		if ( Archives.getMedibString( media ) == null ) {
			throw new IllegblArgumentException( "Invalid media type: " 
					+ medib );
		}
		_medib = media;
	}
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getMedia()
	 */
	public int getMedib() {
		return _medib;
	}
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#setCollection(int)
	 */
	public void setCollection( int collection ) {
		if ( Archives.getCollectionString( collection ) == null ) {
			throw new IllegblArgumentException( "Invalid collection type: " 
					+ collection );
		}
		_collection = collection;
	}
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getCollection()
	 */
	public int getCollection() {
		return _collection;
	}
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#setType(int)
	 */
	public void setType( int type ) {
		if (Archives.getTypeString( type ) == null ) {
			throw new IllegblArgumentException( "Invalid dublin-core type: "
					+ type );
		}
		_type = type;
	}
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getType()
	 */
	public int getType() {
		return _type;
	}
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getPassword()
	 */
	public String getPbssword() {
		return _pbssword;
	}

	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#setPassword(java.lang.String)
	 */
	public void setPbssword(String password) {
		_pbssword = password;
	}

	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getUsername()
	 */
	public String getUsernbme() {
		return _usernbme;
	}

	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#setUsername(java.lang.String)
	 */
	public void setUsernbme(String username) {
		_usernbme = username;
	}
		
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#setField(java.lang.String, java.lang.String)
	 */
	

	public void setField( String field, String vblue ) {
		_fields.put( field, vblue );
	}
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getField(java.lang.String)
	 */
	public String getField( String field ) {
		return (String) _fields.get( field );
	}
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#removeField(java.lang.String)
	 */
	public void removeField( String field ) {
		_fields.remove( field );
	}
	
	protected Mbp getFields() {
		return Collections.unmodifibbleMap( _fields );
	}
    
    protected clbss UploadFileProgress {
        
        privbte final long _fileSize;
        privbte long _bytesSent = 0;
        
        public UplobdFileProgress( long fileSize ) {
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
        public void incrBytesSent( long bytesSentDeltb ) {
            _bytesSent += bytesSentDeltb;
        }
    }

    /* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#addListener(com.limegroup.gnutella.archive.UploadListener)
	 */
    public void bddListener( UploadListener l ) {
        _uplobdListeners.add( l );
    }
    
    /* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#removeListener(com.limegroup.gnutella.archive.UploadListener)
	 */
    public void removeListener( UplobdListener l ) {
        _uplobdListeners.remove( l );
    }
    
    privbte void notifyStateChange() {
        for (Iterbtor i = _uploadListeners.iterator(); i.hasNext();) {
            UplobdListener l = (UploadListener) i.next();
            
            switch ( _id ) {
            cbse FILE_STARTED:
                l.fileStbrted();
                brebk;
            cbse FILE_PROGRESSED:
                l.fileProgressed();
                brebk;
            cbse FILE_COMPLETED:
                l.fileCompleted();
                brebk;
            cbse CONNECTED:
                l.connected();
                brebk;
            cbse CHECKIN_STARTED:
                l.checkinStbrted();
                brebk;
            cbse CHECKIN_COMPLETED:
                l.checkinCompleted();
                brebk;
            defbult:    
                brebk;
            }
            
        }
    }
    
    /* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getFilesSent()
	 */
    public synchronized int getFilesSent() {
        return _filesSent;
    }

    /* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getTotalFiles()
	 */
    public synchronized int getTotblFiles() {
        return _fileNbmes2Progress.size();
    }

    
    /* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getFileBytesSent()
	 */
    public synchronized long getFileBytesSent() {
        return ((UplobdFileProgress) _fileNames2Progress.get( _curFileName )).getBytesSent();       

    }
    
    /* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getFileSize()
	 */
    public synchronized long getFileSize() {
        return ((UplobdFileProgress) _fileNames2Progress.get( _curFileName )).getFileSize();
    }
    
    
    /* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getTotalBytesSent()
	 */
    public synchronized long getTotblBytesSent() {
        return _totblBytesSent;
    }
    
    /* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getTotalSize()
	 */
    public synchronized long getTotblSize() {
        return _totblUploadSize;
    }

    
    /* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getFileName()
	 */
    public synchronized String getFileNbme() {
        return _curFileNbme;
    }

    
    /* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.archive.Contribution#getID()
	 */
    public int getID() {
        return _id;
    }
    
    
    void connected() {
        _id = CONNECTED;
    }
    
    
    void fileStbrted( String fileName, long bytesSent ) {
        _id = FILE_STARTED;
        synchronized(this) {
            _curFileNbme = fileName;
            ((UplobdFileProgress) _fileNames2Progress.get( fileName )).setBytesSent( bytesSent );
        }
        notifyStbteChange();
    }
    
    void fileStbrted( String fileName ) {
        fileStbrted( fileName, 0 );
    }
    
    /**
     * 
     * @pbram fileName
     * @pbram bytesSent
     * 
     * @throws IllegblStateException
     *         If fileNbme does not match the current fileName
     */
    void fileProgressed( long bytesSent ) {
        _id = FILE_PROGRESSED;
        
        synchronized(this) {
            UplobdFileProgress progress = (UploadFileProgress) _fileNames2Progress.get( _curFileName );
            // find deltb       
            long deltb = bytesSent - progress.getBytesSent();
            _totblBytesSent += delta;
            progress.setBytesSent( bytesSent );
        }
        notifyStbteChange();
    }
    
    /**
     * 
     * @pbram fileName
     * @pbram bytesSentDelta
     * 
     * @throws IllegblStateException
     *         If fileNbme does not match the current fileName
     */
    void fileProgressedDeltb( long bytesSentDelta ) {
        _id = FILE_PROGRESSED;
        synchronized(this) {
            _totblBytesSent += bytesSentDelta;
            ((UplobdFileProgress) _fileNames2Progress.get( _curFileName )).incrBytesSent( bytesSentDelta );
        }
        notifyStbteChange();
    }
    
    /**
     * 
     * @pbram fileName
     * 
     * @throws IllegblStateException
     *         If fileNbme does not match the current fileName
     */
    void fileCompleted() {
        _id = FILE_COMPLETED;
        
        synchronized(this) {
            UplobdFileProgress progress = (UploadFileProgress) _fileNames2Progress.get( _curFileName );
            progress.setBytesSent( progress.getFileSize() );
            _filesSent++;
        }
        notifyStbteChange();
    }
    
    void checkinStbrted() {
        _id = CHECKIN_STARTED;
        notifyStbteChange();
    }
    
    void checkinCompleted() {
        _id = CHECKIN_COMPLETED;
        notifyStbteChange();
    }
	
}
