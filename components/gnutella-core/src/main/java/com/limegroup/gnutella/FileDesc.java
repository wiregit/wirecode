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
	 * Constant <tt>Set</tt> of <tt>URN</tt> instances for the file.  This
	 * is immutable.
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
	 * Constant for an empty, unmodifiable <tt>Set</tt>.  This is necessary
	 * because Collections.EMPTY_SET is not serializable in the collections 1.1
	 * implementation.
	 */
	private static final Set EMPTY_SET = 
		Collections.unmodifiableSet(new HashSet());
		
    /**
	 * Constructs a new <tt>FileDesc</tt> instance from the specified 
	 * <tt>File</tt> class and the associated urns.
	 *
	 * @param file the <tt>File</tt> instance to use for constructing the
	 *  <tt>FileDesc</tt>
	 * @param urns the <tt>Set</tt> of URNs for this <tt>FileDesc</tt>
     */
    public FileDesc(File file, int index) {	
		if((file == null)) {
			throw new NullPointerException("cannot create a FileDesc with "+
										   "a null File");
		}
		if(index < 0) {
			throw new IndexOutOfBoundsException("negative values not "+
												"permitted in FileDesc: " +
												index);
		}
		// make a defensive copy
		FILE = new File(file.getAbsolutePath());
        _index = index;
        _name = FILE.getName();
        _path = FILE.getAbsolutePath(); //TODO: right method?
        _size = (int)FILE.length();
        _modTime = FILE.lastModified();
		
		Set urns = UrnCache.instance().getUrns(FILE);
		if(urns.size() == 0) {			
			// expensive the first time a new file is added
			URNS = Collections.unmodifiableSet(calculateUrns(FILE));
			UrnCache.instance().addUrns(FILE, URNS);
		}
		else {
			URNS = urns;
		}
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
	 * Returns the last modification time for the file according to this
	 * <tt>FileDesc</tt> instance.
	 *
	 * @return the modification time for the file
	 */
	public long lastModified() {
		return _modTime;
	}

    /**
     * Return SHA1 <tt>URN</tt> instance, if available.
	 *
	 * @return the SHA1 <tt>URN</tt> instance if there is one, <tt>null</tt>
	 *  otherwise
     */
    public URN getSHA1Urn() {
        Iterator iter = URNS.iterator(); 
        while(iter.hasNext()) {
            URN urn = (URN)iter.next();
            if(urn.isSHA1()) {
                return urn;
            }
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
     * Adds any URNs that can be locally calculated; may take a while to 
	 * complete on large files.
	 *
	 * @param file the <tt>File</tt> instance to calculate URNs for
	 * @return the new <tt>Set</tt> of calculated <tt>URN</tt> instances
     */
    private static Set calculateUrns(File file) {
		try {
			Set set = new HashSet();
			set.add(URNFactory.createSHA1Urn(file));
			return set;
		} catch(IOException e) {
			// the urn just does not get added
			return EMPTY_SET;
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
    public boolean containsUrn(URN urn) {
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
     * Opens an input stream to the <tt>File</tt> instance for this
	 * <tt>FileDesc</tt>.
	 *
	 * @return an <tt>InputStream</tt> to the <tt>File</tt> instance
	 * @throws <tt>FileNotFoundException</tt> if the file represented
	 *  by the <tt>File</tt> instance could not be found
     */
    public InputStream createInputStream() throws FileNotFoundException {
		return new FileInputStream(FILE);
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
				"alt locs: "+_altLocs+"\r\n");
	}
}


