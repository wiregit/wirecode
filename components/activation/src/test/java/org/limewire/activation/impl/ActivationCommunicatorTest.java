package org.limewire.activation.impl;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;

/**
 * Test for {@link ActivationCommunicatorImpl}
 * 
 * Some scenarios tested:
 * 
 * - server down, ioexception expected
 * - server up, invalid json data sent back, invaliddataexception expected
 * 
 */
public class ActivationCommunicatorTest extends LimeTestCase {
    
    public ActivationCommunicatorTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ActivationCommunicatorTest.class);
    }
}
