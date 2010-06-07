package org.limewire.lws.server;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManagerImpl;
import org.limewire.util.Base32;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

/**
 * Provides the basis methods for doing communication. Subclasses should test
 * each aspect of this communication separately.
 */
abstract class AbstractCommunicationSupport extends BaseTestCase {
    
    public final static int LOCAL_PORT  = LocalServerImpl.PORT;

    private KeyPair keyPair;
    private LocalServerImpl localServer;
    private FakeJavascriptCodeInTheWebpage code;
    
    private Thread localThread;

    public AbstractCommunicationSupport(String s) {
        super(s);
    }
    
    // -------------------------------------------------------
    // Access
    // -------------------------------------------------------
    
    protected final LocalServerImpl getLocalServer() {
        return this.localServer;
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
    
    /**
     * Returns a {@link FakeJavascriptCodeInTheWebpage#Handler} for ensuring
     * there was some error, but the type doesn't matter.
     * 
     * @return a {@link FakeJavascriptCodeInTheWebpage#Handler} for ensuring
     *         there was some error, but the type doesn't matter.
     */
    protected final FakeJavascriptCodeInTheWebpage.Handler errorHandlerAny() {
        return new FakeJavascriptCodeInTheWebpage.Handler() {
            public void handle(final String res) {
                final String have = LWSServerUtil.unwrapError(LWSServerUtil.removeCallback(res));
                assertTrue(have.indexOf('.') != -1);
            }
        };
    }
    
    /**
     * Returns a {@link FakeJavascriptCodeInTheWebpage#Handler} for ensuring
     * there was an error from <code>wants</code>.
     * 
     * @return Returns a {@link FakeJavascriptCodeInTheWebpage#Handler} for
     *         ensuring there was an error from <code>wants</code>.
     */    
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
                    fail("didn't received one message of: " + unwrap(wants) + " but did receive '" + have  + "'");
                }
            }
        };
    }

    protected final Thread getLocalThread() {
        return localThread;
    }

    @Override
    protected final void setUp() throws Exception {

        beforeSetup();
        
        // generate private-public key pair
        KeyPairGenerator keyGen = null;      
        keyGen = KeyPairGenerator.getInstance("DSA");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        random.setSeed(System.currentTimeMillis());
        keyGen.initialize(1024, random);
        keyPair = keyGen.generateKeyPair();
        
        String storePublicKey = Base32.encode(keyPair.getPublic().getEncoded());
        
        SocketsManager socketsManager = new SocketsManagerImpl();
        localServer     = new LocalServerImpl(socketsManager, "localhost", storePublicKey);
        localThread     = localServer.start();
        code            = new FakeJavascriptCodeInTheWebpage(socketsManager, localServer);

        afterSetup();
    }

    @Override
    protected final void tearDown() throws Exception {

        beforeTearDown();

        stop(localServer);

        localThread = null;

        afterTearDown();
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
    
    private void stop(final AbstractServer t) {
        if (t != null) {
          t.shutDown();
        }
    }
    
    protected String getSignedBytes(String data) throws Exception{
        PrivateKey priv = keyPair.getPrivate();
        Signature dsa = Signature.getInstance("SHA1withDSA"); 
        dsa.initSign(priv);

        /* Update and sign the data */
        dsa.update(StringUtils.toUTF8Bytes(data));
        byte[] sig = dsa.sign();
        return Base32.encode(sig);   
    }
}
