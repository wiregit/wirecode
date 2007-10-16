package org.limewire.lws.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.lws.server.DispatcherSupport;
import org.limewire.lws.server.LWSServerUtil;
import org.limewire.util.BaseTestCase;

/**
 * Provides the basis methods for doing communication. Subclasses should test
 * each aspect of this communication separately.
 */
abstract class AbstractCommunicationTest extends BaseTestCase {
    
    public final static int LOCAL_PORT  = LocalServerForTesting.PORT;
    public final static int REMOTE_PORT = RemoteServerForTesting.PORT;

    private LocalServerForTesting localServer;
    private RemoteServerForTesting remoteServer;
    private FakeJavascriptCodeInTheWebpage code;
    
    private Thread localThread;
    private Thread remoteThread;    

    protected AbstractCommunicationTest(String s) {
        super(s);
    }
    
    // -------------------------------------------------------
    // Access
    // -------------------------------------------------------
    
    protected final ServerImpl getLocalServer() {
        return this.localServer;
    }

    protected final AbstractRemoteServer getRemoteServer() {
        return this.remoteServer;
    }

    protected final FakeJavascriptCodeInTheWebpage getCode() {
        return this.code;
    }

    protected static final Map<String, String> NULL_ARGS = new HashMap<String,String>(); //Collections.emptyMap();
    
    protected static final Map<String, String> DUMMY_CALLBACK_ARGS;
    static {
        DUMMY_CALLBACK_ARGS = new HashMap<String,String>();
        DUMMY_CALLBACK_ARGS.put(DispatcherSupport.Parameters.CALLBACK, "dummy");        
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
     * Returns a handler that ensures that the response returned is
     * <tt>code</tt>.
     * 
     * @param want expected code
     * @return a handler that ensures that the response returned is
     *         <tt>code</tt>
     */
    protected final FakeJavascriptCodeInTheWebpage.Handler errorHandler(final String want) {
        return new FakeJavascriptCodeInTheWebpage.Handler() {
            public void handle(final String res) {
                //
                // We first have to remove the parens and single quotes
                // from around the message, because we always pass a
                // callback back to javascript
                //
                final String have = LWSServerUtil.unwrapError(LWSServerUtil.removeCallback(res));
                assertEquals(want, have);
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

        localServer = new LocalServerForTesting("localhost", REMOTE_PORT);
        remoteServer = new RemoteServerForTesting(LOCAL_PORT);
        localThread = AbstractServer.start(localServer);
        remoteThread = AbstractServer.start(remoteServer);
        code = new FakeJavascriptCodeInTheWebpage(localServer, remoteServer);

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
        args.put(DispatcherSupport.Parameters.PRIVATE, privateKey);
        return sendMessageFromWebpageToClient(DispatcherSupport.Commands.AUTHENTICATE, args);
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
        args.put(DispatcherSupport.Parameters.PUBLIC, publicKey);
        return sendMessageFromClientToRemoteServer(DispatcherSupport.Commands.GIVE_KEY, args);
    }

    protected final String getPublicKey() {
        return sendMessageFromWebpageToClient(DispatcherSupport.Commands.START_COM, NULL_ARGS);
    }

    // -----------------------------------------------------------
    // Private
    // -----------------------------------------------------------

    private String sendMessageFromWebpageToClient(final String cmd, final Map<String, String> args) {
        args.put(DispatcherSupport.Parameters.CALLBACK, "dummy");
        final String[] result = new String[1];
        getCode().sendLocalMsg(cmd, args, new FakeJavascriptCodeInTheWebpage.Handler() {
            public void handle(final String res) {
                result[0] = LWSServerUtil.removeCallback(res);
            }
        });
        return result[0];
    }

    private String sendMessageFromClientToRemoteServer(final String cmd, final Map<String, String> args) {
        args.put(DispatcherSupport.Parameters.CALLBACK, "dummy");
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
