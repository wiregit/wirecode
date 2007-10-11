package org.limewire.lws.server;

import java.util.HashMap;
import java.util.Map;

import org.limewire.lws.server.DispatcherSupport;
import org.limewire.lws.server.Util;

/**
 * Provides the basis methods for doing communication. Subclasses should test
 * each aspect of this communication separately.
 */
abstract class AbstractCommunicationTest extends DemoSupport {

    protected AbstractCommunicationTest(String s) {
        super(s);
    }

    // -------------------------------------------------------
    // Convenience
    // -------------------------------------------------------

    protected final String doEcho(final String privateKey, final String msg) {
        final Map<String, String> args = new HashMap<String, String>();
        args.put(DispatcherSupport.Parameters.PRIVATE, privateKey);
        args.put(DispatcherSupport.Parameters.MSG, msg);
        return sendLocalMsg(DispatcherSupport.Commands.ECHO, args);
    }

    protected final String doAuthenticate() {
        return doAuthenticate(getPrivateKey());
    }

    protected final String doAuthenticate(final String privateKey) {
        final Map<String, String> args = new HashMap<String, String>();
        args.put(DispatcherSupport.Parameters.PRIVATE, privateKey);
        return sendLocalMsg(DispatcherSupport.Commands.AUTHENTICATE, args);
    }

    protected final String getPrivateKey() {
        final String publicKey = getPublicKey();
        System.out.println("have public key: " + publicKey);
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
        final Map<String, String> args = new HashMap<String, String>();
        args.put(DispatcherSupport.Parameters.PUBLIC, publicKey);
        return sendRemoteMsg(DispatcherSupport.Commands.GIVE_KEY, args);
    }

    protected final String getPublicKey() {
        final Map<String, String> args = new HashMap<String, String>();
        return sendLocalMsg(DispatcherSupport.Commands.START_COM, args);
    }

    // -----------------------------------------------------------
    // Private
    // -----------------------------------------------------------

    private String sendLocalMsg(final String cmd, final Map<String, String> args) {
        args.put(DispatcherSupport.Parameters.CALLBACK, "dummy");
        final String[] result = new String[1];
        final Object lock = new Object();
        final boolean[] shouldWait = new boolean[] { true };
        getCode().sendLocalMsg(cmd, args, new FakeCode.Handler() {
            public void handle(final String res) {
                final String msg = Util.removeCallback(res);
                result[0] = msg;
                synchronized (lock) {
                    lock.notify();
                    shouldWait[0] = false;
                }
            }
        });
        if (shouldWait[0]) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return result[0];
    }

    private String sendRemoteMsg(final String cmd,
            final Map<String, String> args) {
        args.put(DispatcherSupport.Parameters.CALLBACK, "dummy");
        final String[] result = new String[1];
        final Object lock = new Object();
        final boolean[] shouldWait = new boolean[] { true };
        getCode().sendRemoteMsg(cmd, args, new FakeCode.Handler() {
            public void handle(final String res) {
                final String msg = Util.removeCallback(res);
                result[0] = msg;
                synchronized (lock) {
                    lock.notify();
                    shouldWait[0] = false;
                }
            }
        });
        if (shouldWait[0]) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return result[0];
    }
}
