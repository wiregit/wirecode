package org.limewire.store.server;


public class StoreServerFactory {

    public static Dispatcher createDispatcher(final SendsMessagesToServer sender, Dispatchee dispatchee) {
        final StoreServerDispatcher s = new StoreServerDispatcher(sender);
        s.setDispatchee(dispatchee);
        return s;
    }

}
