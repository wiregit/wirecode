package com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.AlternateLocationCollector;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.TigerTreeCache;
import com.limegroup.gnutella.xml.LimeXMLDocument;


/**
 * This class contains data for an individual shared file.  It also provides
 * various utility methods for checking against the encapsulated data.<p>
 *
 * Constructing a FileDesc is usually done in two steps, which allows the caller
 * to avoid holding a lock when hashing a file:
 * <pre>
 *    Set urns=FileDesc.calculateAndCacheURN(file);
 *    FileDesc fd=new FileDesc(file, urns, index);
 * </pre>
 */
public class FileDesc implements AlternateLocationCollector {
    
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
	 * The size of the file, casted to an <tt>int</tt>.
	 */
    private final int _size;

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
	 * The LimeXMLDocs associated with this FileDesc.
	 *
	 * Never add or remove from this list.  Always create a new list.
	 * This has extra overhead when adding/removing, but makes synchronization
	 * much much easier.
	 */
	private volatile List /* of LimeXMLDocument */ _limeXMLDocs = 
	    Collections.EMPTY_LIST;

	/**
	 * The collection of alternate locations for the file.
	 */
	protected AlternateLocationCollection ALT_LOCS;
	
	/**
	 * The collection of firewalled alternate locations for the file.
	 */
	protected AlternateLocationCollection PUSH_ALT_LOCS;
	
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
     * @param urns the return value from calculateAndCacheURN(file);
     *  an unmodifiable <tt>Set</tt> of <tt>URN</tt>'s.
     * @param index the index in the FileManager
     * @see calculateAndCacheURN
     */
    public FileDesc(File file, Set urns, int index) {	
		if((file == null)) {
			throw new NullPointerException("cannot create a FileDesc with "+
										   "a null File");
		}
		if(index < 0) {
			throw new IndexOutOfBoundsException("negative values not "+
												"permitted in FileDesc: " +
												index);
		}
		if(urns == null) {
			throw new NullPointerException("cannot create a FileDesc with "+
										   "a null URN Set");
		}
		// make a defensive copy
		FILE = new File(file.getAbsolutePath());
        _index = index;
        _name = FILE.getName();
        _path = FILE.getAbsolutePath(); //TODO: right method?
        _size = (int)FILE.length();
        _modTime = FILE.lastModified();
        URNS = Collections.unmodifiableSet(urns);
		SHA1_URN = extractSHA1();
		if(SHA1_URN == null) {
			throw new IllegalArgumentException("no SHA1 URN");
		}
        ALT_LOCS = AlternateLocationCollection.create(SHA1_URN);
        PUSH_ALT_LOCS = AlternateLocationCollection.create(SHA1_URN);
        try {
            addUrnsForSelf();
        } catch(IllegalArgumentException e) {
            // oh well, we'll add it later.
        }                        
        _hits = 0; // Starts off with 0 hits
    }		

