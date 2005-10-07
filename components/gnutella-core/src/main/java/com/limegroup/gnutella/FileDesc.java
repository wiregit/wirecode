package com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.TigerTreeCache;
import com.limegroup.gnutella.util.CoWList;
import com.limegroup.gnutella.util.I18NConvert;
import com.limegroup.gnutella.xml.LimeXMLDocument;


/**
 * This class contains data for an individual shared file.  It also provides
 * various utility methods for checking against the encapsulated data.<p>
 */

public class FileDesc implements FileDetails {
    
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
    private final Set /* of URNS */ URNS; 

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
	private final List /* of LimeXMLDocument */ _limeXMLDocs = new CoWList(CoWList.ARRAY_LIST);

	/**
	 * The number of hits this file has recieved.
	 */
	private int _hits;	
	
	/** 
	 * The number of times this file has had attempted uploads
	 */
	private int _attemptedUploads;
	
	/** 
	 * The number of times this file has had completed uploads
	 */
	private int _completedUploads;

    /**
	 * Constructs a new <tt>FileDesc</tt> instance from the specified 
	 * <tt>File</tt> class and the associated urns.
	 *
	 * @param file the <tt>File</tt> instance to use for constructing the
	 *  <tt>FileDesc</tt>
     * @param urns the URNs to associate with this FileDesc
     * @param index the index in the FileManager
     */
    public FileDesc(File file, Set urns, int index) {	
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
	    for(Iterator iter = URNS.iterator(); iter.hasNext(); ) {
            URN urn = (URN)iter.next();
            if(urn.isSHA1())
                return urn;
        }

		// this should never happen!!
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
	 * Returns a new <tt>Set</tt> instance containing the <tt>URN</tt>s
	 * for the this <tt>FileDesc</tt>.  The <tt>Set</tt> instance
	 * returned is immutable.
	 *
	 * @return a new <tt>Set</tt> of <tt>URN</tt>s for this 
	 *  <tt>FileDesc</tt>
	 */
	public Set getUrns() {
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
        
	    doc.setIdentifier(FILE);
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
        
        newDoc.setIdentifier(FILE);
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
    public List getLimeXMLDocuments() {
        return _limeXMLDocs;
    }
	
	public LimeXMLDocument getXMLDocument() {
        List docs = getLimeXMLDocuments();
		return docs.isEmpty() ? null 
			: (LimeXMLDocument)docs.get(0);
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
     * Returns TIGER_TREE
     * @return the <tt>TigerTree</tt> this class holds
     */
    public HashTree getHashTree() {
        return TigerTreeCache.instance().getHashTree(this);
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
    public int incrementAttemptedUploads() {
        return ++_attemptedUploads;
    }
    
    /** 
     * @return the current attempted uploads
     */
    public int getAttemptedUploads() {
        return _attemptedUploads;
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
    
    /**
     * Opens an input stream to the <tt>File</tt> instance for this
	 * <tt>FileDesc</tt>.
	 *
	 * @return an <tt>InputStream</tt> to the <tt>File</tt> instance
	 * @throws <tt>FileNotFoundException</tt> if the file represented
	 *  by the <tt>File</tt> instance could not be found
     */
    public InputStream createInputStream() throws FileNotFoundException {
		return new BufferedInputStream(new FileInputStream(FILE));
    }
    
    /**
     * Utility method for toString that converts the specified
     * <tt>Iterator</tt>'s items to a string.
     *
     * @param i the <tt>Iterator</tt> to convert
     * @return the contents of the set as a comma-delimited string
     */
    private String listInformation(Iterator i) {
        StringBuffer stuff = new StringBuffer();
        for(; i.hasNext(); ) {
            stuff.append(i.next().toString());
            if( i.hasNext() )
                stuff.append(", ");
        }
        return stuff.toString();
    }

	// overrides Object.toString to provide a more useful description
	public String toString() {
		return ("FileDesc:\r\n"+
				"name:     "+_name+"\r\n"+
				"index:    "+_index+"\r\n"+
				"path:     "+_path+"\r\n"+
				"size:     "+_size+"\r\n"+
				"modTime:  "+_modTime+"\r\n"+
				"File:     "+FILE+"\r\n"+
				"urns:     "+URNS+"\r\n"+
				"docs:     "+ _limeXMLDocs);
	}
	
	public InetSocketAddress getSocketAddress() {
		// TODO maybe cache this, even statically
		try {
			return new InetSocketAddress(InetAddress.getByAddress
										 (RouterService.getAcceptor().getAddress(true)), 
										 RouterService.getAcceptor().getPort(true));
		} catch (UnknownHostException e) {
		}
		return null;
	}
	
	public boolean isFirewalled() {
		return !RouterService.acceptedIncomingConnection();
	}
}


