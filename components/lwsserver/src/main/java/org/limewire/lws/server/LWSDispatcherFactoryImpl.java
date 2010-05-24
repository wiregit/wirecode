package org.limewire.lws.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;



/**
 * Creates a {@link LWSDispatcher} and is the main entry point to this component.
 */
@Singleton
class LWSDispatcherFactoryImpl implements LWSDispatcherFactory {

    @Inject
    public LWSDispatcherFactoryImpl() {
    }

    public LWSDispatcher createDispatcher(LWSReceivesCommandsFromDispatcher recipient, LWSCommandValidator verifier) {
        LWSDispatcherImpl s = new LWSDispatcherImpl(verifier);
        s.setCommandReceiver(recipient);
        return s;
    }
}