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
	private final AlternateLocationCollection ALT_LOCS = 
		new AlternateLocationCollection();
		
    /**
	 * Constructs a new <tt>FileDesc</tt> instance from the specified 
	 * <tt>File</tt> class and the associated urns.
	 *
	 * @param file the <tt>File</tt> instance to use for constructing the
	 *  <tt>FileDesc</tt>
	 * @param urns the <tt>HashSet</tt> of URNs for this <tt>FileDesc</tt>
     */
    public FileDesc(File file, int index, Collection urns) {		
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

	public int getIndex() {
		return _index;
	}

	public long getSize() {
		return _size;
	}

	public String getName() {
		return _name;
	}

	/**
	 * Adds the specified <tt>AlternateLocation</tt> instance to the list
	 * of alternate locations for this this file.
	 *
	 * @param al the <tt>AlternateLocation</tt> instance to add
	 */
	public void addAlternateLocation(AlternateLocation al) {
		ALT_LOCS.addAlternateLocation(al);
	}

	public void addAlternateLocationCollection(AlternateLocationCollection alc) {
		ALT_LOCS.addAlternateLocationCollection(alc);
	}

	public AlternateLocationCollection getAlternateLocationCollection() {
		return ALT_LOCS;
	}

	public boolean hasAlternateLocations() {
		return ALT_LOCS.hasAlternateLocations();
	}

	/**
	 * Writes the SHA1 URN for this file out to the specified stream in
	 * the format described in HUGE v0.93.
	 * 
	 * @param os the <tt>OutputStream</tt> instance to write to
	 * @exception <tt>IOException</tt> if we could not write to the
	 *  output stream
	 */
	public synchronized void writeUrnTo(OutputStream os) 
		throws IOException {
		URN urn = this.getSHA1Urn();
		if(urn == null) return;
		String str = HTTPConstants.CONTENT_URN_HEADER+" "+urn+HTTPConstants.CRLF;
		os.write(str.getBytes());
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
	 * namely the <tt>Collection</tt> of <tt>URN</tt>s.
     */
    public void calculateUrns() {
		// update modTime
		_modTime = FILE.lastModified();
		URN urn =  null;
		try {
			urn = URNFactory.createSHA1Urn(FILE);
		} catch(IOException e) {
			return;
		}		
		URNS.add(urn);
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
    public URN getSHA1Urn() {
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

	/*
	public static void main(String[] args) {
		FileDesc fd = new FileDesc(new File("FileDesc.java"), 0, 
								   new HashSet());
		String[] validDistinctLocations = {
			"Alt-Location: http://Y.Y.Y.Y:6352/get/2/"+
			"lime%20capital%20management%2001.mpg "+
			"2002-04-09T20:32:33Z",
			"Alt-Location: http://Y.Y.Y.Y:6352/get/2/"+
			"lime%20capital%20management%2001.mpg"
		};

		OutputStream os = new ByteArrayOutputStream();	   
		try {
			for(int i=0; i<validDistinctLocations.length; i++) {
				AlternateLocation al = 
				    new AlternateLocation(validDistinctLocations[i]);
				fd.addAlternateLocation(al);
			}
			fd.writeAlternateLocationsTo(os);
		} catch(IOException e) {
			e.printStackTrace();
		}
		System.out.println("FILE DESC ALT LOCATIONS:");
		System.out.println(); 
		System.out.println(os); 		
	}
	*/

}


