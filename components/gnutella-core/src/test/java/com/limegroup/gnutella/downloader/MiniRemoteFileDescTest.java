package com.limegroup.gnutella.downloader;

import java.util.HashMap;

import junit.framework.Test;

import com.limegroup.gnutella.GUID;

public class MiniRemoteFileDescTest extends com.limegroup.gnutella.util.BaseTestCase {

    public MiniRemoteFileDescTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(MiniRemoteFileDescTest.class);
    }

	public void testLegacy() {
        byte[] guid1 = GUID.makeGuid();
        byte[] guid2 = GUID.makeGuid();
        MiniRemoteFileDesc m1 = new MiniRemoteFileDesc("a.txt", 12, guid1);
        MiniRemoteFileDesc m2 = new MiniRemoteFileDesc("b.txt", 13, guid2);
        MiniRemoteFileDesc m3 = new MiniRemoteFileDesc("b.txt", 12, guid2);
        assertTrue("different MFDs equal",!m1.equals(m2));
        assertTrue("equals looking at index",m2.equals(m3));
        assertTrue("hashcode broken",m2.hashCode()== m3.hashCode());
        assertTrue("hashcode broken",m1.hashCode()!= m3.hashCode());
        m3 = new MiniRemoteFileDesc("a.txt",12,guid1);
        assertEquals("equals method broken",m1, m3);
        assertEquals("equals method broken", m3, m1);
        HashMap map = new HashMap();
        Object o = new Object();
        map.put(m1,o);
        Object o1 = map.get(m3);
        assertSame("equals or hashcode broken",o1,o);
    }
}
