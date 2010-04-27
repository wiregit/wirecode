package org.limewire.lws.server;

import org.limewire.core.api.network.NetworkManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;



/**
 * Creates a {@link LWSDispatcher} and is the main entry point to this component.
 */
@Singleton
public class LWSDispatcherFactoryImpl implements LWSDispatcherFactory {
      
    private final NetworkManager networkManager;

    @Inject
    public LWSDispatcherFactoryImpl(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public LWSDispatcher createDispatcher(LWSSenderOfMessagesToServer sender, LWSReceivesCommandsFromDispatcher recipient) {
        LWSDispatcherImpl s = new LWSDispatcherImpl(sender, networkManager);
        s.setCommandReceiver(recipient);
        return s;
    }

}
