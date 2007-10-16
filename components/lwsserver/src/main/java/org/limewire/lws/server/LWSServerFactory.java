package org.limewire.lws.server;


/**
 * Creates a {@link Dispatcher}.
 */
public class LWSServerFactory {

    /**
     * Returns a new {@link Dispatcher}.
     * 
     * @param sender        responsible for sending messages to the Lime Wire Store server
     * @param dispatchee    responsible for acting on commands sent from the web page
     * @return              a new {@link Dispatcher}
     */
    public static Dispatcher createDispatcher(final SenderOfMessagesToServer sender, Dispatchee dispatchee) {
        final LWSServerDispatcher s = new LWSServerDispatcher(sender);
        s.setDispatchee(dispatchee);
        return s;
    }

}
