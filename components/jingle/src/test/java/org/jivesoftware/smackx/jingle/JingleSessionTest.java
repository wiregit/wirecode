package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.test.SmackTestCase;

public class JingleSessionTest extends SmackTestCase {

    public JingleSessionTest(final String name) {
        super(name);
    }

    public void testEqualsObject() throws XMPPException {
        JingleSession js1 = new OutgoingJingleSession(getConnection(0), "res1", null); // TODO add content?
        JingleSession js2 = new OutgoingJingleSession(getConnection(1), "res1", null); // TODO add content?
        JingleSession js3 = new OutgoingJingleSession(getConnection(2), "res2", null); // TODO add content?

        System.out.println(js1.getSid());
        System.out.println(js2.getSid());

        js1.setInitiator("js1");
        js2.setInitiator("js1");
        js1.setSid("10");
        js2.setSid("10");

        assertEquals(js1, js2);
        assertEquals(js2, js1);

        assertFalse(js1.equals(js3));
    }

    public void testGetInstanceFor() throws XMPPException {
//        String ini1 = "initiator1";
        String sid1 = "sid1";
//        String ini2 = "initiator2";
        String sid2 = "sid2";

        JingleSession js1 = new OutgoingJingleSession(getConnection(0), sid1, null);  // TODO add content?
        JingleSession js2 = new OutgoingJingleSession(getConnection(1), sid2, null);  // TODO add content?

        // For a packet, we should be able to get a session that handles that...
        assertNotNull(JingleSession.getInstanceFor(getConnection(0)));
        assertNotNull(JingleSession.getInstanceFor(getConnection(1)));

        assertEquals(JingleSession.getInstanceFor(getConnection(0)), js1);
        assertEquals(JingleSession.getInstanceFor(getConnection(1)), js2);
    }

    protected int getMaxConnections() {
        return 3;
    }
}
