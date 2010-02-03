package org.limewire.core.impl.rest;

import org.limewire.core.impl.rest.handler.RestTarget;
import org.limewire.core.impl.rest.handler.RestRequestHandlerFactory;
import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.browser.LocalHTTPAcceptor;

/**
 * REST API service for the live core.
 */
@EagerSingleton
public class CoreGlueRestService implements Service {

    private static final String REMOTE_PREFIX = "/remote/";
    
    private final Provider<LocalHTTPAcceptor> localHttpAcceptorFactory;
    private final RestRequestHandlerFactory restRequestHandlerFactory;
    
    @Inject
    public CoreGlueRestService(Provider<LocalHTTPAcceptor> localHttpAcceptorFactory,
            RestRequestHandlerFactory restRequestHandlerFactory) {
        this.localHttpAcceptorFactory = localHttpAcceptorFactory;
        this.restRequestHandlerFactory = restRequestHandlerFactory;
    }
    
    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }
    
    @Override
    public String getServiceName() {
        return I18nMarker.marktr("REST Service");
    }

    @Override
    public void initialize() {
    }

    @Override
    public void start() {
        // Register handlers for all REST targets.
        for (RestTarget restTarget : RestTarget.values()) {
            localHttpAcceptorFactory.get().registerHandler(createPattern(restTarget.pattern()),
                    restRequestHandlerFactory.createRequestHandler(restTarget));
        }
    }

    @Override
    public void stop() {
        // Unregister handlers for all REST targets.
        for (RestTarget restTarget : RestTarget.values()) {
            localHttpAcceptorFactory.get().unregisterHandler(createPattern(restTarget.pattern()));
        }
    }

    /**
     * Creates the remote access URI pattern for the specified target.
     */
    private String createPattern(String target) {
        return REMOTE_PREFIX + target + "*";
    }
}
