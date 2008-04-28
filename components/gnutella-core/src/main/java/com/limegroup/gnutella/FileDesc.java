package com.limegroup.gnutella;

import static com.limegroup.gnutella.Constants.MAX_FILE_SIZE;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.util.I18NConvert;
import org.limewire.util.RPNParser.StringLookup;

import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.routing.HashFunction;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.xml.LimeXMLDocument;


/**
 * This class contains data for an individual shared file.  It also provides
 * various utility methods for checking against the encapsulated data.<p>
 */

public class FileDesc implements StringLookup {

	/**
	 * Constant for the index of this <tt>FileDesc</tt> instance in the 
	 * shared file data structure.
	 */
    private final int _index;

	/**
	 * The absolute path for the file.
	 */
    private final String _path;

	/**
	 * The name of the file, as returned by File.getName().
	 */
    private final String _name;

	/**
	 * The size of the file.
	 */
    private final long _size;

	/**
	 * The modification time of the file, which can be updated.
	 */
    private long _modTime;

	/**
	 * Constant <tt>Set</tt> of <tt>URN</tt> instances for the file.  This
	 * is immutable.
	 */
    private volatile Set<URN> URNS; 

	/**
	 * Constant for the <tt>File</tt> instance.
	 */
	private final File FILE;

	/**
	 * The constant SHA1 <tt>URN</tt> instance.
	 */
	private final URN SHA1_URN;
	
	/**
	 * The License, if one exists, for this FileDesc.
	 */
	private License _license;
	
	/**
	 * The LimeXMLDocs associated with this FileDesc.
	 */
	private final List<LimeXMLDocument> _limeXMLDocs = new CopyOnWriteArrayList<LimeXMLDocument>();

	/**
	 * The number of hits this file has recieved.
	 */
	private int _hits;	
	
	/** 
	 * The number of times this file has had attempted uploads
	 */
	private int _attemptedUploads;
	
    /**
     * The time when the last attempt was made to upload this file
     */
    private long lastAttemptedUploadTime = System.currentTimeMillis();
    
	/** 
	 * The number of times this file has had completed uploads
	 */
	private int _completedUploads;
	    
    /** A simple constructor, for easier testing. */
    protected FileDesc() {
        SHA1_URN = null;
        _size = -1;
        _name = null;
        FILE = null;
        _index = -1;
        URNS = null;
        _path = null;
    }

    /**
	 * Constructs a new <tt>FileDesc</tt> instance from the specified 
	 * <tt>File</tt> class and the associated urns.
	 *
	 * @param file the <tt>File</tt> instance to use for constructing the
	 *  <tt>FileDesc</tt>
     * @param urns the URNs to associate with this FileDesc
     * @param index the index in the FileManager
     */
    public FileDesc(File file, Set<? extends URN> urns, int index) {	
		if((file == null))
			throw new NullPointerException("cannot create a FileDesc with a null File");
		if(index < 0)
			throw new IndexOutOfBoundsException("negative index (" + index + ") not permitted in FileDesc");
		if(urns == null)
			throw new NullPointerException("cannot create a FileDesc with a null URN Set");

		FILE = file;
        _index = index;
        _name = I18NConvert.instance().compose(FILE.getName());
        _path = FILE.getAbsolutePath();
        _size = FILE.length();
        assert _size >= 0 && _size <= MAX_FILE_SIZE : "invalid size "+_size+" of file "+FILE;
        _modTime = FILE.lastModified();
        URNS = Collections.unmodifiableSet(urns);
		SHA1_URN = extractSHA1();
		if(SHA1_URN == null)
			throw new IllegalArgumentException("no SHA1 URN");

        _hits = 0; // Starts off with 0 hits
    }

	/**
	 * Returns whether or not this <tt>FileDesc</tt> has any urns.
	 *
	 * @return <tt>true</tt> if this <tt>FileDesc</tt> has urns,
	 *  <tt>false</tt> otherwise
	 */
	public boolean hasUrns() {
		return !URNS.isEmpty();
	}

	/**
	 * Returns the index of this file in our file data structure.
	 *
	 * @return the index of this file in our file data structure
	 */
	public int getIndex() {
		return _index;
	}

	/**
	 * Returns the size of the file on disk, in bytes.
	 *
	 * @return the size of the file on disk, in bytes
	 */
	public long getFileSize() {
		return _size;
	}

