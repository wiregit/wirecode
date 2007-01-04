package com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Random;

import org.limewire.security.QueryKey;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageVerifier;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.LimeWireUtils;

public class MiscTests extends LimeTestCase {
    
    public MiscTests(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MiscTests.class);
    }
    
    /** Makes sure all stuff we need to work works. */
    public void testSecureUpdateMessage() throws Exception {
        StaticMessages.initialize();
        QueryReply reply = StaticMessages.getUpdateReply();
        assertTrue(reply.hasSecureData());

        SecureMessageVerifier verifier =
            new SecureMessageVerifier(new File(LimeWireUtils.getUserSettingsDir(), "secureMessage.key"));
        StubSecureMessageCallback callback = new StubSecureMessageCallback();
        verifier.verify(reply, callback);
        callback.waitForReply();
        assertTrue(callback.getPassed());
        assertSame(reply, callback.getSecureMessage());
        assertEquals(SecureMessage.SECURE, reply.getSecureStatus());
    }

    // Makes sure QueryKeys have no problem going in and out of GGEP blocks
    public void testQueryKeysAndGGEP() throws Exception {
        Random rand = new Random();
        for (int i = 4; i < 17; i++) {
            byte[] qk = new byte[i];
            Arrays.sort(qk);
            // make sure the bytes have offensive characters....
            while ((Arrays.binarySearch(qk, (byte) 0x1c) < 0) ||
                   (Arrays.binarySearch(qk, (byte) 0x00) < 0)) {
                rand.nextBytes(qk);
                Arrays.sort(qk);
            }
            QueryKey queryKey = QueryKey.getQueryKey(qk, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            queryKey.write(baos);
            GGEP in = new GGEP(false);
            in.put(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT,
                   baos.toByteArray());
            baos = new ByteArrayOutputStream();
            in.write(baos);
            GGEP out = new GGEP(baos.toByteArray(), 0, null);
            QueryKey queryKey2 = 
            QueryKey.getQueryKey(out.getBytes(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT), false);
            assertEquals("qks not equal, i = " + i,
                       queryKey, queryKey2);
        }
    }

}
