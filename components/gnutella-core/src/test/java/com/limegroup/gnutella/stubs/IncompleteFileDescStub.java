
package com.limegroup.gnutella.stubs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import java.util.HashSet;
import java.util.Set;

/**
 * A stub that is identical to FileDescStub.  The code uses instanceof,
 * so we need this stub for Partial files.
 * 
 * It also stubs out some methods.  Feel free to stub out more methods as
 * need arises.
 */
public class IncompleteFileDescStub extends IncompleteFileDesc {
	public static final String urn = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";
    public static final Set set;
    private static Set localSet2,globalSet;
    public Set localSet;
    
    private AlternateLocationCollection _altlocCollection;
    
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
    }
    
    public IncompleteFileDescStub(String name, URN urn, int index) {
    	super(new File(name), createUrnSet(urn),index,name,size,null);
    	localSet=localSet2;
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

    public InputStream createInputStream() {
        return new InputStream() {
            public int read() {
                return 'a';
            }
            public int read(byte[] b) {
                for(int i=0; i < b.length; i++)
                    b[i] = (byte)'a';
                return b.length;
            }
        };
    }
    
    public long getSize() {
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
	
	public void setAlternateLocationCollection(AlternateLocationCollection what) {
		_altlocCollection=what;
	}
}