	/**
	 * Returns the name of this file.
	 * 
	 * @return the name of this file
	 */
	public String getFileName() {
		return _name;
	}

	/**
	 * Returns the last modification time for the file according to this
	 * <tt>FileDesc</tt> instance.
	 *
	 * @return the modification time for the file
	 */
	public long lastModified() {
		return _modTime;
	}

	/**
	 * Extracts the SHA1 URN from the set of urns.
	 */
	private URN extractSHA1() {
        for(URN urn : URNS) {
            if(urn.isSHA1())
                return urn;
        }

		// this should never happen!!
        return null;
    }

	/**
	 * @return the TTROOT URN from the set of urns.
	 */
	public URN getTTROOTUrn() {
	    for(URN urn : URNS) {
	        if(urn.isTTRoot())
	            return urn;
	    }
	    
	    // this can happen.
	    return null;
	}
	
	/**
	 * Returns the <tt>File</tt> instance for this <tt>FileDesc</tt>.
	 *
	 * @return the <tt>File</tt> instance for this <tt>FileDesc</tt>
	 */
	public File getFile() {
	    return FILE;
	}
    
    public URN getSHA1Urn() {
        return SHA1_URN;
    }

    /**
     * updates this FD as carrying a ttroot.
     * @return true if we didn't already know about that root
     */
    public boolean setTTRoot(URN ttroot) {
        boolean ret = !getUrns().contains(ttroot);
        UrnSet s = new UrnSet();
        s.add(SHA1_URN);
        s.add(ttroot);
        URNS = Collections.unmodifiableSet(s);
        return ret;
    }
    
	/**
	 * Returns a new <tt>Set</tt> instance containing the <tt>URN</tt>s
	 * for the this <tt>FileDesc</tt>.  The <tt>Set</tt> instance
	 * returned is immutable.
	 *
	 * @return a new <tt>Set</tt> of <tt>URN</tt>s for this 
	 *  <tt>FileDesc</tt>
	 */
	public Set<URN> getUrns() {
		return URNS;
	}   

	/**
	 * Returns the absolute path of the file represented wrapped by this
	 * <tt>FileDesc</tt>.
	 *
	 * @return the absolute path of the file
	 */
	public String getPath() {
		return FILE.getAbsolutePath();
	}
	
	/**
	 * Adds a LimeXMLDocument to this FileDesc.
	 */
	public void addLimeXMLDocument(LimeXMLDocument doc) {
        
        _limeXMLDocs.add(doc);
        
	    doc.initIdentifier(FILE);
	    if(doc.isLicenseAvailable())
	        _license = doc.getLicense();
    }
    
    /**
     * Replaces one LimeXMLDocument with another.
     */
    public boolean replaceLimeXMLDocument(LimeXMLDocument oldDoc, 
                                          LimeXMLDocument newDoc) {
        synchronized(_limeXMLDocs) {
            int index = _limeXMLDocs.indexOf(oldDoc);
            if( index == -1 )
                return false;
            
            _limeXMLDocs.set(index, newDoc);
        }
        
        newDoc.initIdentifier(FILE);
        if(newDoc.isLicenseAvailable())
            _license = newDoc.getLicense();
        else if(_license != null && oldDoc.isLicenseAvailable())
            _license = null;        
        return true;
    }
    
    /**
     * Removes a LimeXMLDocument from the FileDesc.
     */
    public boolean removeLimeXMLDocument(LimeXMLDocument toRemove) {
        
        if (!_limeXMLDocs.remove(toRemove))
            return false;
        
        if(_license != null && toRemove.isLicenseAvailable())
            _license = null;
        
        return true;
    }   
    
    /**
     * Returns the LimeXMLDocuments for this FileDesc.
     */
    public List<LimeXMLDocument> getLimeXMLDocuments() {
        return _limeXMLDocs;
    }
    
    /**
     * Returns the first LimeXMLDocument or null if the 
     * document List is empty.
     */
    public LimeXMLDocument getXMLDocument() {
        List<LimeXMLDocument> docs = getLimeXMLDocuments();
        return docs.isEmpty() ? null : docs.get(0);
    }
    
    /**
     * Returns a LimeXMLDocument whose schema URI is equal to
     * the passed schema URI or null if no such LimeXMLDocument
     * exists.
     */
    public LimeXMLDocument getXMLDocument(String schemaURI) {
        for(LimeXMLDocument doc : getLimeXMLDocuments()) {
            if (doc.getSchemaURI().equalsIgnoreCase(schemaURI))
                return doc;
        }
        return null;
    }
    
