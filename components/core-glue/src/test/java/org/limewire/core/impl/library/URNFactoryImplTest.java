package org.limewire.core.impl.library;

import java.io.IOException;

import org.limewire.core.api.library.URNFactory;
import org.limewire.util.BaseTestCase;

/**
 * JUnit test case for URNFactory.
 */
public class URNFactoryImplTest extends BaseTestCase {
    URNFactory urnFactory;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        urnFactory = new URNFactoryImpl();
    }
    
    @Override
    protected void tearDown() throws Exception {
        urnFactory = null;
    }
    
    protected void assertBadSha1ThrowsException(String badSha1) {
        try {
            urnFactory.createSHA1Urn(badSha1);
            fail("Should have thrown IOException.");
        } catch (IOException ex) {}
    }
    
    public void testCreatingSha1UrnWithBadSha1String() throws Exception {
        assertBadSha1ThrowsException("h4X0rZ");
        assertBadSha1ThrowsException("urn:sha1:h4X0rZ");
    }
}
