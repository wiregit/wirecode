package com.limegroup.gnutella.version;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;

public class UpdateMessageVerifierImplTest extends BaseTestCase {
    
    public UpdateMessageVerifierImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(UpdateMessageVerifierImplTest.class);
    }

    public void testDefaultBindings() {
        Injector injector = LimeTestUtils.createInjector();
        assertEquals(UpdateMessageVerifierImpl.class, injector.getInstance(UpdateMessageVerifier.class).getClass());
    }
    
    private final String SIGNED_ASDF = "GAWAEFDW7Q73ILI2N5FSNPS7ASVYZ646BFYLZLQCCQ6G3VLJD4EE7KNGHVUDPLCALWTH2R4BLQ||asdf\n";
    
    public void testMessageVerifies() {
        Injector injector = LimeTestUtils.createInjector();
        UpdateMessageVerifier updateMessageVerifier = injector.getInstance(UpdateMessageVerifier.class);
        String data = updateMessageVerifier.getVerifiedData(SIGNED_ASDF.getBytes());
        assertEquals("asdf\n", data);
    }

}