    /**
     * Determines if a license exists on this FileDesc.
     */
    public boolean isLicensed() {
        return _license != null;
    }
    
    /**
     * Returns the license associated with this FileDesc.
     */
    public License getLicense() {
        return _license;
    }
	
    /**
     * Determine whether or not the given <tt>URN</tt> instance is 
	 * contained in this <tt>FileDesc</tt>.
	 *
	 * @param urn the <tt>URN</tt> instance to check for
	 * @return <tt>true</tt> if the <tt>URN</tt> is a valid <tt>URN</tt>
	 *  for this file, <tt>false</tt> otherwise
     */
    public boolean containsUrn(URN urn) {
        return URNS.contains(urn);
    }
    
    /**
     * Increase & return the new hit count.
     * @return the new hit count
     */    
    public int incrementHitCount() {
        return ++_hits;
    }
    
    /** 
     * @return the current hit count 
     */
    public int getHitCount() {
        return _hits;
    }
    
    /**
     * Increase & return the new attempted uploads
     * @return the new attempted upload count
     */    
    public synchronized int incrementAttemptedUploads() {
        lastAttemptedUploadTime = System.currentTimeMillis();
        return ++_attemptedUploads;
    }
    
    /** 
     * @return the current attempted uploads
     */
    public synchronized int getAttemptedUploads() {
        return _attemptedUploads;
    }
    
    /**
     * Returns the time when the last upload attempt was made
     */
    public synchronized long getLastAttemptedUploadTime() {
        return lastAttemptedUploadTime;
    }
    
    /**
     * Increase & return the new completed uploads
     * @return the new completed upload count
     */    
    public int incrementCompletedUploads() {
        return ++_completedUploads;
    }
    
    /** 
     * @return the current completed uploads
     */
    public int getCompletedUploads() {
        return _completedUploads;
    }       
    
	// overrides Object.toString to provide a more useful description
	@Override
    public String toString() {
		return ("FileDesc:\r\n"+
				"name:     "+_name+"\r\n"+
				"index:    "+_index+"\r\n"+
				"path:     "+_path+"\r\n"+
				"size:     "+_size+"\r\n"+
				"modTime:  "+_modTime+"\r\n"+
				"File:     "+FILE+"\r\n"+
				"urns:     "+URNS+"\r\n"+
				"docs:     "+ _limeXMLDocs+"\r\n");
	}
    
    /**
     * some factors to consider when deciding if a file fits certain criteria
     * like being rare.
     */
    public String lookup(String key) {
        if (key == null)
            return null;
        if ("hits".equals(key))
            return String.valueOf(getHitCount());
        else if ("ups".equals(key))
            return String.valueOf(getAttemptedUploads());
        else if ("cups".equals(key))
            return String.valueOf(getCompletedUploads());
        else if ("lastup".equals(key))
            return String.valueOf(System.currentTimeMillis() - getLastAttemptedUploadTime());
        else if ("licensed".equals(key))
            return String.valueOf(isLicensed());
        else if ("atUpSet".equals(key))
            return DHTSettings.RARE_FILE_ATTEMPTED_UPLOADS.getValueAsString();
        else if ("cUpSet".equals(key))
            return DHTSettings.RARE_FILE_COMPLETED_UPLOADS.getValueAsString();
        else if ("rftSet".equals(key))
            return DHTSettings.RARE_FILE_TIME.getValueAsString();
        else if ("hasXML".equals(key))
            return String.valueOf(getXMLDocument() != null);
        else if ("size".equals(key))
            return String.valueOf(_size);
        else if ("lastM".equals(key))
            return String.valueOf(lastModified());
        else if ("numKW".equals(key))
            return String.valueOf(HashFunction.keywords(getPath()).length);
        else if ("numKWP".equals(key))
            return String.valueOf(HashFunction.getPrefixes(HashFunction.keywords(getPath())).length);
        else if (key.startsWith("xml_") && getXMLDocument() != null) {
            key = key.substring(4,key.length());
            return getXMLDocument().lookup(key);
            
        // Note: Removed 'firewalled' check -- might not be necessary, but
        // should see if other ways to re-add can be done.
        }
        return null;
    }
    
}


