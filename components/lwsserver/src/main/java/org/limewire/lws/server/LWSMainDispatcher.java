/**
 * 
 */
package org.limewire.lws.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.limewire.service.ErrorService;


/**
 * Dispatches commands after going through an authentication phase explained
 * here. <br/> This represents the state in the FSA. Basically, the operation of
 * the local server is the following:
 * 
 * <pre>
 * Code -&gt; Local : Authenticate  
 *       +------+                             
 *  + -- | Wait | &lt;---------------------------+
 *  |    +------+                             | Local -&gt; Remote : StoreKey
 *  |    | Code -&gt; Local : Detach             | Local -&gt; Code : PrivateKey
 *  |    |                                    |
 *  |    V         Code -&gt; Local : Detach     |
 *  |    +------+ &lt;-------------------------- +-------+
 *  |    | Idle |                             | Store |
 *  |    +------+ --------------------------&gt; +-------+
 *  |    &circ;         Code -&gt; Local : StartCom
 *  |    |
 *  |    | Code -&gt; Local : Detach
 *  |    |
 *  |    +---------------+
 *  +--&gt; | Communicating |
 *       +---------------+
 * &lt;pre&gt;
 * 
 */
final class LWSMainDispatcher extends LWSDispatcherSupport {
       
    private final SenderOfMessagesToServer sender;
    private String publicKey;
    private String privateKey;
    
    public LWSMainDispatcher(SenderOfMessagesToServer sender) {
        this.sender = sender;
    }

    @Override
    protected final Handler[] createHandlers() {
        return new Handler[] { 
                new StartCom(),
                new Authenticate(), 
                new Detatch(), 
                new Msg() 
         };
    }
    
    final String getPublicKey() {
        return publicKey;
    }

    final String getPrivateKey() {
        return privateKey;
    }

    private void regenerateKeys() {
        publicKey = LWSServerUtil.generateKey();
        privateKey = LWSServerUtil.generateKey();
        note("public key  : {0}", publicKey);
        note("private key : {0}", privateKey);
    }

    // ------------------------------------------------------------
    // Handlers
    // ------------------------------------------------------------

    /**
     * A {@link Handler} that needs both a callback and private
     * key.
     */
    protected abstract class HandlerWithCallbackWithPrivateKey extends HandlerWithCallback {

        protected final void handleRest(final Map<String, String> args, StringCallback cb) {
            String privateKey = getPrivateKey();
            if (privateKey == null) {
                cb.process(report(LWSDispatcherSupport.ErrorCodes.UNITIALIZED_PRIVATE_KEY));
                return;
            }
            String herPrivateKey = args.get(LWSDispatcherSupport.Parameters.PRIVATE);
            if (herPrivateKey == null) {
                cb.process(report(LWSDispatcherSupport.ErrorCodes.MISSING_PRIVATE_KEY_PARAMETER));
                return;
            }
            if (!herPrivateKey.equals(getPrivateKey())) {
                cb.process(report(LWSDispatcherSupport.ErrorCodes.INVALID_PRIVATE_KEY));
                return;
            }
            handleRest(herPrivateKey, args, cb);
        }

        /**
         * The result <b>IN PLAIN TEXT</b> using the private key,
         * <tt>privateKey</tt>. Override this to do something meaningful
         * with the passed along private key, too.
         * 
         * @param privateKey private key pulled from the args
         * @param args original, untouched arguments
         * @return result <b>IN PLAIN TEXT</b> using the private key,
         *         <tt>privateKey</tt>
         */
        abstract void handleRest(String privateKey,Map<String, String> args, StringCallback cb);
    }

    /**
     * Issues a command to start authentication.
     */
    class StartCom extends HandlerWithCallback {
        protected void handleRest(final Map<String, String> args, final StringCallback cb) {
            regenerateKeys();
            //
            // send the keys to the Server and wait for a response
            //
            final Map<String, String> sendArgs = new HashMap<String, String>();
            sendArgs.put(LWSDispatcherSupport.Parameters.PRIVATE, privateKey);
            sendArgs.put(LWSDispatcherSupport.Parameters.PUBLIC, publicKey);
            try {
                sender.sendMessageToServer(LWSDispatcherSupport.Commands.STORE_KEY, sendArgs, new StringCallback() {
                    public void process(String response) {
                        cb.process(Responses.OK.equals(response.trim()) ? publicKey : "0");
                    }
                });
            } catch (IOException e) {
                ErrorService.error(e, "StartCom.handleRest");
            }
        }
    }

    /**
     * Sent from code with private key to authenticate.
     */
    class Authenticate extends HandlerWithCallbackWithPrivateKey {
        protected void handleRest(String privateKey, Map<String, String> args, StringCallback cb) {
            getDispatchee().setConnected(true);
            cb.process(LWSDispatcherSupport.Responses.OK);
        }
    }

    /**
     * Send from code to end session.
     */
    class Detatch extends HandlerWithCallback {
        protected void handleRest(Map<String, String> args, StringCallback cb) {
            privateKey = null;
            publicKey = null;
            getDispatchee().setConnected(false);
            cb.process(LWSDispatcherSupport.Responses.OK);
        }
    }

    /**
     * Sent from code with parameter {@link Parameters#COMMAND}.
     */
    class Msg extends HandlerWithCallbackWithPrivateKey {
        protected void handleRest(String privateKey,
                                    Map<String, String> args,
                                    StringCallback cb) {
            String cmd = args.get(LWSDispatcherSupport.Parameters.COMMAND);
            if (cmd == null) {
                cb.process(report(LWSDispatcherSupport.ErrorCodes.MISSING_COMMAND_PARAMETER));
                return;
            }
            if (getDispatchee() != null) {
                final Map<String, String> newArgs = new HashMap<String, String>(args);
                String newCmd = LWSServerUtil.addURLEncodedArguments(cmd, newArgs);
                cb.process(getDispatchee().receiveCommand(newCmd, newArgs));
                return;
            }
            cb.process(LWSDispatcherSupport.Responses.NO_DISPATCHEE);
        }
    }

}