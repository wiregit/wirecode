package org.limewire.lws.server;


/**
 * Creates a {@link LWSDispatcher} and is the main entry point to this component.
 */
public class LWSDispatcherFactory {

    /**
     * Returns a new {@link LWSDispatcher}. A {@link LWSDispatcher} has to know how
     * to send a message to a remote server -- i.e. a
     * {@link LWSSenderOfMessagesToServer} -- and to whom commands should go after
     * authentication -- i.e. a {@link LWSReceivesCommandsFromDispatcher}.
     * 
     * @param sender responsible for sending messages to the Lime Wire Store
     *        server
     * @param recipient responsible for acting on commands sent from the web
     *        page
     * @return a new {@link LWSDispatcher}
     */
    public static LWSDispatcher createDispatcher(LWSSenderOfMessagesToServer sender, LWSReceivesCommandsFromDispatcher recipient) {
        final LWSDispatcherImpl s = new LWSDispatcherImpl(sender);
        s.setCommandReceiver(recipient);
        return s;
    }

}
