package org.limewire.store.storeserver.core;

import java.util.HashMap;
import java.util.Map;

import org.limewire.concurrent.ManagedThread;
import org.limewire.store.storeserver.api.Dispatchee;
import org.limewire.store.storeserver.api.Dispatcher;
import org.limewire.store.storeserver.api.Server;
import org.limewire.store.storeserver.util.Util;

/**
 * Base class for local servers.
 */
public abstract class ServerImpl extends AbstractServer implements Server {

    /** Whether we're using the watcher of not. */
    private final static boolean USING_WATCHER = true;

    public final void start() {
        start(this);
    }

    public ServerImpl(final int port, final String name, final DispatcherImpl dis) {
        super(port, name, dis);
    }
    
    public ServerImpl(final int port, final String name) {
        super(port, name);
    }
    
    public static abstract class DispatcherImpl extends Dispatcher {
        
        private String publicKey;
        private String privateKey;
        private State state;
        
        public DispatcherImpl() {
            newState(State.IDLE);
        }
 

        @Override
        protected final Dispatcher.Handler[] createHandlers() {
            return new Dispatcher.Handler[] { new StartCom(),
                    new Authenticate(), new Detatch(), new Msg(), new Echo() };
        }
        
        public final String getPublicKey() {
            return publicKey;
        }

        public final String getPrivateKey() {
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
            noteNewState(this.state);
        }
        
        /**
         * Called on every state change. Subclasses should override to provide extra
         * functionality on a state change.
         * 
         * @param newState the new state
         */
        protected abstract void noteNewState(final State newState);

        // ------------------------------------------------------------
        // Handlers
        // ------------------------------------------------------------

        /**
         * A {@link Dispatcher.Handler} that needs both a callback and private
         * key.
         */
        protected abstract class HandlerWithCallbackWithPrivateKey extends
                HandlerWithCallback {

            protected final String handleRest(final Map<String, String> args,
                    final Request req) {
                if (getPrivateKey() == null) {
                    return report(ErrorCodes.UNITIALIZED_PRIVATE_KEY);
                }
                final String herPrivateKey = getArg(args, Parameters.PRIVATE);
                if (herPrivateKey == null) {
                    return report(ErrorCodes.MISSING_PRIVATE_KEY_PARAMETER);
                }
                if (!herPrivateKey.equals(getPrivateKey())) {
                    return report(ErrorCodes.INVALID_PRIVATE_KEY);
                }
                return handleRest(herPrivateKey, args, req);
            }

            /**
             * The result <b>IN PLAIN TEXT</b> using the private key,
             * <tt>privateKey</tt>. Override this to do something meaningful
             * with the passed along private key, too.
             * 
             * @param privateKey private key pulled from the args
             * @param args original, untouched arguments
             * @param req originating {@link Request} object
             * @return result <b>IN PLAIN TEXT</b> using the private key,
             *         <tt>privateKey</tt>
             */
            abstract String handleRest(String privateKey,
                    Map<String, String> args, Request req);
        }

        /**
         * Issues a command to start authentication.
         */
        class StartCom extends HandlerWithCallback {
            protected String handleRest(final Map<String, String> args,
                    final Request req) {
                regenerateKeys();
                //
                // send the keys to the Server and wait for a response
                //
                final Map<String, String> sendArgs = new HashMap<String, String>();
                newState(State.STORE);
                sendArgs.put(Parameters.PRIVATE, privateKey);
                sendArgs.put(Parameters.PUBLIC, publicKey);
                sendMsg(Commands.STORE_KEY, sendArgs);
                newState(State.WAITING);
                return publicKey;
            }
        }

        /**
         * Sent from code with private key to authenticate.
         */
        class Authenticate extends HandlerWithCallbackWithPrivateKey {
            protected String handleRest(final String privateKey,
                    final Map<String, String> args, final Request req) {
                newState(State.COMMUNICATING);
                if (getDispatchee() != null)
                    getDispatchee().setConnected(true);
                return Server.Responses.OK;
            }
        }

        /**
         * Send from code to end session.
         */
        class Detatch extends HandlerWithCallback {
            protected String handleRest(final Map<String, String> args,
                    final Request req) {
                newState(State.IDLE);
                privateKey = null;
                publicKey = null;
                if (getDispatchee() != null)
                    getDispatchee().setConnected(false);
                return Server.Responses.OK;
            }
        }

        /**
         * Sent from code with parameter {@link Parameters#COMMAND}.
         */
        class Msg extends HandlerWithCallbackWithPrivateKey {
            protected String handleRest(final String privateKey,
                    final Map<String, String> args, final Request req) {
                String cmd = getArg(args, Parameters.COMMAND);
                if (cmd == null) {
                    return report(ErrorCodes.MISSING_COMMAND_PARAMETER);
                }
                if (getDispatchee() != null) {
                    final Map<String, String> newArgs = new HashMap<String, String>(
                            args);
                    String newCmd = Util.addURLEncodedArguments(cmd, newArgs);
                    return getDispatchee().dispatch(newCmd, newArgs);
                }
                return Server.Responses.NO_DISPATCHEE;
            }
        }

        /**
         * Sent from code with parameter {@link Parameters#MSG}.
         */
        class Echo extends HandlerWithCallbackWithPrivateKey {
            protected String handleRest(final String privateKey,
                    final Map<String, String> args, final Request req) {
                String msg = getArg(args, Parameters.MSG);
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
    
    }

 

}
