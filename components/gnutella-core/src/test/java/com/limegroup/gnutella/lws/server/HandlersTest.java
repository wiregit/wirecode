package com.limegroup.gnutella.lws.server;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.limewire.lws.server.LWSServerUtil;
import org.limewire.lws.server.LWSDispatcherSupport.Commands;
import org.limewire.lws.server.LWSDispatcherSupport.Responses;

/**
 * Makes sure
 * {@link LWSManager#registerListener(String, com.limegroup.gnutella.lws.server.LWSManager.Listener)}
 * is working.
 */
public class HandlersTest extends AbstractCommunicationSupportWithNoLocalServer {

    public HandlersTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(HandlersTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testAddTwoHandlers() {

        final boolean handled[] = { false, false };

        // Add the handlers
        String cmd1 = "Foo1";
        LWSManager.AbstractHandler lis1 = new LWSManager.AbstractHandler(cmd1) {
            public String handle(Map<String, String> args) {
                assertEquals("a1 should map to A1", args.get("a1"), "A1");
                assertEquals("b1 should map to B1", args.get("b1"), "B1");
                assertEquals("c1 should map to C1", args.get("c1"), "C1");
                handled[0] = true;
                return Responses.OK;
            }
        };
        assertTrue(getLWSManager().registerHandler(cmd1, lis1));

        String cmd2 = "Foo2";
        LWSManager.AbstractHandler lis2 = new LWSManager.AbstractHandler(cmd2) {
            public String handle(Map<String, String> args) {
                assertEquals("a2 should map to A2", args.get("a2"), "A2");
                assertEquals("b2 should map to B2", args.get("b2"), "B2");
                assertEquals("c2 should map to C2", args.get("c2"), "C2");
                handled[1] = true;
                return Responses.OK;
            }
        };
        assertTrue(getLWSManager().registerHandler(cmd2, lis2));

        // Send a message

        String response1, response2;

        Map<String, String> args1 = new HashMap<String, String>();
        args1.put("a1", "A1");
        args1.put("b1", "B1");
        args1.put("c1", "C1");

        response1 = sendCommandToClient(cmd1, args1, true);
        note("Got response1 '" + response1 + "'");
        assertEquals(Responses.OK, LWSServerUtil.removeCallback(response1));
        assertTrue(handled[0]);

        Map<String, String> args2 = new HashMap<String, String>();
        args2.put("a2", "A2");
        args2.put("b2", "B2");
        args2.put("c2", "C2");

        response2 = sendCommandToClient(cmd2, args2, true);
        assertEquals(Responses.OK, LWSServerUtil.removeCallback(response2));
        assertTrue(handled[1]);

        assertTrue(getLWSManager().unregisterHandler(cmd1));
        assertTrue(getLWSManager().unregisterHandler(cmd2));

        handled[0] = false;
        response1 = sendCommandToClient(Commands.MSG, args1, true);
        assertEquals("", LWSServerUtil.removeCallback(response1));
        assertFalse(handled[0]);

        handled[1] = false;
        response2 = sendCommandToClient(Commands.MSG, args2, true);
        assertEquals("", LWSServerUtil.removeCallback(response2));
        assertFalse(handled[0]);
    }

    /**
     * This should fail because you can't register to handlers for the same
     * command, since each has to return a result.
     */
    public void testWithTowHandlersAndSameName() {

        final boolean handled[] = { false, false };

        // Add the handlers
        String cmd1 = "Foo1";
        LWSManager.AbstractHandler lis1 = new LWSManager.AbstractHandler(cmd1) {
            public String handle(Map<String, String> args) {
                assertEquals("a1 should map to A1", args.get("a1"), "A1");
                assertEquals("b1 should map to B1", args.get("b1"), "B1");
                assertEquals("c1 should map to C1", args.get("c1"), "C1");
                handled[0] = true;
                return Responses.OK;
            }
        };
        assertTrue(getLWSManager().registerHandler(cmd1, lis1));

        String cmd2 = "Foo1";
        LWSManager.AbstractHandler lis2 = new LWSManager.AbstractHandler(cmd2) {
            public String handle(Map<String, String> args) {
                assertEquals("a2 should map to A2", args.get("a2"), "A2");
                assertEquals("b2 should map to B2", args.get("b2"), "B2");
                assertEquals("c2 should map to C2", args.get("c2"), "C2");
                handled[1] = true;
                return Responses.OK;
            }
        };
        assertTrue(getLWSManager().registerHandler(cmd2, lis2));

    }

    public void testAddThreeHandlers() {

        final boolean[] handled = { false, false, false };

        // Add the handlers
        String cmd1 = "Foo1";
        LWSManager.AbstractHandler lis1 = new LWSManager.AbstractHandler(cmd1) {
            public String handle(Map<String, String> args) {
                assertEquals("a1 should map to A1", args.get("a1"), "A1");
                assertEquals("b1 should map to B1", args.get("b1"), "B1");
                assertEquals("c1 should map to C1", args.get("c1"), "C1");
                handled[0] = true;
                return Responses.OK;
            }
        };
        assertTrue(getLWSManager().registerHandler(cmd1, lis1));

        String cmd2 = "Foo2";
        LWSManager.AbstractHandler lis2 = new LWSManager.AbstractHandler(cmd2) {
            public String handle(Map<String, String> args) {
                assertEquals("a2 should map to A2", args.get("a2"), "A2");
                assertEquals("b2 should map to B2", args.get("b2"), "B2");
                assertEquals("c2 should map to C2", args.get("c2"), "C2");
                handled[1] = true;
                return Responses.OK;
            }
        };
        assertTrue(getLWSManager().registerHandler(cmd2, lis2));

        String cmd3 = "Foo3";
        LWSManager.AbstractHandler lis3 = new LWSManager.AbstractHandler(cmd3) {
            public String handle(Map<String, String> args) {
                assertEquals("a3 should map to A3", args.get("a3"), "A3");
                assertEquals("b3 should map to B3", args.get("b3"), "B3");
                assertEquals("c3 should map to C3", args.get("c3"), "C3");
                handled[2] = true;
                return Responses.OK;
            }
        };
        assertTrue(getLWSManager().registerHandler(cmd3, lis3));

        // Send a message


        String response1, response2, response3;

        Map<String, String> args1 = new HashMap<String, String>();
        args1.put("a1", "A1");
        args1.put("b1", "B1");
        args1.put("c1", "C1");

        response1 = sendCommandToClient(cmd1, args1, true);
        note("Got response1: '" + response1 + "'");
        assertEquals(Responses.OK, LWSServerUtil.removeCallback(response1));
        assertTrue(handled[0]);

        Map<String, String> args2 = new HashMap<String, String>();
        args2.put("a2", "A2");
        args2.put("b2", "B2");
        args2.put("c2", "C2");

        response2 = sendCommandToClient(cmd2, args2, true);
        note("Got response2: '" + response2 + "'");
        assertEquals(Responses.OK, LWSServerUtil.removeCallback(response2));
        assertTrue(handled[1]);

        Map<String, String> args3 = new HashMap<String, String>();
        args3.put("a3", "A3");
        args3.put("b3", "B3");
        args3.put("c3", "C3");

        response3 = sendCommandToClient(cmd3, args3, true);
        note("Got response3: '" + response3 + "'");
        assertEquals(Responses.OK, LWSServerUtil.removeCallback(response3));
        assertTrue(handled[2]);

        assertTrue(getLWSManager().unregisterHandler(cmd1));
        assertTrue(getLWSManager().unregisterHandler(cmd2));
        assertTrue(getLWSManager().unregisterHandler(cmd3));

        handled[0] = false;
        response1 = sendCommandToClient(Commands.MSG, args1, true);
        assertEquals("", LWSServerUtil.removeCallback(response1));
        assertFalse(handled[0]);

        handled[1] = false;
        response2 = sendCommandToClient(Commands.MSG, args2, true);
        assertEquals("", LWSServerUtil.removeCallback(response2));
        assertFalse(handled[0]);

        handled[2] = false;
        response3 = sendCommandToClient(Commands.MSG, args3, true);
        assertEquals("", LWSServerUtil.removeCallback(response3));
        assertFalse(handled[2]);
    }

}
