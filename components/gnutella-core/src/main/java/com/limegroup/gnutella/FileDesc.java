package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;

import com.bitzi.util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.util.Date;
import java.security.*;
import java.util.Enumeration;

/**
 * This class contains data frr an individual shared file.  It also provides
 * various utility methods for checking against the encapsulated data.
 */
public final class FileDesc implements AlternateLocationCollector {
    
	/**
	 * Constant for the index of this <tt>FileDesc</tt> instance in the 
	 * shared file data structure.
	 */
    public final int _index;

	/**
	 * The absolute path for the file.
	 */
    public final String _path;

	/**
	 * The name of the file, as returned by File.getName().
	 */
    public final String _name;

	/**
	 * The size of the file, casted to an <tt>int</tt>.
	 */
    public final int _size;

	/**
	 * The modification time of the file, which can be updated.
	 */
    public long _modTime;

	/**
	 * Constant <tt>Collection</tt> of <tt>URN</tt> instances for the file.
	 */
    private final Set /* of URNS */ URNS; 

	/**
	 * Constant for the <tt>File</tt> instance.
	 */
	private final File FILE;

	/**
	 * The collection of alternate locations for the file.
	 */
	private AlternateLocationCollection _altLocs;
		
    /**
	 * Constructs a new <tt>FileDesc</tt> instance from the specified 
	 * <tt>File</tt> class and the associated urns.
	 *
	 * @param file the <tt>File</tt> instance to use for constructing the
	 *  <tt>FileDesc</tt>
	 * @param urns the <tt>HashSet</tt> of URNs for this <tt>FileDesc</tt>
     */
    public FileDesc(File file, int index, Collection urns) {	
		if((file == null) || (urns == null)) {
			throw new NullPointerException("cannot create a FileDesc with "+
										   "null values");
		}
		if(index <0) {
			throw new IndexOutOfBoundsException("negative values not permitted "+
												"in FileDesc: "+index);
		}
		// make a defensive copy
		FILE = new File(file.getAbsolutePath());
        _index = index;
        _name = FILE.getName();
        _path = FILE.getAbsolutePath(); //TODO: right method?
        _size = (int)FILE.length();
        _modTime = FILE.lastModified();

		// defensively copy the urn HashSet		
		URNS = new HashSet(urns);

        //if(this.shouldCalculateUrns()) {
		//this.calculateUrns();
		//}
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
	public long getSize() {
		return _size;
	}

	/**
	 * Returns the name of this file.
	 * 
	 * @return the name of this file
	 */
	public String getName() {
		return _name;
	}

	/**
	 * Returns the <tt>AlternateLocationCollection</tt> instance for this
	 * <tt>FileDesc</tt>.  The collection could be empty or <tt>null</tt>.
	 *
	 * @return the <tt>AlternateLocationCollection</tt> for this 
	 *  <tt>FileDesc</tt> instance, which can be empty, or <tt>null</tt>
	 *  if it is not initialized
	 */
	public AlternateLocationCollection getAlternateLocationCollection() {
		return _altLocs;
	}

	// implements AlternateLocationCollector interface
	public void addAlternateLocation(AlternateLocation al) {
		createAlternateLocations();
		_altLocs.addAlternateLocation(al);
	}

	// implements AlternateLocationCollector interface
	public void addAlternateLocationCollection(AlternateLocationCollection alc) {
		createAlternateLocations();
		_altLocs.addAlternateLocationCollection(alc);
	}

	// implements AlternateLocationCollector interface
	public boolean hasAlternateLocations() {
		if(_altLocs == null) return false;
		return _altLocs.hasAlternateLocations();
	}

	/**
	 * Constructs the alternate location collection instance if it's null.
	 */
	private void createAlternateLocations() {
		if(_altLocs == null) _altLocs = new AlternateLocationCollection();		
	}
    
    /**
	 * Returns whether or not this <tt>FileDesc</tt> has an associated
	 * SHA1 URN value.
	 * 
	 * @return <tt>true</tt> if this <tt>FileDesc</tt> has an 
	 *  associated SHA1 value, <tt>false</tt> otherwise
	 */
    public synchronized boolean hasSHA1Urn() {
		return (getSHA1Urn() != null);
	}
	
    
    /**
     * Adds any URNs that can be locally calculated; may take a while to 
	 * complete on large files.<p>
	 * 
	 * This is a place where members of <tt>FileDesc</tt> are mutable,
	 * namely the collection of <tt>URN</tt>s.
     */
    public void calculateUrns() {
		// update modTime
		_modTime = FILE.lastModified();
		try {
			URN urn = URNFactory.createSHA1Urn(FILE);
			URNS.add(urn);
		} catch(IOException e) {
			// the urn just does not get added
		}		
	}
    
    /**
     * Determine whether or not the given <tt>URN</tt> instance is 
	 * contained in this <tt>FileDesc</tt>.
	 *
	 * @param urn the <tt>URN</tt> instance to check for
	 * @return <tt>true</tt> if the <tt>URN</tt> is a valid <tt>URN</tt>
	 *  for this file, <tt>false</tt> otherwise
     */
    public synchronized boolean containsUrn(URN urn) {
        // first check if modified since last hashing
        if (FILE.lastModified()!=_modTime) {
            // recently modified; throw out SHA1 values
            Iterator iter = URNS.iterator();
            while(iter.hasNext()){
                if (((URN)iter.next()).isSHA1()) {
                    iter.remove();
                }
            }
        }
        // now check if given urn matches
        Iterator iter = URNS.iterator();
        while(iter.hasNext()){
            if (urn.equals((URN)iter.next())) {
                return true;
            }
        }
        // no match
        return false;
    }
    
    /**
     * Return SHA1 <tt>URN</tt> instance, if available.
	 *
	 * @return the SHA1 <tt>URN</tt> instance if there is one, <tt>null</tt>
	 *  otherwise
     */
    public synchronized URN getSHA1Urn() {
        Iterator iter = URNS.iterator(); 
        while(iter.hasNext()) {
            URN urn = (URN)iter.next();
            if(urn.isSHA1()) {
                return urn;
            }
        }
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

	/**
	 * Returns a new <tt>Set</tt> instance containing the <tt>URN</tt>s
	 * for the this <tt>FileDesc</tt>.
	 *
	 * @return a new <tt>Set</tt> of <tt>URN</tt>s for this 
	 *  <tt>FileDesc</tt>
	 */
	public Set getUrns() {
		return new HashSet(URNS);
	}

    /**
     * Opens an input stream to the <tt>File</tt> instance for this
	 * <tt>FileDesc</tt>.
	 *
	 * @return an <tt>InputStream</tt> to the <tt>File</tt> instance
	 * @throws <tt>FileNotFoundException</tt> if the file represented
	 *  by the <tt>File</tt> instance could not be found
     */
    public InputStream getInputStream() throws FileNotFoundException {
		return new FileInputStream(FILE);
    }
}


