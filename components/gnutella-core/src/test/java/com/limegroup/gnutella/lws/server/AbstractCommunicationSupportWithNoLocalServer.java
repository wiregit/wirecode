package com.limegroup.gnutella.lws.server;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.api.network.NetworkManager;
import org.limewire.core.settings.LWSSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.lws.server.FakeJavascriptCodeInTheWebpage;
import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LWSServerUtil;
import org.limewire.lws.server.LocalServerImpl;
import org.limewire.lws.server.RemoteServerImpl;
import org.limewire.lws.server.TestNetworkManagerImpl;
import org.limewire.net.SocketsManager;
import org.limewire.util.Base32;
import org.limewire.util.StringUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.LifecycleManager;


/**
 * This is a simpler class than {@link org.limewire.lws.server.AbstractCommunicationSupport} in the
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
    
    protected final Log LOG = LogFactory.getLog(getClass());
    
    public final int LOCAL_PORT  = LocalServerImpl.PORT;
    public final int REMOTE_PORT = RemoteServerImpl.PORT;
    
    protected final Map<String,String> EMPTY_ARGS = new HashMap<String,String>();
    
    private RemoteServerImpl remoteServer;
    private Thread remoteThread;
    
    private CommandSender sender;
    private LWSManager lwsManager;
    private LifecycleManager lifecycleManager;
    
    private KeyPair keyPair; 
    
        

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
    protected final Map<String, String> NULL_ARGS = new HashMap<String,String>();
    
    protected final Map<String, String> DUMMY_CALLBACK_ARGS;
    
    {
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
    
    private Injector inj;    

    @Override
    protected final void setUp() throws Exception {
        
        note("begin setUp");

        beforeSetup();
        
        // generate private-public key pair
        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance("DSA");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            random.setSeed(System.currentTimeMillis());
            keyGen.initialize(1024, random);
            keyPair = keyGen.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
        }

        PublicKey publicKey = keyPair.getPublic();
        LWSSettings.LWS_PUBLIC_KEY.set(Base32.encode(publicKey.getEncoded()));
        
        inj = LimeTestUtils.createInjector(TestUtils.bind(NetworkManager.class).toInstances(new TestNetworkManagerImpl()));
        remoteServer = new RemoteServerImpl(inj.getInstance(SocketsManager.class), LOCAL_PORT, keyPair);
        lifecycleManager = inj.getInstance(LifecycleManager.class);
        lifecycleManager.start();
        remoteThread = remoteServer.start();
        lwsManager = getInstance(LWSManager.class);
        sender = new CommandSender();

        afterSetup();
        
        note("end setUp");
    }

    @Override
    protected final void tearDown() throws Exception {
        
        note("begin tearDown");

        beforeTearDown();
        
        lwsManager.clearHandlers();

        remoteServer.shutDown();
        
        lifecycleManager.shutdown();
                
        keyPair = null;
        remoteThread = null;
       
        afterTearDown();
        
        note("end tearDown");
    }    
    

    protected final void note(Object str) {
        LOG.debug(str);
    }

    
    protected final String sendPing() {
        Map<String, String> args = new HashMap<String, String>();
        return sendMessageFromWebpageToClient("Ping", args);          
    }
    
    /**
     * Returns the response after calling
     * {@link #sendMessageFromWebpageToClient(String, Map)}
     * <p>
     * 
     * @param cmd Command to send
     * @param args arguments
     * @param includeDummyCallback <code>true</code> to include the callback
     *        <code>dummy</code>. This is used as a convenience in testing
     *        handlers when we don't need to have an explicit callback function.
     * @return the response after calling
     *         {@link #sendMessageFromWebpageToClient(String, Map)}
     */
    protected final String sendCommandToClient(String cmd, Map<String, String> args,
            boolean includeDummyCallback) {
        if (includeDummyCallback) {
            args.put("callback", "dummy");
        }
        return sendMessageFromWebpageToClient(cmd, args);
    }

    /**
     * Returns the value of calling
     * {@link #sendCommandToClient(String, Map, boolean)} with the last argument
     * <code>false</code>, so this method requires an explicit callback
     * function in <code>args</code>.
     * 
     * @return the value of calling
     *         {@link #sendCommandToClient(String, Map, boolean)} with the last
     *         argument <code>false</code>, so this method requires an
     *         explicit callback function in <code>args</code>.
     * @see #sendCommandToClient(String, Map, boolean) *
     */
    protected final String sendCommandToClient(String cmd, Map<String, String> args) {
        return sendCommandToClient(cmd, args, false);
    }    
    
    protected final String sendMessageFromWebpageToClient(String cmd, Map<String, String> args) {
        args.put("callback", "dummy");
        String responseWithCallback = sender.sendMessage(cmd, args);
        return LWSServerUtil.removeCallback(responseWithCallback);
    }
    
    /**
     * Returns a {@link FakeJavascriptCodeInTheWebpage.Handler} for ensuring
     * there was some error, but the type doesn't matter.
     * 
     * @return a {@link FakeJavascriptCodeInTheWebpage.Handler} for ensuring
     *         there was some error, but the type doesn't matter.
     */
    protected final FakeJavascriptCodeInTheWebpage.Handler errorHandlerAny() {
        return new FakeJavascriptCodeInTheWebpage.Handler() {
            public void handle(final String res) {
                final String have = LWSServerUtil.unwrapError(LWSServerUtil.removeCallback(res));
                //
                // All errors are of the form 
                //
                //   <String> [ '.' <String> ]+
                //
                // See LWSDispatcherSupport#ErrorCodes
                //
                assertTrue(have.indexOf('.') != -1);
            }
        };
    }
    
    /**
     * @see Injector#getInstance(Class)
     */
    protected final <T> T getInstance(Class<T> type) {
        return inj.getInstance(type);
    }
    
    protected String getSignedBytes(String data){
        
        try{
            PrivateKey priv = remoteServer.getPrivateKey();
            Signature dsa = Signature.getInstance("SHA1withDSA"); 
            dsa.initSign(priv);
    
            /* Update and sign the data */
            dsa.update(StringUtils.toUTF8Bytes(data));
            byte[] sig = dsa.sign();
            return Base32.encode(sig);
        }catch(Exception ex){
            
        }
        
        return null;
    }
   
}
