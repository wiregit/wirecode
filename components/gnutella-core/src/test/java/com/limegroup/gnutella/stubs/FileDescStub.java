package com.limegroup.gnutella.stubs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Set;

public class FileDescStub extends FileDesc {
    public static final String urn = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";
    public static final Set set;
    private static Set localSet2,globalSet;
    public Set localSet;
    public static final int size = 1126400;
    
    private AlternateLocationCollection _altlocCollection;
    
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
    
    public FileDescStub() {
        this("abc.txt");
    }
    
    public FileDescStub(String name) {
        super(new File(name), set, 0);
        localSet=new HashSet();
    }
    
    public FileDescStub(String name, URN urn, int index) {
    	super(new File(name), createUrnSet(urn),index);
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
	 * @see com.limegroup.gnutella.FileDesc#getAlternateLocationCollection()
	 */
	public AlternateLocationCollection getAlternateLocationCollection() {

		return _altlocCollection;
	}
	
	public void setAlternateLocationCollection(AlternateLocationCollection what) {
		_altlocCollection=what;
	}
}
