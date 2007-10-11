/**
 * 
 */
package org.limewire.lws.server;

import java.util.HashMap;
import java.util.Map;

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
final class LWSServerDispatcher extends DispatcherSupport implements SendsMessagesToServer {
    
    private final SendsMessagesToServer sender;
    private String publicKey;
    private String privateKey;
    
    public LWSServerDispatcher(SendsMessagesToServer sender) {
        this.sender = sender;
    }

    @Override
    protected final Handler[] createHandlers() {
        return new Handler[] { 
                new StartCom(),
                new Authenticate(), 
                new Detatch(), 
                new Msg(), 
                new Echo() 
         };
    }
    
    final String getPublicKey() {
        return publicKey;
    }

    final String getPrivateKey() {
        return privateKey;
    }

    private void regenerateKeys() {
        publicKey = Util.generateKey();
        privateKey = Util.generateKey();
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
    protected abstract class HandlerWithCallbackWithPrivateKey extends
            HandlerWithCallback {

        protected final String handleRest(final Map<String, String> args) {
            if (getPrivateKey() == null) {
                return report(DispatcherSupport.ErrorCodes.UNITIALIZED_PRIVATE_KEY);
            }
            final String herPrivateKey = Util.getArg(args, DispatcherSupport.Parameters.PRIVATE);
            if (herPrivateKey == null) {
                return report(DispatcherSupport.ErrorCodes.MISSING_PRIVATE_KEY_PARAMETER);
            }
            if (!herPrivateKey.equals(getPrivateKey())) {
                return report(DispatcherSupport.ErrorCodes.INVALID_PRIVATE_KEY);
            }
            return handleRest(herPrivateKey, args);
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
        abstract String handleRest(String privateKey,Map<String, String> args);
    }

    /**
     * Issues a command to start authentication.
     */
    class StartCom extends HandlerWithCallback {
        protected String handleRest(final Map<String, String> args) {
            regenerateKeys();
            //
            // send the keys to the Server and wait for a response
            //
            final Map<String, String> sendArgs = new HashMap<String, String>();
            sendArgs.put(DispatcherSupport.Parameters.PRIVATE, privateKey);
            sendArgs.put(DispatcherSupport.Parameters.PUBLIC, publicKey);
            sendMsgToRemoteServer(DispatcherSupport.Commands.STORE_KEY, sendArgs);
            return publicKey;
        }
    }

    /**
     * Sent from code with private key to authenticate.
     */
    class Authenticate extends HandlerWithCallbackWithPrivateKey {
        protected String handleRest(final String privateKey,
                final Map<String, String> args) {
            getDispatchee().setConnected(true);
            return DispatcherSupport.Responses.OK;
        }
    }

    /**
     * Send from code to end session.
     */
    class Detatch extends HandlerWithCallback {
        protected String handleRest(final Map<String, String> args) {
            privateKey = null;
            publicKey = null;
            getDispatchee().setConnected(false);
            return DispatcherSupport.Responses.OK;
        }
    }

    /**
     * Sent from code with parameter {@link Parameters#COMMAND}.
     */
    class Msg extends HandlerWithCallbackWithPrivateKey {
        protected String handleRest(final String privateKey,
                final Map<String, String> args) {
            String cmd = Util.getArg(args, DispatcherSupport.Parameters.COMMAND);
            if (cmd == null) {
                return report(DispatcherSupport.ErrorCodes.MISSING_COMMAND_PARAMETER);
            }
            if (getDispatchee() != null) {
                final Map<String, String> newArgs = new HashMap<String, String>(
                        args);
                String newCmd = Util.addURLEncodedArguments(cmd, newArgs);
                return getDispatchee().dispatch(newCmd, newArgs);
            }
            return DispatcherSupport.Responses.NO_DISPATCHEE;
        }
    }

    /**
     * Sent from code with parameter {@link Parameters#MSG}.
     */
    class Echo extends HandlerWithCallbackWithPrivateKey {
        protected String handleRest(final String privateKey,
                final Map<String, String> args) {
            String msg = Util.getArg(args, DispatcherSupport.Parameters.MSG);
            return msg;
        }
    }

    @Override
    public String sendMsgToRemoteServer(String msg, Map<String, String> args) {
        return sender.sendMsgToRemoteServer(msg, args);
    }

}