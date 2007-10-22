package org.limewire.lws.server;

import java.util.HashMap;
import java.util.Map;

import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LWSServerUtil;
import org.limewire.util.BaseTestCase;

/**
 * Provides the basis methods for doing communication. Subclasses should test
 * each aspect of this communication separately.
 */
abstract class AbstractCommunicationTest extends BaseTestCase {
    
    public final static int LOCAL_PORT  = LocalServerImpl.PORT;
    public final static int REMOTE_PORT = RemoteServerImpl.PORT;

    private LocalServerImpl localServer;
    private RemoteServerImpl remoteServer;
    private FakeJavascriptCodeInTheWebpage code;
    
    private Thread localThread;
    private Thread remoteThread;    

    public AbstractCommunicationTest(String s) {
        super(s);
    }
    
    // -------------------------------------------------------
    // Access
    // -------------------------------------------------------
    
    protected final LocalServerImpl getLocalServer() {
        return this.localServer;
    }

    protected final RemoteServerImpl getRemoteServer() {
        return this.remoteServer;
    }

    protected final FakeJavascriptCodeInTheWebpage getCode() {
        return this.code;
    }

    /**
     * This is not a {@link Collections#emptyMap()} because we may want to add
     * to it.
     */
    protected static final Map<String, String> NULL_ARGS = new HashMap<String,String>();
    
    protected static final Map<String, String> DUMMY_CALLBACK_ARGS;
    static {
        DUMMY_CALLBACK_ARGS = new HashMap<String,String>();
        DUMMY_CALLBACK_ARGS.put(LWSDispatcherSupport.Parameters.CALLBACK, "dummy");        
    }


    /** Override with functionality <b>after</b> {@link #setUp()}. */
    protected void beforeSetup() { }

    /** Override with functionality <b>after</b> {@link #setUp()}. */
    protected void afterSetup() { }

    /** Override with functionality <b>after</b> {@link #tearDown()}. */
    protected void beforeTearDown() { }

    /** Override with functionality <b>after</b> {@link #tearDown()}. */
    protected void afterTearDown() { }

    /**
     * Returns a handler that ensures that the response returned is one of
     * the <tt>code</tt>s.
     * 
     * @param wants expected codes
     * @return a handler that ensures that the response returned is
     *         on of the <tt>code</tt>
     */
    protected final FakeJavascriptCodeInTheWebpage.Handler errorHandler(final String want) {
        return errorHandler(new String[]{want});
    }
    
    protected final FakeJavascriptCodeInTheWebpage.Handler errorHandlerAny() {
        return new FakeJavascriptCodeInTheWebpage.Handler() {
            public void handle(final String res) {
                final String have = LWSServerUtil.unwrapError(LWSServerUtil.removeCallback(res));
                assertTrue(have.indexOf('.') != -1);
            }
        };
    }
    
    protected final FakeJavascriptCodeInTheWebpage.Handler errorHandler(final String[] wants) {
        return new FakeJavascriptCodeInTheWebpage.Handler() {
            public void handle(final String res) {
                //
                // We first have to remove the parens and single quotes
                // from around the message, because we always pass a
                // callback back to javascript
                //
                final String have = LWSServerUtil.unwrapError(LWSServerUtil.removeCallback(res));
                //
                // Make sure it's *one* of the expected results
                //
                boolean haveIt = false;
                for (String want: wants) {
                    if (have.equals(want)) {
                        haveIt = true;
                        break;
                    }
                }
                if (!haveIt) {
                    fail("didn't received one message of: " + unwrap(wants));
                }
            }
        };
    }

    protected final Thread getLocalThread() {
        return localThread;
    }

    protected final Thread getRemoteThread() {
        return remoteThread;
    }

    @Override
    protected final void setUp() throws Exception {

        beforeSetup();

        localServer     = new LocalServerImpl("localhost", REMOTE_PORT);
        remoteServer    = new RemoteServerImpl(LOCAL_PORT);
        localThread     = localServer.start();
        remoteThread    = remoteServer.start();
        code            = new FakeJavascriptCodeInTheWebpage(localServer, remoteServer);

        afterSetup();
    }

    @Override
    protected final void tearDown() throws Exception {

        beforeTearDown();

        stop(localServer);
        stop(remoteServer);

        localThread = null;
        remoteThread = null;

        afterTearDown();
    }    

    // -------------------------------------------------------
    // Convenience
    // -------------------------------------------------------

    protected final String doAuthenticate() {
        return doAuthenticate(getPrivateKey());
    }

    protected final String doAuthenticate(final String privateKey) {
        Map<String, String> args = new HashMap<String, String>();
        args.put(LWSDispatcherSupport.Parameters.PRIVATE, privateKey);
        return sendMessageFromWebpageToClient(LWSDispatcherSupport.Commands.AUTHENTICATE, args);
    }

    protected final String getPrivateKey() {
        String publicKey = getPublicKey(); 
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            //
            // Not crucial, but would like to see the error
            //
            e.printStackTrace();
        }
        Map<String, String> args = new HashMap<String, String>();
        args.put(LWSDispatcherSupport.Parameters.PUBLIC, publicKey);
        return sendMessageFromClientToRemoteServer(LWSDispatcherSupport.Commands.GIVE_KEY, args);
    }

    protected final String getPublicKey() {
        return sendMessageFromWebpageToClient(LWSDispatcherSupport.Commands.START_COM, NULL_ARGS);
    }

    // -----------------------------------------------------------
    // Private
    // -----------------------------------------------------------
    
    private String unwrap(String[] ss) {
        StringBuffer sb = new StringBuffer("{ ");
        if (ss != null) {
            for (String s : ss) {
                sb.append(String.valueOf(s));
                sb.append(" ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String sendMessageFromWebpageToClient(final String cmd, final Map<String, String> args) {
        args.put(LWSDispatcherSupport.Parameters.CALLBACK, "dummy");
        final String[] result = new String[1];
        getCode().sendLocalMsg(cmd, args, new FakeJavascriptCodeInTheWebpage.Handler() {
            public void handle(final String res) {
                result[0] = LWSServerUtil.removeCallback(res);
            }
        });
        return result[0];
    }

    private String sendMessageFromClientToRemoteServer(final String cmd, final Map<String, String> args) {
        args.put(LWSDispatcherSupport.Parameters.CALLBACK, "dummy");
        final String[] result = new String[1];
        getCode().sendRemoteMsg(cmd, args, new FakeJavascriptCodeInTheWebpage.Handler() {
            public void handle(final String res) {
                result[0] = LWSServerUtil.removeCallback(res);
            }
        });
        return result[0];
    }
    
    private void stop(final AbstractServer t) {
        if (t != null) {
          t.shutDown();
        }
    }    
}
