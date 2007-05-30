package org.limewire.store.storeserver.core;

import java.util.HashMap;
import java.util.Map;

import org.limewire.store.storeserver.api.Dispatchee;
import org.limewire.store.storeserver.api.Server;
import org.limewire.store.storeserver.util.Util;


/**
 * Base class for local servers.
 * 
 * @author jpalm
 */
public abstract class ServerImpl extends AbstractServer implements Server {

    /** Whether we're using the watcher of not. */
    private final static boolean USING_WATCHER = true;

    private String publicKey;
    private String privateKey;
    private State state;
    private Dispatchee dispatchee;

    /* (non-Javadoc)
     * @see org.limewire.store.storeserver.core.I#setDispatchee(org.limewire.store.storeserver.core.Dispatchee)
     */
    public final void setDispatchee(final Dispatchee dispatchee) {
        this.dispatchee = dispatchee;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.store.storeserver.core.I#getDispatchee()
     */
    public final Dispatchee getDispatchee() {
        return this.dispatchee;
    }
    
    public final void start() {
        start(this);
    }

    /**
     * Represents the state in the FSA.
     * 
     * @author jpalm
     */
    public enum State {
        IDLE, STORE, WAITING, COMMUNICATING,
    }

    public ServerImpl(final int port) {
        super(port);
        newState(State.IDLE);
    }

    public final String getPublicKey() {
        return publicKey;
    }

    public final String getPrivateKey() {
        return privateKey;
    }

    /**
     * Called on every state change. Subclasses should override to provide extra
     * functionality on a state change.
     * 
     * @param newState the new state
     */
    protected abstract void noteNewState(final State newState);

    private void regenerateKeys() {
        if (USING_WATCHER && watcher != null) {
            publicKey = Util.generateKey();
            privateKey = Util.generateKey();
            watcher.setPublicKey(publicKey);
            watcher.setPrivateKey(privateKey);
        } else {
            publicKey = Util.generateKey();
            privateKey = Util.generateKey();
        }
        note("public key  : " + publicKey);
        note("private key : " + privateKey);
    }

    @Override
    protected final Handler[] createHandlers() {
        return new Handler[] { new StartCom(), new Authenticate(),
                new Detatch(), new Msg(), new Echo() };
    }

    private void newState(final State newState) {
        if (USING_WATCHER && watcher != null) {
            synchronized (watcher) {
                this.state = newState;
                watcher.setState(newState);
            }
        } else {
            this.state = newState;
        }
        noteNewState(this.state);
    }

    // ------------------------------------------------------------
    // Handlers
    // ------------------------------------------------------------

    /**
     * A {@link Handler} that needs both a callback and private key.
     */
    protected abstract class HandlerWithCallbackWithPrivateKey extends HandlerWithCallback {

        public final String handleRest(final Map<String, String> args,
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
         * The result <b>IN PLAIN TEXT</b> using the private key, <tt>privateKey</tt>.
         * Override this to do something meaningful with the passed along
         * private key, too.
         * 
         * @param privateKey    private key pulled from the args
         * @param args          original, untouched arguments
         * @param req           originating {@link Request} object
         * @return              result <b>IN PLAIN TEXT</b> using the private key, <tt>privateKey</tt>
         */
        abstract String handleRest(String privateKey, Map<String, String> args, Request req);
    }

    /**
     * Issues a command to start authentication.
     * 
     * @author jpalm
     */
    class StartCom extends HandlerWithCallback {
        public String handleRest(final Map<String, String> args, final Request req) {
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
     * 
     * @author jpalm
     */
    class Authenticate extends HandlerWithCallbackWithPrivateKey {
        public String handleRest(final String privateKey, final Map<String, String> args, final Request req) {
            newState(State.COMMUNICATING);
            if (dispatchee != null) dispatchee.setConnected(true);
            return Server.Responses.OK;
        }
    }

    /**
     * Send from code to end session.
     * 
     * @author jpalm
     */
    class Detatch extends HandlerWithCallback {
        public String handleRest(final Map<String, String> args, final Request req) {
            newState(State.IDLE);
            privateKey = null;
            publicKey = null;
            if (dispatchee != null) dispatchee.setConnected(false);
            return Server.Responses.OK;
        }
    }

    /**
     * Sent from code with parameter {@link Parameters#COMMAND}.
     * 
     * @author jpalm
     */
    class Msg extends HandlerWithCallbackWithPrivateKey {
        public String handleRest(final String privateKey, final Map<String, String> args, final Request req) {
            String cmd = getArg(args, Parameters.COMMAND);
            if (cmd == null) {
                return report(ErrorCodes.MISSING_COMMAND_PARAMETER);
            }
            if (dispatchee != null) {
                final Map<String, String> newArgs = new HashMap<String, String>(args);
                String newCmd = Util.addURLEncodedArguments(cmd, newArgs);
                return dispatchee.dispatch(newCmd, newArgs);
            }
            return Server.Responses.NO_DISPATCHEE;
        }
    }

    /**
     * Sent from code with parameter {@link Parameters#MSG}.
     * 
     * @author jpalm
     */
    class Echo extends HandlerWithCallbackWithPrivateKey {
        public String handleRest(final String privateKey,
                final Map<String, String> args, final Request req) {
            String msg = getArg(args, Parameters.MSG);
            return msg;
        }
    }

    // ------------------------------------------------------------
    // Watcher
    // ------------------------------------------------------------

    private final Watcher watcher = new Watcher();

    protected void noteRun() {
        if (USING_WATCHER)
            new Thread(this.watcher).start();
    }

    /**
     * This class will watcher over the server, and during any timeouts when we
     * quit can restart the server, resetting the current state.
     */
    private final class Watcher implements Runnable {

        private String privateKey;

        private String publicKey;

        private State state;

        public void run() {
            while (true) {
                //
                // Make sure we're still running
                //
                if (!isDone() && !hasShutDown()) {
                    if (getRunner() == null) {
                        ServerImpl.this.privateKey = privateKey;
                        ServerImpl.this.publicKey = publicKey;
                        ServerImpl.this.state = state;
                        start(ServerImpl.this);
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // we don't care
                }
            }
        }

        public void setPrivateKey(final String privateKey) {
            this.privateKey = privateKey;
        }

        public void setPublicKey(final String publicKey) {
            this.publicKey = publicKey;
        }

        public void setState(final State newState) {
            this.state = newState;
        }

    }

}
