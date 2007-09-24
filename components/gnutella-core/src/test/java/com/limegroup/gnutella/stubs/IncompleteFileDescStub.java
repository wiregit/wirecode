
package com.limegroup.gnutella.stubs;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.URN;

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
    
    private IntervalSet.ByteIntervals _ranges;
    
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
        super(FileDescStub.createStubFile(new File(name)), set, 0,name,size,null);
        localSet=new HashSet();
    }
    
    public IncompleteFileDescStub(String name, URN urn, int index) {
    	super(FileDescStub.createStubFile(new File(name)), createUrnSet(urn),index,name,size,null);
    	localSet=localSet2;
    }
    
    private static Set createUrnSet(URN urn) {
    	localSet2 = new HashSet();
    	localSet2.add(urn);
    	globalSet.add(urn);
    	return localSet2;
    }
    
    @Override
    public boolean containsUrn(URN urn) {
    	if (globalSet.contains(urn))
    		return true;
    	else return super.containsUrn(urn);
    }
    
    @Override
    public long getFileSize() {
        return size;
    }
    
    @Override
    public URN getSHA1Urn() {
    	if (localSet.isEmpty())
    		return super.getSHA1Urn();
    	else return
			(URN)localSet.toArray()[0];
    }
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.IncompleteFileDesc#getRangesAsByte()
	 */
    @Override
	public IntervalSet.ByteIntervals getRangesAsByte() {
		return _ranges;
	}
	
	public void setRangesByte(IntervalSet.ByteIntervals what) {
		_ranges=what;
	}
    
    public void setRangesAsIntervals(Range... intervals) {
        IntervalSet set = new IntervalSet();
        for(Range intvl : intervals)
            set.add(intvl);
        _ranges = set.toBytes();
    }
	
}
