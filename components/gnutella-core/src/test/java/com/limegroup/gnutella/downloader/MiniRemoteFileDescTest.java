package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import junit.framework.*;

public class MiniRemoteFileDescTest extends TestCase {

    public MiniRemoteFileDescTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return new TestSuite(MiniRemoteFileDescTest.class);
    }

	public void testLegacy() {
        byte[] guid1 = GUID.makeGuid();
        byte[] guid2 = GUID.makeGuid();
        MiniRemoteFileDesc m1 = new MiniRemoteFileDesc("a.txt", 12, guid1);
        MiniRemoteFileDesc m2 = new MiniRemoteFileDesc("b.txt", 13, guid2);
        MiniRemoteFileDesc m3 = new MiniRemoteFileDesc("b.txt", 12, guid2);
        assertTrue("different MFDs equal",!m1.equals(m2));
        assertTrue("equals ignoring index",!m2.equals(m3));
        assertTrue("hashcode broken",m2.hashCode()== m3.hashCode());
        assertTrue("hashcode broken",m1.hashCode()!= m3.hashCode());
        m3 = new MiniRemoteFileDesc("a.txt",12,guid1);
        assertTrue("equals method broken",m1.equals(m3));
        HashMap map = new HashMap();
        Object o = new Object();
        map.put(m1,o);
        Object o1 = map.get(m3);
        assertTrue("equals or hashcode broken",o1==o);
    }
}
