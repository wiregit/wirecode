package com.limegroup.gnutella.lws.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LWSServerUtil;
import org.limewire.net.SocketsManager;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.settings.LWSSettings;
import com.limegroup.gnutella.util.LimeTestCase;


/**
 * This is a simpler class than {@link AbstractCommunicationSupport} in the
 * <code>lwsserver</code> component, because it doesn't have a mock local
 * server representing that on the client. Instead it's an integration test so
 * only has a mock remote server and mock web page javascript code to send the
 * client commands.
 * 
 * <p>
 * 
 * There should be only one method for each class, or else multiple instances of
 * the remote server are created, and we need to share that among everything.
 */
abstract class AbstractCommunicationSupportWithNoLocalServer extends LimeTestCase {
    
    private final static org.apache.commons.logging.Log LOG 
        = LogFactory.getLog(AbstractCommunicationSupportWithNoLocalServer.class);
    
    public final static int LOCAL_PORT  = LocalServerImpl.PORT;
    public final static int REMOTE_PORT = RemoteServerImpl.PORT;
    
    /**
     * The number of times we'll try for a private key. Because we can't respond
     * in the client on the same thread we received a request, we hand back the
     * web page code the public key before giving it to the server. So it may
     * have not arrived yet.
     */
    private final static int TIMES_TO_TRY_FOR_PRIVATE_KEY = 3;
    
    /**
     * Time to sleep between tries for the private key if we haven't gotten it
     * yet.
     */
    private final static int SLEEP_TIME_BETWEEN_PRIVATE_KEY_TRIES = 300;

    private RemoteServerImpl remoteServer;
    private Thread remoteThread;
    
    private CommandSender sender;
    private static LWSManager lwsManager;
    private LifecycleManager lifecycleManager;
    
    private String privateKey;
    
        

    public AbstractCommunicationSupportWithNoLocalServer(String s) {
        super(s);
    }
    
    // -------------------------------------------------------
    // Access
    // -------------------------------------------------------

    protected final RemoteServerImpl getRemoteServer() {
        return this.remoteServer;
    }
    
    protected final CommandSender getCommandSender() {
        return this.sender;
    }
    
    protected final LWSManager getLWSManager() {
        return lwsManager;
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

    protected final Thread getRemoteThread() {
        return remoteThread;
    }

    @Override
    protected final void setUp() throws Exception {
        
        note("begin setUp");

        beforeSetup();
        
        LWSSettings.LWS_AUTHENTICATION_HOSTNAME.setValue("localhost");
        LWSSettings.LWS_AUTHENTICATION_PORT.setValue(8080);
        
        Injector inj = LimeTestUtils.createInjector(new AbstractModule() {
            protected void configure() {
                requestStaticInjection(LWSManager.class);
                requestStaticInjection(SocketsManager.class);
            }
        });
        //
        if (remoteServer == null) {
            remoteServer = new RemoteServerImpl(inj.getInstance(SocketsManager.class), LOCAL_PORT);
        }
        lifecycleManager = inj.getInstance(LifecycleManager.class);
        lifecycleManager.start();
        remoteThread = remoteServer.start();
        //
        // This should persist over tests, because you can't register/unregister/register
        // handlers for the local http acceptor
        //
        if (lwsManager == null) lwsManager = inj.getInstance(LWSManager.class);
        sender = new CommandSender();

        afterSetup();
        
        note("end setUp");
    }

    @Override
    protected final void tearDown() throws Exception {
        
        note("begin tearDown");

        beforeTearDown();

        stop(remoteServer);
        
        if (lifecycleManager != null) lifecycleManager.shutdown();
        
        privateKey = null;
        remoteThread = null;
        
        doDetatch();

        afterTearDown();
        
        note("end tearDown");
    }    
    
    protected final void doDetatch() {
        getCommandSender().detach();
        lwsManager.clearHandlersAndListeners();
    }

    protected final String doAuthenticate() {
        note("Authenticating");
        return doAuthenticate(getPrivateKey());
    }

    protected final void note(Object str) {
        LOG.debug(str);
    }

    protected final String doAuthenticate(final String privateKey) {
        Map<String, String> args = new HashMap<String, String>();
        args.put("private", privateKey);
        return sendMessageFromWebpageToClient("authenticate", args);        
    }

    protected final String getPrivateKey() {
        note("Getting private key");
        if (privateKey == null) {
            note("Getting public key");
            String publicKey = getPublicKey();
            note("Got the public key: " + publicKey);
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                //
                // Not crucial, but would like to see the error
                //
                e.printStackTrace();
            }
            String ip = "127.0.0.1";
            //
            // We'll try this TIMES_TO_TRY_FOR_PRIVATE_KEY times
            // See the comment at TIMES_TO_TRY_FOR_PRIVATE_KEY for why.
            //
            for (int i=0; i<TIMES_TO_TRY_FOR_PRIVATE_KEY; i++) {
                privateKey = remoteServer.lookupPrivateKey(publicKey, ip);
                if (LWSServerUtil.isValidPrivateKey(privateKey)) break;
                try {
                    Thread.sleep(SLEEP_TIME_BETWEEN_PRIVATE_KEY_TRIES);
                } catch (InterruptedException e) {
                    // ignore
                }
            }            
            
        }
        note("Got the private key: " + privateKey);
        return privateKey;
    }
   

    protected final String getPublicKey() {
        String publicKey = sendMessageFromWebpageToClient(LWSDispatcherSupport.Commands.START_COM, NULL_ARGS);
        return publicKey;
    }
    
    protected final String sendMessageFromWebpageToClient(final String cmd, final Map<String, String> args) {
        args.put(LWSDispatcherSupport.Parameters.CALLBACK, "dummy");
        String responseWithCallback = sender.sendMessage(cmd, args);
        return LWSServerUtil.removeCallback(responseWithCallback);
    }


    // -----------------------------------------------------------
    // Private
    // -----------------------------------------------------------
    
    private void stop(final AbstractServer t) {
        if (t != null) {
          t.shutDown();
        }
    }    
}
