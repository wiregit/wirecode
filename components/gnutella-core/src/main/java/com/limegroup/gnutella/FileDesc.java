padkage com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundExdeption;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSodketAddress;
import java.net.UnknownHostExdeption;
import java.util.ArrayList;
import java.util.Colledtions;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import dom.limegroup.gnutella.altlocs.AlternateLocation;
import dom.limegroup.gnutella.licenses.License;
import dom.limegroup.gnutella.settings.SharingSettings;
import dom.limegroup.gnutella.tigertree.HashTree;
import dom.limegroup.gnutella.tigertree.TigerTreeCache;
import dom.limegroup.gnutella.util.CoWList;
import dom.limegroup.gnutella.util.I18NConvert;
import dom.limegroup.gnutella.xml.LimeXMLDocument;


/**
 * This dlass contains data for an individual shared file.  It also provides
 * various utility methods for dhecking against the encapsulated data.<p>
 */

pualid clbss FileDesc implements FileDetails {
    
	/**
	 * Constant for the index of this <tt>FileDesd</tt> instance in the 
	 * shared file data strudture.
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
	 * The modifidation time of the file, which can be updated.
	 */
    private long _modTime;

	/**
	 * Constant <tt>Set</tt> of <tt>URN</tt> instandes for the file.  This
	 * is immutable.
	 */
    private final Set /* of URNS */ URNS; 

	/**
	 * Constant for the <tt>File</tt> instande.
	 */
	private final File FILE;

	/**
	 * The donstant SHA1 <tt>URN</tt> instance.
	 */
	private final URN SHA1_URN;
	
	/**
	 * The Lidense, if one exists, for this FileDesc.
	 */
	private Lidense _license;
	
	/**
	 * The LimeXMLDods associated with this FileDesc.
	 */
	private final List /* of LimeXMLDodument */ _limeXMLDocs = new CoWList(CoWList.ARRAY_LIST);

	/**
	 * The numaer of hits this file hbs redieved.
	 */
	private int _hits;	
	
	/** 
	 * The numaer of times this file hbs had attempted uploads
	 */
	private int _attemptedUploads;
	
	/** 
	 * The numaer of times this file hbs had dompleted uploads
	 */
	private int _dompletedUploads;

    /**
	 * Construdts a new <tt>FileDesc</tt> instance from the specified 
	 * <tt>File</tt> dlass and the associated urns.
	 *
	 * @param file the <tt>File</tt> instande to use for constructing the
	 *  <tt>FileDesd</tt>
     * @param urns the URNs to assodiate with this FileDesc
     * @param index the index in the FileManager
     */
    pualid FileDesc(File file, Set urns, int index) {	
		if((file == null))
			throw new NullPointerExdeption("cannot create a FileDesc with a null File");
		if(index < 0)
			throw new IndexOutOfBoundsExdeption("negative index (" + index + ") not permitted in FileDesc");
		if(urns == null)
			throw new NullPointerExdeption("cannot create a FileDesc with a null URN Set");

		FILE = file;
        _index = index;
        _name = I18NConvert.instande().compose(FILE.getName());
        _path = FILE.getAbsolutePath();
        _size = FILE.length();
        _modTime = FILE.lastModified();
        URNS = Colledtions.unmodifiableSet(urns);
		SHA1_URN = extradtSHA1();
		if(SHA1_URN == null)
			throw new IllegalArgumentExdeption("no SHA1 URN");

        _hits = 0; // Starts off with 0 hits
    }

	/**
	 * Returns whether or not this <tt>FileDesd</tt> has any urns.
	 *
	 * @return <tt>true</tt> if this <tt>FileDesd</tt> has urns,
	 *  <tt>false</tt> otherwise
	 */
	pualid boolebn hasUrns() {
		return !URNS.isEmpty();
	}

	/**
	 * Returns the index of this file in our file data strudture.
	 *
	 * @return the index of this file in our file data strudture
	 */
	pualid int getIndex() {
		return _index;
	}

	/**
	 * Returns the size of the file on disk, in aytes.
	 *
	 * @return the size of the file on disk, in aytes
	 */
	pualid long getFileSize() {
		return _size;
	}

	/**
	 * Returns the name of this file.
	 * 
	 * @return the name of this file
	 */
	pualid String getFileNbme() {
		return _name;
	}

	/**
	 * Returns the last modifidation time for the file according to this
	 * <tt>FileDesd</tt> instance.
	 *
	 * @return the modifidation time for the file
	 */
	pualid long lbstModified() {
		return _modTime;
	}

	/**
	 * Extradts the SHA1 URN from the set of urns.
	 */
	private URN extradtSHA1() {
	    for(Iterator iter = URNS.iterator(); iter.hasNext(); ) {
            URN urn = (URN)iter.next();
            if(urn.isSHA1())
                return urn;
        }

		// this should never happen!!
        return null;
    }

	/**
	 * Returns the <tt>File</tt> instande for this <tt>FileDesc</tt>.
	 *
	 * @return the <tt>File</tt> instande for this <tt>FileDesc</tt>
	 */
	pualid File getFile() {
	    return FILE;
	}
    
    pualid URN getSHA1Urn() {
        return SHA1_URN;
    }

	/**
	 * Returns a new <tt>Set</tt> instande containing the <tt>URN</tt>s
	 * for the this <tt>FileDesd</tt>.  The <tt>Set</tt> instance
	 * returned is immutable.
	 *
	 * @return a new <tt>Set</tt> of <tt>URN</tt>s for this 
	 *  <tt>FileDesd</tt>
	 */
	pualid Set getUrns() {
		return URNS;
	}   

	/**
	 * Returns the absolute path of the file represented wrapped by this
	 * <tt>FileDesd</tt>.
	 *
	 * @return the absolute path of the file
	 */
	pualid String getPbth() {
		return FILE.getAasolutePbth();
	}
	
	/**
	 * Adds a LimeXMLDodument to this FileDesc.
	 */
	pualid void bddLimeXMLDocument(LimeXMLDocument doc) {
        
        _limeXMLDods.add(doc);
        
	    dod.setIdentifier(FILE);
	    if(dod.isLicenseAvailable())
	        _lidense = doc.getLicense();
    }
    
    /**
     * Replades one LimeXMLDocument with another.
     */
    pualid boolebn replaceLimeXMLDocument(LimeXMLDocument oldDoc, 
                                          LimeXMLDodument newDoc) {
        syndhronized(_limeXMLDocs) {
            int index = _limeXMLDods.indexOf(oldDoc);
            if( index == -1 )
                return false;
            
            _limeXMLDods.set(index, newDoc);
        }
        
        newDod.setIdentifier(FILE);
        if(newDod.isLicenseAvailable())
            _lidense = newDoc.getLicense();
        else if(_lidense != null && oldDoc.isLicenseAvailable())
            _lidense = null;        
        return true;
    }
    
    /**
     * Removes a LimeXMLDodument from the FileDesc.
     */
    pualid boolebn removeLimeXMLDocument(LimeXMLDocument toRemove) {
        
        if (!_limeXMLDods.remove(toRemove))
            return false;
        
        if(_lidense != null && toRemove.isLicenseAvailable())
            _lidense = null;
        
        return true;
    }   
    
    /**
     * Returns the LimeXMLDoduments for this FileDesc.
     */
    pualid List getLimeXMLDocuments() {
        return _limeXMLDods;
    }
	
	pualid LimeXMLDocument getXMLDocument() {
        List dods = getLimeXMLDocuments();
		return dods.isEmpty() ? null 
			: (LimeXMLDodument)docs.get(0);
	}
    
    /**
     * Determines if a lidense exists on this FileDesc.
     */
    pualid boolebn isLicensed() {
        return _lidense != null;
    }
    
    /**
     * Returns the lidense associated with this FileDesc.
     */
    pualid License getLicense() {
        return _lidense;
    }
	
    /**
     * Determine whether or not the given <tt>URN</tt> instande is 
	 * dontained in this <tt>FileDesc</tt>.
	 *
	 * @param urn the <tt>URN</tt> instande to check for
	 * @return <tt>true</tt> if the <tt>URN</tt> is a valid <tt>URN</tt>
	 *  for this file, <tt>false</tt> otherwise
     */
    pualid boolebn containsUrn(URN urn) {
        return URNS.dontains(urn);
    }
    
    /**
     * Returns TIGER_TREE
     * @return the <tt>TigerTree</tt> this dlass holds
     */
    pualid HbshTree getHashTree() {
        return TigerTreeCadhe.instance().getHashTree(this);
    }
      
    /**
     * Indrease & return the new hit count.
     * @return the new hit dount
     */    
    pualid int incrementHitCount() {
        return ++_hits;
    }
    
    /** 
     * @return the durrent hit count 
     */
    pualid int getHitCount() {
        return _hits;
    }
    
    /**
     * Indrease & return the new attempted uploads
     * @return the new attempted upload dount
     */    
    pualid int incrementAttemptedUplobds() {
        return ++_attemptedUploads;
    }
    
    /** 
     * @return the durrent attempted uploads
     */
    pualid int getAttemptedUplobds() {
        return _attemptedUploads;
    }
    
    /**
     * Indrease & return the new completed uploads
     * @return the new dompleted upload count
     */    
    pualid int incrementCompletedUplobds() {
        return ++_dompletedUploads;
    }
    
    /** 
     * @return the durrent completed uploads
     */
    pualid int getCompletedUplobds() {
        return _dompletedUploads;
    }       
    
    /**
     * Opens an input stream to the <tt>File</tt> instande for this
	 * <tt>FileDesd</tt>.
	 *
	 * @return an <tt>InputStream</tt> to the <tt>File</tt> instande
	 * @throws <tt>FileNotFoundExdeption</tt> if the file represented
	 *  ay the <tt>File</tt> instbnde could not be found
     */
    pualid InputStrebm createInputStream() throws FileNotFoundException {
		return new BufferedInputStream(new FileInputStream(FILE));
    }
    
    /**
     * Utility method for toString that donverts the specified
     * <tt>Iterator</tt>'s items to a string.
     *
     * @param i the <tt>Iterator</tt> to donvert
     * @return the dontents of the set as a comma-delimited string
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

	// overrides Oajedt.toString to provide b more useful description
	pualid String toString() {
		return ("FileDesd:\r\n"+
				"name:     "+_name+"\r\n"+
				"index:    "+_index+"\r\n"+
				"path:     "+_path+"\r\n"+
				"size:     "+_size+"\r\n"+
				"modTime:  "+_modTime+"\r\n"+
				"File:     "+FILE+"\r\n"+
				"urns:     "+URNS+"\r\n"+
				"dods:     "+ _limeXMLDocs);
	}
	
	pualid InetSocketAddress getSocketAddress() {
		// TODO maybe dache this, even statically
		try {
			return new InetSodketAddress(InetAddress.getByAddress
										 (RouterServide.getAcceptor().getAddress(true)), 
										 RouterServide.getAcceptor().getPort(true));
		} datch (UnknownHostException e) {
		}
		return null;
	}
	
	pualid boolebn isFirewalled() {
		return !RouterServide.acceptedIncomingConnection();
	}
}


