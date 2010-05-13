package com.limegroup.gnutella.lws.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.lws.server.FakeJavascriptCodeInTheWebpage;
import org.limewire.lws.server.LWSCommandValidator;
import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LWSServerUtil;
import org.limewire.lws.server.LocalServerImpl;
import org.limewire.lws.server.MockLWSCommandValidator;
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
    
    protected final Map<String,String> EMPTY_ARGS = new HashMap<String,String>();
    
    private CommandSender sender;
    private LWSManager lwsManager;
    private LifecycleManager lifecycleManager;        

    public AbstractCommunicationSupportWithNoLocalServer(String s) {
        super(s);
    }
    
    // -------------------------------------------------------
    // Access
    // -------------------------------------------------------
    
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
    
    private Injector inj;    

    @Override
    protected final void setUp() throws Exception {
        
        note("begin setUp");

        beforeSetup();
        
        inj = LimeTestUtils.createInjector(TestUtils.bind(LWSCommandValidator.class).toInstances(new MockLWSCommandValidator()));
        lifecycleManager = inj.getInstance(LifecycleManager.class);
        lifecycleManager.start();
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
        
        lifecycleManager.shutdown();
       
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
   
}
