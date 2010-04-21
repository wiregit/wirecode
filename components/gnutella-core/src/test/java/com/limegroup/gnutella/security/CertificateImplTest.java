package com.limegroup.gnutella.security;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class CertificateImplTest extends BaseTestCase {


    public static Test suite() {
        return buildTestSuite(CertificateImplTest.class);
    }
    
    public void testHashCode() {
        assertEquals(new CertificateImpl(null, null, 1, null, "").hashCode(), new CertificateImpl(null, null, 1, null, "").hashCode());
        assertEquals(new CertificateImpl(null, null, 1, null, "ABC|1|DEF").hashCode(), new CertificateImpl(null, null, 1, null, "ABC|1|DEF").hashCode());
        assertNotEquals(new CertificateImpl(null, null, 1, null, "ABC|1|DEF").hashCode(), new CertificateImpl(null, null, 1, null, "ABC|1|DEG").hashCode());
    }

    public void testEquals() {
        assertEquals(new CertificateImpl(null, null, 1, null, ""), new CertificateImpl(null, null, 1, null, ""));
        assertEquals(new CertificateImpl(null, null, 1, null, "ABC|1|DEF"), new CertificateImpl(null, null, 1, null, "ABC|1|DEF"));
        assertNotEquals(new CertificateImpl(null, null, 1, null, "ABC|1|DEF"), new CertificateImpl(null, null, 1, null, "ABC|1|DEG"));
    }

}
