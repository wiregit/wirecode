/**
 * 
 */
package org.limewire.store.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.limewire.service.ErrorService;

final class StoreServerDispatcher extends DispatcherSupport implements SendsMessagesToServer {
    
    private final SendsMessagesToServer sender;
    private String publicKey;
    private String privateKey;
    private State state;
    
    public StoreServerDispatcher(SendsMessagesToServer sender) {
        this.sender = sender;
        newState(State.IDLE);
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
    
    private void newState(final State newState) {
       this.state = newState;
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
            newState(State.STORE);
            sendArgs.put(DispatcherSupport.Parameters.PRIVATE, privateKey);
            sendArgs.put(DispatcherSupport.Parameters.PUBLIC, publicKey);
            sendMsgToRemoteServer(DispatcherSupport.Commands.STORE_KEY, sendArgs);
            newState(State.WAITING);
            return publicKey;
        }
    }

    /**
     * Sent from code with private key to authenticate.
     */
    class Authenticate extends HandlerWithCallbackWithPrivateKey {
        protected String handleRest(final String privateKey,
                final Map<String, String> args) {
            newState(State.COMMUNICATING);
            getDispatchee().setConnected(true);
            return DispatcherSupport.Responses.OK;
        }
    }

    /**
     * Send from code to end session.
     */
    class Detatch extends HandlerWithCallback {
        protected String handleRest(final Map<String, String> args) {
            newState(State.IDLE);
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

    /**
     * Represents the state in the FSA. <br>
     * Basically, the operation of the local server is the following:
     * <ol>
     * 
     * </ol>
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
    public enum State {
    
        /**
         * Just waiting. Go to this state after detaching, always.
         */
        IDLE,
    
        /**
         * After receiving a <em>StartCom</em> message from the Code, the
         * local server send a <em>StoreKey</em> message to the remote server,
         * and gives the private key back to the Code.
         */
        STORE,
    
        /**
         * After storing the public/private key pair on the remote server, the
         * local machine waits for an <em>Authenticate</em> message from the
         * Code.
         */
        WAITING,
    
        /**
         * After receiving the correct private key from the Code, we start
         * communication mode and pass all subsequent messages to the
         * {@link Dispatchee}.
         */
        COMMUNICATING,
    }

    @Override
    public String sendMsgToRemoteServer(String msg, Map<String, String> args) {
        return sender.sendMsgToRemoteServer(msg, args);
    }

}