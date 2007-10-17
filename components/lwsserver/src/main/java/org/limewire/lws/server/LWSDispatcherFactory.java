package org.limewire.lws.server;


/**
 * Creates a {@link Dispatcher} and is the main entry point to this component.
 */
public class LWSDispatcherFactory {

    /**
     * Returns a new {@link Dispatcher}. A {@link Dispatcher} has to know how
     * to send a message to a remote server -- i.e. a
     * {@link SenderOfMessagesToServer} -- and to whom commands should go after
     * authentication -- i.e. a {@link ReceivesCommandsFromDispatcher}.
     * 
     * @param sender responsible for sending messages to the Lime Wire Store
     *        server
     * @param recipient responsible for acting on commands sent from the web
     *        page
     * @return a new {@link Dispatcher}
     */
    public static Dispatcher createDispatcher(SenderOfMessagesToServer sender, ReceivesCommandsFromDispatcher recipient) {
        final LWSServerDispatcher s = new LWSServerDispatcher(sender);
        s.setDispatchee(recipient);
        return s;
    }

}
