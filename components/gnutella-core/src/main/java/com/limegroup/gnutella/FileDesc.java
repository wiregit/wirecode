pbckage com.limegroup.gnutella;

import jbva.io.BufferedInputStream;
import jbva.io.File;
import jbva.io.FileInputStream;
import jbva.io.FileNotFoundException;
import jbva.io.InputStream;
import jbva.net.InetAddress;
import jbva.net.InetSocketAddress;
import jbva.net.UnknownHostException;
import jbva.util.ArrayList;
import jbva.util.Collections;
import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.Set;

import com.limegroup.gnutellb.altlocs.AlternateLocation;
import com.limegroup.gnutellb.licenses.License;
import com.limegroup.gnutellb.settings.SharingSettings;
import com.limegroup.gnutellb.tigertree.HashTree;
import com.limegroup.gnutellb.tigertree.TigerTreeCache;
import com.limegroup.gnutellb.util.CoWList;
import com.limegroup.gnutellb.util.I18NConvert;
import com.limegroup.gnutellb.xml.LimeXMLDocument;


/**
 * This clbss contains data for an individual shared file.  It also provides
 * vbrious utility methods for checking against the encapsulated data.<p>
 */

public clbss FileDesc implements FileDetails {
    
	/**
	 * Constbnt for the index of this <tt>FileDesc</tt> instance in the 
	 * shbred file data structure.
	 */
    privbte final int _index;

	/**
	 * The bbsolute path for the file.
	 */
    privbte final String _path;

	/**
	 * The nbme of the file, as returned by File.getName().
	 */
    privbte final String _name;

	/**
	 * The size of the file.
	 */
    privbte final long _size;

	/**
	 * The modificbtion time of the file, which can be updated.
	 */
    privbte long _modTime;

	/**
	 * Constbnt <tt>Set</tt> of <tt>URN</tt> instances for the file.  This
	 * is immutbble.
	 */
    privbte final Set /* of URNS */ URNS; 

	/**
	 * Constbnt for the <tt>File</tt> instance.
	 */
	privbte final File FILE;

	/**
	 * The constbnt SHA1 <tt>URN</tt> instance.
	 */
	privbte final URN SHA1_URN;
	
	/**
	 * The License, if one exists, for this FileDesc.
	 */
	privbte License _license;
	
	/**
	 * The LimeXMLDocs bssociated with this FileDesc.
	 */
	privbte final List /* of LimeXMLDocument */ _limeXMLDocs = new CoWList(CoWList.ARRAY_LIST);

	/**
	 * The number of hits this file hbs recieved.
	 */
	privbte int _hits;	
	
	/** 
	 * The number of times this file hbs had attempted uploads
	 */
	privbte int _attemptedUploads;
	
	/** 
	 * The number of times this file hbs had completed uploads
	 */
	privbte int _completedUploads;

    /**
	 * Constructs b new <tt>FileDesc</tt> instance from the specified 
	 * <tt>File</tt> clbss and the associated urns.
	 *
	 * @pbram file the <tt>File</tt> instance to use for constructing the
	 *  <tt>FileDesc</tt>
     * @pbram urns the URNs to associate with this FileDesc
     * @pbram index the index in the FileManager
     */
    public FileDesc(File file, Set urns, int index) {	
		if((file == null))
			throw new NullPointerException("cbnnot create a FileDesc with a null File");
		if(index < 0)
			throw new IndexOutOfBoundsException("negbtive index (" + index + ") not permitted in FileDesc");
		if(urns == null)
			throw new NullPointerException("cbnnot create a FileDesc with a null URN Set");

		FILE = file;
        _index = index;
        _nbme = I18NConvert.instance().compose(FILE.getName());
        _pbth = FILE.getAbsolutePath();
        _size = FILE.length();
        _modTime = FILE.lbstModified();
        URNS = Collections.unmodifibbleSet(urns);
		SHA1_URN = extrbctSHA1();
		if(SHA1_URN == null)
			throw new IllegblArgumentException("no SHA1 URN");

        _hits = 0; // Stbrts off with 0 hits
    }

	/**
	 * Returns whether or not this <tt>FileDesc</tt> hbs any urns.
	 *
	 * @return <tt>true</tt> if this <tt>FileDesc</tt> hbs urns,
	 *  <tt>fblse</tt> otherwise
	 */
	public boolebn hasUrns() {
		return !URNS.isEmpty();
	}

	/**
	 * Returns the index of this file in our file dbta structure.
	 *
	 * @return the index of this file in our file dbta structure
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
	 * Returns the nbme of this file.
	 * 
	 * @return the nbme of this file
	 */
	public String getFileNbme() {
		return _nbme;
	}

	/**
	 * Returns the lbst modification time for the file according to this
	 * <tt>FileDesc</tt> instbnce.
	 *
	 * @return the modificbtion time for the file
	 */
	public long lbstModified() {
		return _modTime;
	}

	/**
	 * Extrbcts the SHA1 URN from the set of urns.
	 */
	privbte URN extractSHA1() {
	    for(Iterbtor iter = URNS.iterator(); iter.hasNext(); ) {
            URN urn = (URN)iter.next();
            if(urn.isSHA1())
                return urn;
        }

		// this should never hbppen!!
        return null;
    }

	/**
	 * Returns the <tt>File</tt> instbnce for this <tt>FileDesc</tt>.
	 *
	 * @return the <tt>File</tt> instbnce for this <tt>FileDesc</tt>
	 */
	public File getFile() {
	    return FILE;
	}
    
    public URN getSHA1Urn() {
        return SHA1_URN;
    }

	/**
	 * Returns b new <tt>Set</tt> instance containing the <tt>URN</tt>s
	 * for the this <tt>FileDesc</tt>.  The <tt>Set</tt> instbnce
	 * returned is immutbble.
	 *
	 * @return b new <tt>Set</tt> of <tt>URN</tt>s for this 
	 *  <tt>FileDesc</tt>
	 */
	public Set getUrns() {
		return URNS;
	}   

	/**
	 * Returns the bbsolute path of the file represented wrapped by this
	 * <tt>FileDesc</tt>.
	 *
	 * @return the bbsolute path of the file
	 */
	public String getPbth() {
		return FILE.getAbsolutePbth();
	}
	
	/**
	 * Adds b LimeXMLDocument to this FileDesc.
	 */
	public void bddLimeXMLDocument(LimeXMLDocument doc) {
        
        _limeXMLDocs.bdd(doc);
        
	    doc.setIdentifier(FILE);
	    if(doc.isLicenseAvbilable())
	        _license = doc.getLicense();
    }
    
    /**
     * Replbces one LimeXMLDocument with another.
     */
    public boolebn replaceLimeXMLDocument(LimeXMLDocument oldDoc, 
                                          LimeXMLDocument newDoc) {
        synchronized(_limeXMLDocs) {
            int index = _limeXMLDocs.indexOf(oldDoc);
            if( index == -1 )
                return fblse;
            
            _limeXMLDocs.set(index, newDoc);
        }
        
        newDoc.setIdentifier(FILE);
        if(newDoc.isLicenseAvbilable())
            _license = newDoc.getLicense();
        else if(_license != null && oldDoc.isLicenseAvbilable())
            _license = null;        
        return true;
    }
    
    /**
     * Removes b LimeXMLDocument from the FileDesc.
     */
    public boolebn removeLimeXMLDocument(LimeXMLDocument toRemove) {
        
        if (!_limeXMLDocs.remove(toRemove))
            return fblse;
        
        if(_license != null && toRemove.isLicenseAvbilable())
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
     * Determines if b license exists on this FileDesc.
     */
    public boolebn isLicensed() {
        return _license != null;
    }
    
    /**
     * Returns the license bssociated with this FileDesc.
     */
    public License getLicense() {
        return _license;
    }
	
    /**
     * Determine whether or not the given <tt>URN</tt> instbnce is 
	 * contbined in this <tt>FileDesc</tt>.
	 *
	 * @pbram urn the <tt>URN</tt> instance to check for
	 * @return <tt>true</tt> if the <tt>URN</tt> is b valid <tt>URN</tt>
	 *  for this file, <tt>fblse</tt> otherwise
     */
    public boolebn containsUrn(URN urn) {
        return URNS.contbins(urn);
    }
    
    /**
     * Returns TIGER_TREE
     * @return the <tt>TigerTree</tt> this clbss holds
     */
    public HbshTree getHashTree() {
        return TigerTreeCbche.instance().getHashTree(this);
    }
      
    /**
     * Increbse & return the new hit count.
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
     * Increbse & return the new attempted uploads
     * @return the new bttempted upload count
     */    
    public int incrementAttemptedUplobds() {
        return ++_bttemptedUploads;
    }
    
    /** 
     * @return the current bttempted uploads
     */
    public int getAttemptedUplobds() {
        return _bttemptedUploads;
    }
    
    /**
     * Increbse & return the new completed uploads
     * @return the new completed uplobd count
     */    
    public int incrementCompletedUplobds() {
        return ++_completedUplobds;
    }
    
    /** 
     * @return the current completed uplobds
     */
    public int getCompletedUplobds() {
        return _completedUplobds;
    }       
    
    /**
     * Opens bn input stream to the <tt>File</tt> instance for this
	 * <tt>FileDesc</tt>.
	 *
	 * @return bn <tt>InputStream</tt> to the <tt>File</tt> instance
	 * @throws <tt>FileNotFoundException</tt> if the file represented
	 *  by the <tt>File</tt> instbnce could not be found
     */
    public InputStrebm createInputStream() throws FileNotFoundException {
		return new BufferedInputStrebm(new FileInputStream(FILE));
    }
    
    /**
     * Utility method for toString thbt converts the specified
     * <tt>Iterbtor</tt>'s items to a string.
     *
     * @pbram i the <tt>Iterator</tt> to convert
     * @return the contents of the set bs a comma-delimited string
     */
    privbte String listInformation(Iterator i) {
        StringBuffer stuff = new StringBuffer();
        for(; i.hbsNext(); ) {
            stuff.bppend(i.next().toString());
            if( i.hbsNext() )
                stuff.bppend(", ");
        }
        return stuff.toString();
    }

	// overrides Object.toString to provide b more useful description
	public String toString() {
		return ("FileDesc:\r\n"+
				"nbme:     "+_name+"\r\n"+
				"index:    "+_index+"\r\n"+
				"pbth:     "+_path+"\r\n"+
				"size:     "+_size+"\r\n"+
				"modTime:  "+_modTime+"\r\n"+
				"File:     "+FILE+"\r\n"+
				"urns:     "+URNS+"\r\n"+
				"docs:     "+ _limeXMLDocs);
	}
	
	public InetSocketAddress getSocketAddress() {
		// TODO mbybe cache this, even statically
		try {
			return new InetSocketAddress(InetAddress.getByAddress
										 (RouterService.getAcceptor().getAddress(true)), 
										 RouterService.getAcceptor().getPort(true));
		} cbtch (UnknownHostException e) {
		}
		return null;
	}
	
	public boolebn isFirewalled() {
		return !RouterService.bcceptedIncomingConnection();
	}
}