    /** 
     * Returns the set of URNs for a file to be passed to the FileDesc
     * constructor.  This is done by looking it up in UrnCache or calculating it
     * from disk.  constructor.  Updates the UrnCache. 
     * 
     * @return an unmodifiable <tt>Set</tt> of <tt>URN</tt>.  If the calling
     * thread is interrupted while executing this, returns an empty set.  
	 * @throws <tt>NullPointerException</tt> if the <tt>file</tt> argument is
	 *  <tt>null</tt>
     * @throws <tt>IOException</tt> if there is an IO error calculating the 
     *  URN
     * @throws <tt>InterruptedException</tt> if the thread that calculates
     *  the URN is interrupted
     */
    public static Set /* of URN */ calculateAndCacheURN(File file) 
        throws IOException, InterruptedException {
        if(file == null)
            throw new NullPointerException("cannot accept null file argument");
		if(!file.isFile())
			throw new IOException("not a file: "+file);
		Set urns = UrnCache.instance().getUrns(file);
        // TODO: If we ever create more URN types (other than SHA1)
        // we cannot just check for size == 0, we must check for
        // size == NUM_URNS_WE_WANT, and calculateUrns should only
        // calculate the URN for the specific hash we still need.
		if(urns.size() == 0) {			
			// expensive the first time a new file is added
			urns = calculateUrns(file);
			UrnCache.instance().addUrns(file, urns);
		}
        return urns;
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

    // inherit doc comment
    public URN getSHA1Urn() {
		return SHA1_URN;
	}

	/**
	 * Extracts the SHA1 URN from the set of urns.
	 */
	private URN extractSHA1() {
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
	    List newDocs = new ArrayList(_limeXMLDocs.size() + 1);
	    newDocs.addAll(_limeXMLDocs);
	    newDocs.add(doc);
	    doc.setIdentifier(FILE);
	    _limeXMLDocs = newDocs;
    }
    
    /**
     * Replaces one LimeXMLDocument with another.
     */
    public boolean replaceLimeXMLDocument(LimeXMLDocument oldDoc, 
                                          LimeXMLDocument newDoc) {
        int index = _limeXMLDocs.indexOf(oldDoc);
        if( index == -1 )
            return false;
            
        List newDocs = new ArrayList(_limeXMLDocs);
        Object removed = newDocs.remove(index);
        Assert.that(removed == oldDoc, "wrong doc removed!");
        newDocs.add(newDoc);
        newDoc.setIdentifier(FILE);
        _limeXMLDocs = newDocs;
        return true;
    }
    
    /**
     * Removes a LimeXMLDocument from the FileDesc.
     */
    public boolean removeLimeXMLDocument(LimeXMLDocument toRemove) {
        if( _limeXMLDocs.size() == 0 ) 
            return false;
        
        List newDocs = new ArrayList(_limeXMLDocs);
        boolean removed = newDocs.remove(toRemove);
        _limeXMLDocs = newDocs;
        return removed;
    }   
    
    /**
     * Returns the LimeXMLDocuments for this FileDesc.
     */
    public List getLimeXMLDocuments() {
        return _limeXMLDocs;
    }

	/**
	 * @return the <tt>AlternateLocationCollection</tt> for this 
	 *  <tt>FileDesc</tt> instance, which can be empty, or <tt>null</tt>
	 *  if it is not initialized
	 */
	public AlternateLocationCollection getAlternateLocationCollection() {
		return ALT_LOCS;
	}
	
	public AlternateLocationCollection getPushAlternateLocationCollection() {
		return PUSH_ALT_LOCS;
	}
	
	/**
	 * Adds URNs for this' location to the alternate location collection.
	 * This should be called whenever the address changes.
	 */
    public void addUrnsForSelf() {
        if(SharingSettings.ADD_ALTERNATE_FOR_SELF.getValue()) {
        	AlternateLocation alt = AlternateLocation.create(SHA1_URN);
        	if (alt instanceof DirectAltLoc)
        		ALT_LOCS.add(alt);
        	else
        		PUSH_ALT_LOCS.add(alt);
        }
    }
	

	/** 
	 * Implements <tt>AlternateLocationCollector</tt> interface.
	 *
	 * @throws <tt>NullPointerException</tt> if the argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the alternate location
	 *  has a different SHA1 than this file, or if its sha1 is <tt>null</tt>
	 */
	public boolean add(AlternateLocation al) {
        if(al == null) {
            throw new NullPointerException("cannot accept null alt locs");
        }
		URN sha1 = al.getSHA1Urn();
		if(sha1 == null) {
			throw new IllegalArgumentException("sha1 cannot be null");
		}
		if(!sha1.equals(SHA1_URN)) {
			throw new IllegalArgumentException("URN does not match:\n"+
											   SHA1_URN+"\n"+sha1);
		}
		
		if (al instanceof DirectAltLoc)
			return ALT_LOCS.add(al);
		else
			return PUSH_ALT_LOCS.add(al);
		
	}
    /**
     * @throws <tt>NullPointerException</tt> if the argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the alternate location
	 *  has a different SHA1 than this file, or if its sha1 is <tt>null</tt>
     */
    public boolean remove(AlternateLocation al) {
        if(al == null)
            throw new NullPointerException("cannot accept null alt locs");

		URN sha1 = al.getSHA1Urn();
		if(sha1 == null)
			throw new IllegalArgumentException("sha1 cannot be null");

		if(!sha1.equals(SHA1_URN))
			throw new IllegalArgumentException("URN does not match:\n"+
											   SHA1_URN+"\n"+sha1);

		if (al instanceof DirectAltLoc)
			return ALT_LOCS.remove(al);
		else
			return PUSH_ALT_LOCS.remove(al);
    }

	/**
     * Implements the <tt>AlternateLocationCollector</tt> interface.
     * Adds the specified <tt>AlternateLocationCollection</tt> to this 
     * collection.
     *
     * @param alc the <tt>AlternateLocationCollection</tt> to add
     * @throws <tt>NullPointerException</tt> if <tt>alc</tt> is 
     *  <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the SHA1 of the
     *  collection to add does not match the collection of <tt>this</tt>
     */
	public int addAll(AlternateLocationCollection alc) {
        if(alc == null) {
            throw new NullPointerException("cannot accept null alt loc coll");
        }
		if(!alc.getSHA1Urn().equals(SHA1_URN)) {
			throw new IllegalArgumentException("SHA1 does not match:\n"+
											   SHA1_URN+"\n"+alc.getSHA1Urn());
		}
		
		int added =0;
		
		for (Iterator iter= alc.iterator();iter.hasNext();)
			if (add((AlternateLocation)iter.next()))
				added++;
			
		return added;
	}

	// implements AlternateLocationCollector interface
	public boolean hasAlternateLocations() {
		return ALT_LOCS.hasAlternateLocations() ||
			PUSH_ALT_LOCS.hasAlternateLocations();
	}
	
	// implements AlternateLocationCollector interface
	public int getAltLocsSize() {
	    return ALT_LOCS.getAltLocsSize() + PUSH_ALT_LOCS.getAltLocsSize();
	}
	
    
    /**
     * Adds any URNs that can be locally calculated; may take a while to 
	 * complete on large files.
	 *
	 * @param file the <tt>File</tt> instance to calculate URNs for
	 * @return the new <tt>Set</tt> of calculated <tt>URN</tt> instances.  If 
     * the calling thread is interrupted while executing this, returns an empty
     * set.
     */
    private static Set calculateUrns(File file) 
        throws IOException, InterruptedException {
        Set set = new HashSet(1);
        set.add(URN.createSHA1Urn(file));
        return set;
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
        if(urn == null) {
            throw new NullPointerException("null URNS not allowed in containsUrn");
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
				"urns:     "+listInformation(URNS.iterator())+"\r\n"+
				"docs:     "+
				 (_limeXMLDocs == null ? "null" : 
				        listInformation(_limeXMLDocs.iterator()) )
				            +"\r\n"+
				"alt locs: "+ALT_LOCS+"\r\n"+
				"push locs: "+PUSH_ALT_LOCS+"\r\n");
	}
}


