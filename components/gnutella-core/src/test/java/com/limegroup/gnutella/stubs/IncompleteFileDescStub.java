
package com.limegroup.gnutella.stubs;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.limewire.service.ErrorService;

import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;

/**
 * A stub that is identical to FileDescStub.  The code uses instanceof,
 * so we need this stub for Partial files.
 * 
 * It also stubs out some methods.  Feel free to stub out more methods as
 * need arises.
 */
@SuppressWarnings("unchecked")
public class IncompleteFileDescStub extends IncompleteFileDesc {
	public static final String urn = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";
    public static final Set set;
    private static Set localSet2,globalSet;
    public Set localSet;
    
    private boolean _activelyDownloading;
    
    private AlternateLocationCollection _altlocCollection,_pushCollection;
    
    private byte [] _ranges;
    
    public static final int size = 1126400;
    static {
        set = new HashSet();
        globalSet=new HashSet();
        try {
            set.add(URN.createSHA1Urn(urn));
            globalSet.addAll(set);
        } catch(IOException ioe) {
            ErrorService.error(ioe);
        }
    }
    
    public IncompleteFileDescStub() {
        this("abc.txt");
    }
    
    public IncompleteFileDescStub(String name) {
        super(new File(name), set, 0,name,size,null);
        localSet=new HashSet();
        
        FileDescStub.createStubFile(this);
    }
    
    public IncompleteFileDescStub(String name, URN urn, int index) {
    	super(new File(name), createUrnSet(urn),index,name,size,null);
    	localSet=localSet2;
    	
    	FileDescStub.createStubFile(this);
    }
    
    private static Set createUrnSet(URN urn) {
    	localSet2 = new HashSet();
    	localSet2.add(urn);
    	globalSet.add(urn);
    	return localSet2;
    }
    
    public boolean containsUrn(URN urn) {
    	if (globalSet.contains(urn))
    		return true;
    	else return super.containsUrn(urn);
    }

    public long getFileSize() {
        return size;
    }
    
    public URN getSHA1Urn() {
    	if (localSet.isEmpty())
    		return super.getSHA1Urn();
    	else return
			(URN)localSet.toArray()[0];
    }
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.IncompleteFileDesc#getRangesAsByte()
	 */
	public byte[] getRangesAsByte() {
		return _ranges;
	}
	
	public void setRangesByte(byte [] what) {
		_ranges=what;
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.FileDesc#getAlternateLocationCollection()
	 */
	public AlternateLocationCollection getAlternateLocationCollection() {

		return _altlocCollection;
	}
	
	public AlternateLocationCollection getPushAlternateLocationCollection() {

		return _pushCollection;
	}
	
	public void setAlternateLocationCollection(AlternateLocationCollection what) {
		_altlocCollection=what;
	}
	
	public void setPushAlternateLocationCollection(AlternateLocationCollection what) {
		_pushCollection=what;
	}
	
	public boolean addVerified(AlternateLocation al) {
		return _altlocCollection.add(al);
	}
	
	public boolean remove(AlternateLocation al) {
	    return _altlocCollection.remove(al);
	}
	
	public void setActivelyDownloading(boolean yes) {
	    _activelyDownloading=yes;
	}
	
	public boolean isActivelyDownloading() {
	    return _activelyDownloading;
	}
}
