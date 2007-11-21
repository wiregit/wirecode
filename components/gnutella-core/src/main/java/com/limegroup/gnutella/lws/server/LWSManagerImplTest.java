package com.limegroup.gnutella.lws.server;

import java.io.IOException;
import java.util.HashMap;

import org.limewire.lws.server.LWSDispatcherFactoryImpl;
import org.limewire.lws.server.StringCallback;

import com.limegroup.gnutella.http.DefaultHttpExecutor;
import com.limegroup.gnutella.settings.LWSSettings;
import com.limegroup.gnutella.util.LimeTestCase;

public class LWSManagerImplTest extends LimeTestCase {

    public LWSManagerImplTest(String name) {
        super(name);
    }

    public void testSendingMessageToServerWithEmptyHost() throws IOException {
        // precondition: host should be empty
        assertEquals("", LWSSettings.LWS_AUTHENTICATION_HOSTNAME.getValue());
        
        LWSManagerImpl lwsManagerImpl = new LWSManagerImpl(new DefaultHttpExecutor(), new LWSDispatcherFactoryImpl());

        try {
            lwsManagerImpl.sendMessageToServer("blah", new HashMap<String, String>(), new StringCallback() {
                public void process(String response) {
                }
            });
        } catch (IllegalArgumentException iae) {
            fail("IllegalArgumentException should not have been thrown: " + iae);
        }
    }

}
