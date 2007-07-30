package com.limegroup.gnutella;


import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.downloader.VerifyingFileFactory;
import com.limegroup.gnutella.util.LimeTestCase;

@SuppressWarnings("unchecked")
public class IncompleteFileDescTest extends LimeTestCase {
    
    private IncompleteFileDesc ifd;
    private String fileName = "ifd.txt";
    private URN urn;
    private Set urns;
    private VerifyingFile vf  = VerifyingFileFactory.getSharedFactory().createVerifyingFile(0);

    public IncompleteFileDescTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(IncompleteFileDescTest.class);
    }
    
    public void setUp() throws Exception {
        urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        urns = new HashSet(1);
        urns.add(urn);
        
        ifd = new IncompleteFileDesc(
            new File(fileName),
            urns,
            0,
            fileName,
            1981,
            vf);
    }
    
    public void testGetAvailableRanges() throws Exception {
        // no ranges now.
        assertEquals("bytes", ifd.getAvailableRanges());
        
        // add a small range and ensure it doesn't get listed.
        Range small = Range.createRange(0);
        addInterval(small);
        assertEquals(ifd.getAvailableRanges(),
            "bytes", ifd.getAvailableRanges());
        
        Range notLargeEnough = Range.createRange(0, 102398);
        addInterval(notLargeEnough);
        assertEquals(ifd.getAvailableRanges(),
            "bytes", ifd.getAvailableRanges());
        
        // extend from the middle ...
        Range extended = Range.createRange(102300, 102500);
        addInterval(extended);
        assertEquals(ifd.getAvailableRanges(),
            "bytes 0-102499", ifd.getAvailableRanges());
        
        // add one not connected ...
        Range other = Range.createRange(102550, 204950);
        addInterval(other);
        assertEquals(ifd.getAvailableRanges(),
            "bytes 0-102499, 102550-204949", ifd.getAvailableRanges());
    }
    
    public void testisRangeSatisfiable() throws Exception {
        // no ranges.
        
        
        



           assertFalse( ifd.isRangeSatisfiable(0, 0) );   
        assertFalse( ifd.isRangeSatisfiable(0, 150) );
        
        // add a range.
        Range small = Range.createRange(0);
        addInterval(small);
        assertTrue( ifd.isRangeSatisfiable(0, 0) );
        
        Range medium = Range.createRange(0, 102399);
        addInterval(medium);
        assertTrue( ifd.isRangeSatisfiable(0, 102399) );
        assertTrue( ifd.isRangeSatisfiable(50,100000) );
        assertFalse( ifd.isRangeSatisfiable(0, 102401) );
        assertFalse( ifd.isRangeSatisfiable( 102399, 102401) );
        assertFalse( ifd.isRangeSatisfiable(102400, 102500) );
        
        // extend from the middle ...
        Range extended = Range.createRange(102300, 102500);
        addInterval(extended);
        assertTrue( ifd.isRangeSatisfiable(0, 102500) );
        assertFalse( ifd.isRangeSatisfiable(1,102501) );
        
        // add one not connected ...
        Range other = Range.createRange(102550, 204950);
        addInterval(other);
        assertTrue( ifd.isRangeSatisfiable( 102550, 204950) );
        assertFalse( ifd.isRangeSatisfiable(102399, 102550) );
    }
    
    private void addInterval(Range i) throws Exception {
        IntervalSet set = (IntervalSet)
            PrivilegedAccessor.getValue(vf,"verifiedBlocks");
        
        set.add(i);
    }
}
        