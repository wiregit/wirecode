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
public final class FileDesc {
    
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
    private final Collection /* of URNS */ URNS; 

	/**
	 * <tt>Map</tt> of <tt>AlternateLocation</tt> instances, keyed by
	 * <tt>AlternateLocation</tt>s.
	 */
	private Map/*AlternateLocation->AlternateLocation*/
		_alternateLocations;

	/**
	 * <tt>Map</tt> of temporary <tt>AlternateLocation</tt> instances, 
	 * keyed by <tt>AlternateLocation</tt>s.  This is used to store
	 * any temporary locations that should not be reported in HTTP
	 * headers, at least for now.
	 */
	private Map/*AlternateLocation->AlternateLocation*/
		_temporaryAlternateLocations;

	/**
	 * Constant for the <tt>File</tt> instance.
	 */
	private final File FILE;
		
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

	/**
	 * Adds the specified <tt>AlternateLocation</tt> instance to the list
	 * of alternate locations for this this file.
	 *
	 * @param al the <tt>AlternateLocation</tt> instance to add
	 */
	public synchronized void addAlternateLocation(AlternateLocation al) {
		if(_alternateLocations == null) {
			// we use a TreeMap to both filter duplicates and provide
			// ordering based on the timestamp
			_alternateLocations = 
			    new TreeMap(AlternateLocation.createComparator());
		}

		// note that alternate locations without a timestamp are placed
		// at the end of the map because they have the oldest possible
		// date according to the date class, namely:
		// January 1, 1970, 00:00:00 GMT.
		_alternateLocations.put(al, al);
	}

	/**
	 * Adds the specified <tt>AlternateLocation</tt> instance to the list
	 * of "temporary" alternate locations.  These will not be stored to
	 * the official alternate locations list until a call to 
	 * commitTemporaryAlternateLocations is made.  This is to avoid,
	 * for example, sending back the same alternate location headers
	 * back to an uploader as they sent in with their upload request,
	 * as they clearly already know about those locations.
	 *
	 * @param al the <tt>AlternateLocation</tt> instance to add to the 
	 *  temporary list.
	 */
	public synchronized void addTemporaryAlternateLocation(AlternateLocation al) {
		if(_temporaryAlternateLocations == null) {
			// we use a TreeMap to both filter duplicates and provide
			// ordering based on the timestamp
			_temporaryAlternateLocations = 
			    new TreeMap(AlternateLocation.createComparator());
		}

		// note that alternate locations without a timestamp are placed
		// at the end of the map because they have the oldest possible
		// date according to the date class, namely:
		// January 1, 1970, 00:00:00 GMT.
		_temporaryAlternateLocations.put(al, al);
	}

	/**
	 * Moves all temporary alternate locations to the "official" list of
	 * alternate locations for this file that will be reported in
	 * HTTP headers.
	 */
	public synchronized void commitTemporaryAlternateLocations() {
		// if there are no temporary locations to commit, just return
		if(_temporaryAlternateLocations == null) {
			return;
		}
		if(_alternateLocations == null) {
			_alternateLocations =
			    new TreeMap(AlternateLocation.createComparator());
		}
		_alternateLocations.putAll(_temporaryAlternateLocations);

		// clear out all the temporary locations.  we could set this to
		// null, but there's a good chance that if a file was asigned
		// temporary alternate locations once, then it will be again
		_temporaryAlternateLocations.clear();
	}

	/**
	 * Writes the alternate locations to the specified output stream as
	 * an HTTP header, as specified in the format specified in HUGE v0.93.
	 *
	 * @param os the <tt>OutputStream</tt> instance to write to
	 * @exception <tt>IOException</tt> if we could not write to the
	 *  output stream
	 */
	public synchronized void writeAlternateLocationsTo(OutputStream os) 
		throws IOException {
		// if there are no alternate locations, simply return
		if(_alternateLocations == null) return;

		Iterator iter = _alternateLocations.values().iterator();	   
		StringBuffer writeBuffer = new StringBuffer();
		writeBuffer.append(HTTPConstants.ALTERNATE_LOCATION_HEADER+" ");
		while(iter.hasNext()) {
			writeBuffer.append(((AlternateLocation)iter.next()).toString());
			if(iter.hasNext()) {
				writeBuffer.append(", ");
			}
		}
		writeBuffer.append(HTTPConstants.CRLF);		
		os.write(writeBuffer.toString().getBytes());
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
	 * Returns a new <tt>Response</tt> instance for the data in this
	 * <tt>FileDesc</tt> for the given <tt>QueryRequest</tt>.
	 *
	 * @param qr the <tt>QueryRequest</tt> instance to create a 
	 *  <tt>Response</tt> for
	 * @return a new <tt>Response</tt> instance for this <tt>FileDesc</tt>
	 *  and the given <tt>QueryRequest</tt>
	 */
    public Response responseFor(QueryRequest qr) {
        Response response = new Response(_index,_size,_name);        
        /** Popular approach: return all URNs **/
        Iterator allUrns = URNS.iterator();
        while(allUrns.hasNext()) {
            response.addUrn(((URN)allUrns.next()));
        }
        
        /** 
         * Technically proper approach (by HUGE v.0.93): 
         * give only URNs that are requested
         *
        if(qr.getRequestedUrnTypes()==null) return r;
        Iterator outer = qr.getRequestedUrnTypes().iterator();
        while (outer.hasNext()) {
            Iterator inner = URNS.iterator();
            String req = (String)outer.next();
            while(inner.hasNext()) {
                String urn = (String)inner.next();
                if(urn.startsWith(req)) {
                    r.addUrn(urn);
                }
            }
        }
        /**/
        return response;
        
    }
    
    //
    //
    // URN-calculation and handling
    //
    
    /**
     * would calling the calculation method add useful URNs?
     */
    public boolean shouldCalculateUrns() {
        Iterator iter = URNS.iterator();
        while(iter.hasNext()) {
            if(((URN)iter.next()).isSHA1()) {
                return false; // we already have all the values we can calculate
            }
        }
        return true; // we could calculate a SHA1
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
			urn = URNFactory.createSHA1URN(FILE);
		} catch(IOException e) {
			return;
		}		
		URNS.add(urn);
	}
    
    /**
     * Verify that given <tt>URN</tt> instance is a URN for this
	 * <tt>FileDesc</tt>.
	 *
	 * @param urn the <tt>URN</tt> instance to check for
	 * @return <tt>true</tt> if the <tt>URN</tt> is a valid <tt>URN</tt>
	 *  for this file, <tt>false</tt> otherwise
     */
    public boolean satisfiesUrn(URN urn) {
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
	 * Returns a new <tt>Collection</tt> instance containing the <tt>URN</tt>s
	 * for the this <tt>FileDesc</tt>.
	 *
	 * @return a new <tt>Collection</tt> of <tt>URN</tt>s for this 
	 *  <tt>FileDesc</tt>
	 */
	public Collection getUrns() {
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

    public void print() {
        System.out.println("Name: " + _name);
        System.out.println("Index: " + _index);
        System.out.println("Size: " + _size);
        System.out.println("Path: " + _path);
        System.out.println("URNs: " + URNS);
        System.out.println(" ");
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


